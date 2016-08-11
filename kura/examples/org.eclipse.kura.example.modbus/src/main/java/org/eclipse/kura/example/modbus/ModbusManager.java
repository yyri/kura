/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.modbus;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.example.modbus.parser.ModbusConfigParser;
import org.eclipse.kura.example.modbus.parser.ScannerConfig;
import org.eclipse.kura.example.modbus.parser.ScannerConfigParser;
import org.eclipse.kura.example.modbus.register.Command;
import org.eclipse.kura.example.modbus.register.Field;
import org.eclipse.kura.example.modbus.register.ModbusHandler;
import org.eclipse.kura.example.modbus.register.Option;
import org.eclipse.kura.example.modbus.register.Register;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.protocol.modbus.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusManager extends Cloudlet implements ConfigurableComponent, KuraChangeListener {

	// (kura.service.pid=org.eclipse.kura.cloud.CloudService) ATTENZIONE!
	
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusManager.class);

	private static final String APP_ID = "ModbusManager";
	private static final String PROP_MODBUS_CONF = "plc.configuration";
	private static final String PROP_MODBUS_SCAN = "plc.scan";
	private static final String PROP_SERIAL_MODE = "serialMode";
	private static final String PROP_PORT = "port";
	private static final String PROP_IP = "ipAddress";
	private static final String PROP_BAUDRATE = "baudRate";
	private static final String PROP_BITPERWORD = "bitsPerWord";
	private static final String PROP_STOPBITS = "stopBits";
	private static final String PROP_PARITY = "parity";
	private static final String CUSTOMER_NAME = "customer.name";
	private static final String TRANSMISSION_MODE = "transmissionMode";
	private static final String RESP_TIMEOUT = "respTimeout";
	
	private static final String BAD_REQUEST_ERROR = "Bad request topic: {}";
	private static final String RESOURCE_ERROR = "Expected one resource but found {}";
	private static final String COMMAND_ERROR = "No model found for command.";
	private static final String RESOURCES_1_ERROR = "Cannot find resource with name: {}";
	private static final String MODBUS_READ_ERROR = "Modbus read command failed.";
	private static final String VALUE = "value";
	
	private CloudClient cloudClient;
	public static ModbusProtocolDeviceService protocolDevice;

	private Map<String, Object> currentProperties;
	private Properties modbusProperties;

	private boolean configured;

	// Map<PLCModel,List<SlaveAdress>>
	private Map<String,List<Integer>> devices;
	// Map<pollGroupName,PollConfiguration>
	private Map<String,PollConfiguration> pollGroups;
	// Map<publishGroupName,PublishConfiguration>
	private Map<String,PublishConfiguration> publishGroups;
	// Map<PLCModel,Map<CommandName,Command>>
	private Map<String,Map<String,Command>> commands;
	private List<Metric> metrics;
	private List<Metric> oldMetrics;

	private ScheduledExecutorService pollExecutor;
	private ScheduledExecutorService publishExecutor;
	private Map<String,ScheduledFuture<?>> publishHandles;
	private Map<String, ModbusWorker> workers = new HashMap<String,ModbusWorker>();

	public ModbusManager() {
		super(APP_ID);
		pollExecutor = Executors.newScheduledThreadPool(5);
		publishExecutor = Executors.newScheduledThreadPool(5);
	}

	public void setModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
		this.protocolDevice = modbusService;
	}

	public void unsetModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
		this.protocolDevice = null;
	}

	protected void activate(ComponentContext ctx, Map<String, Object> properties) {
		s_logger.info("Activating ModbusManager...");

		configured = false;
		publishHandles = new HashMap<String,ScheduledFuture<?>>();

		super.activate(ctx);
		cloudClient = super.getCloudApplicationClient();
			
		updated(ctx, properties);
	}

	@Override
	protected void deactivate(ComponentContext ctx) {
		s_logger.info("Deactivating ModbusManager...");

		initializeWorkers();

		if (pollExecutor != null)
			pollExecutor.shutdown();

		for (Entry<String,ScheduledFuture<?>> handle : publishHandles.entrySet()) {
			handle.getValue().cancel(true);
		}
		publishHandles.clear();
		
		if (publishExecutor != null)
			publishExecutor.shutdown();
		
		if(protocolDevice!=null)
			try {
				protocolDevice.disconnect();
			} catch (ModbusProtocolException e) {
				s_logger.error("Unable to disconnect from modbus device.", e);
			}

		configured = false;

		if (getCloudApplicationClient() != null) {
			super.deactivate(ctx);
		}
		
	}

	protected void updated(ComponentContext ctx, Map<String, Object> properties) {
		s_logger.info("Updating ModbusManager...");

		currentProperties = properties;
		modbusProperties = getModbusProperties();
		configured=false;

		doWork();

	}

	private synchronized void doWork() {

		initializeWorkers();
		initializeHandles();
		initializeMetrics();
		
		final KuraChangeListener kuraChangeListener = this;
		new Thread(new Runnable() {
			@Override
			public void run() {
				if(modbusProperties != null && !configured) {
					try {
						configureDevice();
					} catch (ModbusProtocolException e) {
						s_logger.warn("The modbus port is not yet available", e);
					}
				}
				if (configured) {
					// Scan for connected devices or get devices address from configuration
					searchForDevices();
					// Get PLCs registers from configuration 
					parseConfiguration();
					s_logger.info("Configuration done!");
					startWorkers(kuraChangeListener);
				}
			}
		}).start();
	}

	private void startWorkers(KuraChangeListener kuraChangeListener) {
		// Start workers for polling
		for(Entry<String, PollConfiguration> entry : pollGroups.entrySet()) {
			// Check if the worker is on the workers list and the interval is not -1 (on demand registers)
			if (workers.get(entry.getKey()) == null && entry.getValue().getInterval() != -1)
				workers.put(entry.getKey(),new ModbusWorker(entry.getValue(), pollExecutor, kuraChangeListener));
		}
		
		// Start workers for publishing
		for(final Entry<String, PublishConfiguration> entry : publishGroups.entrySet()) {
			if (publishHandles.get(entry.getKey()) == null) {
				publishHandles.put(entry.getKey(), publishExecutor.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						publishMessage(entry.getValue());
					}
				}, 2000, entry.getValue().getInterval(), TimeUnit.MILLISECONDS));
			}
		}
	}

	private void parseConfiguration() {

		ModbusConfigParser.parse((String) currentProperties.get(PROP_MODBUS_CONF));
		
		// Get Poll Groups
		initializePollGroups();
		List<PollConfiguration> pollConfigurations = ModbusConfigParser.getPollGroups();
		for (PollConfiguration config : pollConfigurations) {
			pollGroups.put(config.getName(), config);
		}
		
		if (pollGroups.isEmpty()) {
			s_logger.warn("No pollGroups found!");
			return;
		}
		
		// Get Pool Resources
		Map<String, List<PollResources>> pollResources = ModbusConfigParser.getPollResources(devices);
		for (Entry<String, List<PollResources>> entry : pollResources.entrySet()) {
			if (pollGroups.get(entry.getKey()) != null) {
				pollGroups.get(entry.getKey()).addResources(entry.getValue());
			}
		}
		
		// Get Publish Groups
		initializePublishGroups();
		List<PublishConfiguration> publishConfigurations = ModbusConfigParser.getPublishGroups();
		for (PublishConfiguration config : publishConfigurations) {
			publishGroups.put(config.getName(), config);
		}
		
		// Get Commands
		initializeCommands();
		for (String model : devices.keySet()) {
			commands.put(model, ModbusConfigParser.getCommands(model));
		}
		
	}

	/*
	 * org.eclipse.kura.example.modbus.KuraChangeListener
	 */
	
	@Override
	public synchronized void stateChanged(List<Metric> dataMetrics) {
		
		for (Metric m : dataMetrics) {
			s_logger.debug("Name {}", m.getMetricName());
			s_logger.debug("Value {}", m.getData());
			metrics.add(new Metric(m.getMetricName(),m.getPublishGroup(),m.getSlaveAddress(),m.getData()));
		}

	}
	
	/*
	 * Modbus Properties and configuration methods
	 */
	
	private Properties getModbusProperties() {
		if(currentProperties != null) {
			// If the port is not set, exit
			if (currentProperties.get(PROP_PORT) == null) 
				return null;
			
			Properties prop = new Properties();
			
			if(currentProperties.get(TRANSMISSION_MODE) != null) 
				prop.setProperty(TRANSMISSION_MODE, (String) currentProperties.get(TRANSMISSION_MODE));
			else
				prop.setProperty(TRANSMISSION_MODE, "RTU");
			
			if(currentProperties.get(RESP_TIMEOUT) != null) 
				prop.setProperty(RESP_TIMEOUT, (String) currentProperties.get(RESP_TIMEOUT));
			else
				prop.setProperty(RESP_TIMEOUT, "1000");
			
			if(currentProperties.get(PROP_SERIAL_MODE) != null) {
				String serialMode = (String) currentProperties.get(PROP_SERIAL_MODE);
				if("RS232".equalsIgnoreCase(serialMode) || "RS485".equalsIgnoreCase(serialMode)) {
					setSerialProperties(prop, serialMode);
				} else {
					setEthernetProperties(prop);
				}
			}

			return prop;
		} else {
			return null;
		}
	}

	private void setEthernetProperties(Properties prop) {
		prop.setProperty("connectionType", "ETHERTCP");
		prop.setProperty(PROP_PORT, (String) currentProperties.get(PROP_PORT));
		
		if(currentProperties.get(PROP_IP) != null) 
			prop.setProperty(PROP_IP, (String) currentProperties.get(PROP_IP));
		else
			prop.setProperty(PROP_IP, null);
	}

	private void setSerialProperties(Properties prop, String serialMode) {
		prop.setProperty(PROP_PORT, (String) currentProperties.get(PROP_PORT));
		prop.setProperty("connectionType", "SERIAL");
		prop.setProperty(PROP_SERIAL_MODE, serialMode);
		prop.setProperty("exclusive", "false");
		prop.setProperty("mode", "0");
			
		if (currentProperties.get(PROP_BAUDRATE) != null)
			prop.setProperty(PROP_BAUDRATE, (String) currentProperties.get(PROP_BAUDRATE));
		else
			prop.setProperty(PROP_BAUDRATE, "9600");
		
		if (currentProperties.get(PROP_STOPBITS) != null) 
			prop.setProperty(PROP_STOPBITS, (String) currentProperties.get(PROP_STOPBITS));
		else
			prop.setProperty(PROP_STOPBITS, "1");

		if(currentProperties.get(PROP_PARITY) != null) 
			prop.setProperty(PROP_PARITY, (String) currentProperties.get(PROP_PARITY));
		else
			prop.setProperty(PROP_PARITY, "0");
		
		if(currentProperties.get(PROP_BITPERWORD) != null) 
			prop.setProperty(PROP_BITPERWORD, (String) currentProperties.get(PROP_BITPERWORD));
		else
			prop.setProperty(PROP_BITPERWORD, null);
	}
	
	private void configureDevice() throws ModbusProtocolException {
		if(protocolDevice!=null){
			protocolDevice.disconnect();

			protocolDevice.configureConnection(modbusProperties);

			configured = true;
		}
	}
	
	/*
	 * Publish Message methods 
	 */
	
	private synchronized void publishMessage(PublishConfiguration configuration) {

		// Valutare se usare una mappa di liste direttamente. Tabella sincronizzata con transaction e commit. Usare il db?
		s_logger.debug("PublishMessage()");
		Map<Integer,List<Metric>> deviceMetricList = new HashMap<Integer,List<Metric>>();
		
		s_logger.debug("Add {} metrics to deviceMetricList...",metrics.size());
		for (Metric metric : metrics) {
			if (metric.getPublishGroup().equals(configuration.getName())) {
				if (!configuration.getOnChange() || (configuration.getOnChange() && isMetricChanged(metric))) {
					Metric metricCopy = new Metric(metric);
					metrics.remove(metric);

					if (deviceMetricList.get(metricCopy.getSlaveAddress()) == null) {
						List<Metric> metricList = new ArrayList<Metric>();
						metricList.add(metricCopy);
						deviceMetricList.put(metricCopy.getSlaveAddress(), metricList);
					} else {
						deviceMetricList.get(metricCopy.getSlaveAddress()).add(metricCopy);
					}
				} else {
					metrics.remove(metric);
				}
			}
		}
		s_logger.debug("...Done!");
		
		s_logger.debug("Create payloads.");
		for (Entry<Integer,List<Metric>> entry : deviceMetricList.entrySet()) {
			KuraPayload payload = new KuraPayload();
			payload.setTimestamp(new Date());

			s_logger.debug("Adding metrics.");
			for (Metric metric : entry.getValue()) {
				payload.addMetric(metric.getMetricName(), metric.getData());
			}
			
			try {
				s_logger.debug("Publish Message on {} ", entry.getKey() + "/" + configuration.getTopic());
				cloudClient.publish(currentProperties.get(CUSTOMER_NAME) + "/" + entry.getKey() + "/" + configuration.getTopic(), payload, configuration.getQos(), false);
			} catch (KuraException e) {
				s_logger.error("Unable to publish message", e);
			}
		}
		s_logger.debug("Publish Done!");
		
	}
	
	private boolean isMetricChanged(Metric metric) {
		boolean isChanged = true;
		for (Metric oldMetric : oldMetrics) {
			if (oldMetric.getMetricName().equals(metric.getMetricName()) && (oldMetric.getSlaveAddress() == metric.getSlaveAddress())) {
				if (oldMetric.compareData(metric)) {
					isChanged = false;
				} else {
					oldMetrics.remove(oldMetric);
				}
				break;
			}
		}
		if (isChanged) {
			oldMetrics.add(new Metric(metric));
		}
		return isChanged;
	}
	
	/*
	Scan for PLC devices methods
	*/
	
	private void searchForDevices() {
		if (devices == null) {
			devices = new HashMap<String,List<Integer>>();
		}
		devices.clear();
		
		ScannerConfig scannerConfig = ScannerConfigParser.parse(new String((String) currentProperties.get(PROP_MODBUS_SCAN)));
		
		if (scannerConfig.getDisabled()) {
			getStaticDevices(scannerConfig);
		} else {
			scanForDevices(scannerConfig);
		}
		
		for (Entry<String,List<Integer>> device : devices.entrySet()) {
			for (Integer address : device.getValue())
				s_logger.debug("Get device " + device.getKey() + " " + address);
		}
		
	}

	private void getStaticDevices(ScannerConfig scannerConfig) {
		
		String model;
		for (Entry<Integer,String> entry : scannerConfig.getAssets().entrySet()) {
			model = entry.getValue();
			if (devices.get(model) == null) {
				ArrayList<Integer> slaveAddresses = new ArrayList<Integer>();
				slaveAddresses.add(entry.getKey());
				devices.put(model, slaveAddresses);
			} else {
				devices.get(model).add(entry.getKey());
			}
		}
		
	}
	
	private void scanForDevices(ScannerConfig scannerConfig) {
		
		// It is executed twice... Please fix it.
		s_logger.debug("Start scan for devices...");
		
		long now = System.currentTimeMillis();
		while((System.currentTimeMillis() - now) < new Long(scannerConfig.getInterval())) {
			// Use registers 1 and 2 for model detection for now!
			for (int i = scannerConfig.getMinRange(); i < scannerConfig.getMaxRange(); i++) {
				try {
					int[] regs = (int[]) ModbusHandler.readModbus("HR", i, 1, 2);
					for (Entry<String,Map<Integer,Integer>> entry : scannerConfig.getModels().entrySet()) {
						if (entry.getValue().get(1).equals(regs[0]) && entry.getValue().get(2).equals(regs[1])) {
							s_logger.debug("Found device {} @ {}", entry.getKey(), i);
							if (devices.get(entry.getKey()) == null) {
								ArrayList<Integer> slaveAddresses = new ArrayList<Integer>();
								slaveAddresses.add(i);
								devices.put(entry.getKey(), slaveAddresses);
							} else {
								if (!devices.get(entry.getKey()).contains(i)) {
									devices.get(entry.getKey()).add(i);
								}
							}
						}
					}
				} catch (ModbusProtocolException e) {
					// Don't report this exception
					s_logger.debug("Modbus device not found at {}.", i);
				}	
			}
		}
		
		s_logger.debug("...Done!");
	}
	
	/*
	 * Cloudlet methods
	 */
	
	@Override
	protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

		s_logger.debug("EXEC received!");
		
		String[] resources = reqTopic.getResources();
		
		if (resources == null || resources.length != 3) {
			s_logger.error(BAD_REQUEST_ERROR, reqTopic.toString());
			s_logger.error(RESOURCE_ERROR, resources != null ? resources.length : "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		Integer slaveAddress = Integer.parseInt(resources[1]);
		String model = searchModel(slaveAddress);
		if (model.isEmpty()) {
			s_logger.warn(COMMAND_ERROR);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			return;
		}
		
		if ("bulkRead".equals(resources[2])) {
			List<String> request = new ArrayList<String>();
			Register register;
			// Get the metric names from the payload
			request.addAll(reqPayload.metricNames());
			request.remove("requester.client.id");
			request.remove("request.id");
			for (int i = 0; i < request.size(); i++) {
				PollResources pollResource = searchRegister(request.get(i), slaveAddress, "R");
				register = pollResource.getRegisters().get(0);
				if (register == null) {
					s_logger.warn("Metric {} not supported for reading on model {}.", request.get(i), model);
				} else {
			
					try {
						if ("HR".equals(pollResource.getType())) {
							getHR(pollResource, respPayload, request.get(i), slaveAddress);
						} else if ("C".equals(pollResource.getType())) {
							boolean[] data = (boolean[]) ModbusHandler.readModbus(pollResource.getType(), slaveAddress, Integer.parseInt(pollResource.getRegisterAddress(),16) + register.getId(), 1);
							respPayload.addMetric(register.getName(), data[0]);
						} else {
							s_logger.error(BAD_REQUEST_ERROR, reqTopic.toString());
							s_logger.error(RESOURCES_1_ERROR, request.get(i));
							return;				
						}
					} catch (ModbusProtocolException e) {
						s_logger.error(MODBUS_READ_ERROR, e);
						respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
					}
				}
			}
			if (respPayload.metrics().isEmpty()) {
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			} else {
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			}
		} else {
			Map<String,Command> availableCommands = commands.get(model);
			if (availableCommands == null) {
				s_logger.warn("No commands found for {}.", model);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
				return;
			}

			Command c = availableCommands.get(resources[2]); 
			if (c == null) {
				s_logger.warn("Command {} not supported for writing on model {}.", resources[2], model);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
				return;			
			}

			try {
				if ("HR".equals(c.getType())) {
					int[] data = {c.getCommandValue()};
					ModbusHandler.writeMultipleRegister(slaveAddress.intValue(), c.getAddress().intValue(), data);
					respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
				} else if ("C".equals(c.getType())) {
					ModbusHandler.writeSingleCoil(slaveAddress.intValue(), c.getAddress().intValue(), c.getCommandValue() == 1 ? true : false);
					respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
				} else {
					s_logger.error(BAD_REQUEST_ERROR, reqTopic.toString());
					s_logger.error(RESOURCES_1_ERROR, resources[0]);
					respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
					return;				
				}
			} catch (ModbusProtocolException e) {
				s_logger.error("Modbus write command failed.", e);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			}
		}

	}
	
	@Override
	protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

		s_logger.debug("GET received!");
		
		String[] resources = reqTopic.getResources();
		
		if (resources == null || resources.length != 3) {
			s_logger.error(BAD_REQUEST_ERROR, reqTopic.toString());
			s_logger.error(RESOURCE_ERROR,
					resources != null ? resources.length : "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		Integer slaveAddress = Integer.parseInt(resources[1]);
		String model = searchModel(slaveAddress);
		if (model.isEmpty()) {
			s_logger.warn(COMMAND_ERROR);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			return;
		}
		
		String request = resources[2];
		PollResources pollResource = searchRegister(request, slaveAddress, "R");
		Register register = pollResource.getRegisters().get(0);
		if (register == null) {
			s_logger.warn("Metric {} not supported reading for model {}.", request, model);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;			
		}
		
		try {
			if ("HR".equals(pollResource.getType())) {
				getHR(pollResource, respPayload, request, slaveAddress);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			} else if ("C".equals(pollResource.getType())) {
				boolean[] data = (boolean[]) ModbusHandler.readModbus(pollResource.getType(), slaveAddress, Integer.parseInt(pollResource.getRegisterAddress(),16) + register.getId(), 1);
				respPayload.addMetric(register.getName(), data[0]);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			} else {
				s_logger.error(BAD_REQUEST_ERROR, reqTopic.toString());
				s_logger.error(RESOURCES_1_ERROR, request);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
				return;				
			}
		} catch (ModbusProtocolException e) {
			s_logger.error(MODBUS_READ_ERROR, e);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
		}

	}
	
	@Override
	protected void doPost(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

		s_logger.debug("POST received!");
		
		String[] resources = reqTopic.getResources();
		
		if (resources == null || resources.length != 3) {
			s_logger.error(BAD_REQUEST_ERROR, reqTopic.toString());
			s_logger.error(RESOURCE_ERROR,
					resources != null ? resources.length : "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		Integer slaveAddress = Integer.parseInt(resources[1]);
		String model = searchModel(slaveAddress);
		if (model.isEmpty()) {
			s_logger.warn(COMMAND_ERROR);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			return;
		}
		
		String request = resources[2];
		PollResources pollResource = searchRegister(request, slaveAddress, "W");
		Register register = pollResource.getRegisters().get(0);
		if (register == null) {
			s_logger.warn("Metric {} not supported for writing on model {}.", request, model);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;			
		}
		
		try {
			if ("HR".equals(pollResource.getType())) {
				postHR(pollResource, reqPayload, request, slaveAddress);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			} else if ("C".equals(pollResource.getType())) {
				ModbusHandler.writeSingleCoil(slaveAddress.intValue(), Integer.parseInt(pollResource.getRegisterAddress(),16) + register.getId(), "1".equals((String) reqPayload.getMetric(VALUE)) ? true : false);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			} else {
				s_logger.error(BAD_REQUEST_ERROR, reqTopic.toString());
				s_logger.error(RESOURCES_1_ERROR, request);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
				return;				
			}
		} catch (ModbusProtocolException e) {
			s_logger.error(MODBUS_READ_ERROR, e);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
		}

	}
	
	private String searchModel(Integer slaveAddress) {
		String model = "";
		for (Entry<String,List<Integer>> entry : devices.entrySet()) {
			if (entry.getValue().contains(slaveAddress)) {
				model = entry.getKey();
			}
		}
		return model;
	}
	
	private PollResources searchRegister(String request, Integer slaveAddress, String accessType) {
		PollResources pollResource = new PollResources();
		for (Entry<String,PollConfiguration> entry : pollGroups.entrySet()) {
			for (PollResources pollRes : entry.getValue().getResources()) {
				if (pollRes.getSlaveAddress().contains(slaveAddress)) {
					for (Register reg : pollRes.getRegisters()) {
						if (request.equals(reg.getName()) && reg.getAccess().contains(accessType)) {
							// For analog inputs
							pollResource.setRegisterAddress(pollRes.getRegisterAddress());
							pollResource.setType(pollRes.getType());
							pollResource.addRegister(reg);
							break;
						} else if (!reg.getFields().isEmpty() && reg.getAccess().contains(accessType)) {
							// For digital input, output, alarms and parameters
							for (Field field : reg.getFields()) {
								if (request.equals(field.getName())) {
									pollResource.setRegisterAddress(pollRes.getRegisterAddress());
									pollResource.setType(pollRes.getType());
									pollResource.addRegister(reg);
									break;
								}
							}
						}
					}
				}
			}
		}
		return pollResource;
	}
	private void getHR(PollResources pollResource, KuraResponsePayload respPayload, String request, Integer slaveAddress) throws ModbusProtocolException {
		Register register = pollResource.getRegisters().get(0);
		int[] data = (int[]) ModbusHandler.readModbus(pollResource.getType(), slaveAddress, Integer.parseInt(pollResource.getRegisterAddress(),16) + register.getId(), 1);
		if ("int16".equals(register.getType())) {
			respPayload.addMetric(register.getName(), (float) (data[0] * register.getScale() + register.getOffset()));
		} else if ("bitmap16".equals(register.getType())) {
			for (Field f : register.getFields()) {
				if (f.getName().equals(request)) {
					if (!f.getOptions().isEmpty()) {
						Integer value = data[0] & Integer.parseInt(f.getMask(),16);
						for (Option option : f.getOptions()) {
							if (Integer.parseInt(option.getValue(),16) == value) {
								respPayload.addMetric(f.getName(), option.getName());
								break;
							}
						}
					} else {
						respPayload.addMetric(f.getName(), (data[0] & Integer.parseInt(f.getMask(),16)) == Integer.parseInt(f.getMask(),16) ? true : false);
					}
				}
			}
		}
	}
	
	private void postHR(PollResources pollResource, KuraRequestPayload reqPayload, String request, Integer slaveAddress) throws ModbusProtocolException {
		Register register = pollResource.getRegisters().get(0);
		if ("int16".equals(register.getType())) {
			Float value = (Float) reqPayload.getMetric(VALUE);
			int[] data = {Math.round((value - register.getOffset()) / register.getScale())};
			ModbusHandler.writeMultipleRegister(slaveAddress.intValue(), Integer.parseInt(pollResource.getRegisterAddress(),16) + register.getId(), data);
		} else if ("bitmap16".equals(register.getType())) {
			for (Field f : register.getFields()) {
				if (f.getName().equals(request)) {
					int[] data = (int[]) ModbusHandler.readModbus(pollResource.getType(), slaveAddress, Integer.parseInt(pollResource.getRegisterAddress(),16) + register.getId(), 1);
					String value = (String) reqPayload.getMetric(VALUE);
					if (!f.getOptions().isEmpty()) {
						for (Option option : f.getOptions()) {
							if (option.getName().equals(value)) {
								int[] result = {(data[0] & (~Integer.parseInt(f.getMask(),16))) | Integer.parseInt(option.getValue(),16)};
								ModbusHandler.writeMultipleRegister(slaveAddress.intValue(), Integer.parseInt(pollResource.getRegisterAddress(),16) + register.getId(), result);
								break;
							}
						}
					} else {
						int[] result = {(data[0] & (~Integer.parseInt(f.getMask(),16))) | Integer.parseInt(value,16)};
						ModbusHandler.writeMultipleRegister(slaveAddress.intValue(), Integer.parseInt(pollResource.getRegisterAddress(),16) + register.getId(), result);
					}
				}
			}
		}
	}
	
	/*
	 * Initializers 
	 */
	
	private synchronized void initializeMetrics() {
		if (metrics == null) {
			metrics = new CopyOnWriteArrayList<Metric>(new ArrayList<Metric>());
		}
		metrics.clear();
		
		if (oldMetrics == null) {
			oldMetrics = new ArrayList<Metric>();
		}
		oldMetrics.clear();
	}
	
	private void initializeHandles() {
		for (Entry<String,ScheduledFuture<?>> handle : publishHandles.entrySet()) {
			handle.getValue().cancel(true);
		}
		publishHandles.clear();
	}

	private void initializeWorkers() {
		for (Entry<String,ModbusWorker> w : workers.entrySet()) {
			w.getValue().stop();
		}
		workers.clear();
	}
	
	private void initializeCommands() {
		if (commands == null) {
			commands = new HashMap<String,Map<String,Command>>();
		}
		commands.clear();
	}

	private void initializePublishGroups() {
		if (publishGroups == null) {
			publishGroups = new HashMap<String,PublishConfiguration>();
		}
		publishGroups.clear();
	}

	private void initializePollGroups() {
		if (pollGroups == null) {
			pollGroups = new HashMap<String,PollConfiguration>();
		}
		pollGroups.clear();
	}
}
