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
import org.eclipse.kura.example.modbus.register.ModbusResources;
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

	private CloudClient cloudClient;
	public static ModbusProtocolDeviceService protocolDevice;

	private Map<String, Object> currentProperties;
	private static Properties modbusProperties;

	private boolean configured;

	private Map<String,List<Integer>> devices;
	private Map<String,ModbusConfiguration> pollGroups;
	private Map<String,PublishConfiguration> publishGroups;
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

		for (Entry<String,ModbusWorker> w : workers.entrySet()) {
			w.getValue().stop();
		}
		workers.clear();

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

		for (Entry<String,ModbusWorker> w : workers.entrySet()) {
			w.getValue().stop();
		}
		workers.clear();
		
		for (Entry<String,ScheduledFuture<?>> handle : publishHandles.entrySet()) {
			handle.getValue().cancel(true);
		}
		publishHandles.clear();

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
					
					// Start workers for polling
					for(Entry<String, ModbusConfiguration> entry : pollGroups.entrySet()) {
						if (workers.get(entry.getKey()) == null)
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
			}
		}).start();
	}

	@Override
	public synchronized void stateChanged(List<Metric> dataMetrics) {
		
		for (Metric m : dataMetrics) {
			s_logger.debug("Name {}", m.getMetricName());
			s_logger.debug("Value {}", m.getData());
			metrics.add(new Metric(m.getMetricName(),m.getPublishGroup(),m.getSlaveAddress(),m.getData()));
		}

	}

	private void parseConfiguration() {

		ModbusConfigParser.parse((String) currentProperties.get(PROP_MODBUS_CONF));
		
		// Get Poll Groups
		if (pollGroups == null) {
			pollGroups = new HashMap<String,ModbusConfiguration>();
		}
		pollGroups.clear();

		List<ModbusConfiguration> modbusConfigurations = ModbusConfigParser.getPollGroups();
		for (ModbusConfiguration config : modbusConfigurations) {
			pollGroups.put(config.getName(), config);
		}
		
		if (pollGroups.isEmpty()) {
			s_logger.warn("No pollGroups found!");
			return;
		}
		
		// Get Modbus Configuration
		Map<String, List<ModbusResources>> modbusResources = ModbusConfigParser.getModbusResources(devices);
		for (Entry<String, List<ModbusResources>> entry : modbusResources.entrySet()) {
			if (pollGroups.get(entry.getKey()) != null) {
				pollGroups.get(entry.getKey()).addRegisters(entry.getValue());
			}
			
		}
		
		// Get Publish Groups
		if (publishGroups == null) {
			publishGroups = new HashMap<String,PublishConfiguration>();
		}
		publishGroups.clear();

		List<PublishConfiguration> publishConfigurations = ModbusConfigParser.getPublishGroups();
		for (PublishConfiguration config : publishConfigurations) {
			publishGroups.put(config.getName(), config);
		}
		
		// Get Commands
		if (commands == null) {
			commands = new HashMap<String,Map<String,Command>>();
		}
		commands.clear();
		
		for (String model : devices.keySet()) {
			commands.put(model, ModbusConfigParser.getCommands(model));
		}
		
	}

	private synchronized void publishMessage(PublishConfiguration configuration) {

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
	
	private Properties getModbusProperties() {
		Properties prop = new Properties();

		if(currentProperties!=null){
			String portName = null;
			String serialMode = null;
			String baudRate = null;
			String bitsPerWord = null;
			String stopBits = null;
			String parity = null;
			String ipAddress = null;
			String mode= null;
			String timeout= null;
			if(currentProperties.get("transmissionMode") != null) 
				mode = (String) currentProperties.get("transmissionMode");
			if(currentProperties.get("respTimeout") != null) 
				timeout	= (String) currentProperties.get("respTimeout");
			if(currentProperties.get(PROP_PORT) != null) 
				portName = (String) currentProperties.get(PROP_PORT);
			if(currentProperties.get(PROP_SERIAL_MODE) != null) 
				serialMode = (String) currentProperties.get(PROP_SERIAL_MODE);
			if(currentProperties.get(PROP_BAUDRATE) != null) 
				baudRate = (String) currentProperties.get(PROP_BAUDRATE);
			if(currentProperties.get(PROP_BITPERWORD) != null) 
				bitsPerWord = (String) currentProperties.get(PROP_BITPERWORD);
			if(currentProperties.get(PROP_STOPBITS) != null) 
				stopBits = (String) currentProperties.get(PROP_STOPBITS);
			if(currentProperties.get(PROP_PARITY) != null) 
				parity = (String) currentProperties.get(PROP_PARITY);
			if(currentProperties.get(PROP_IP) != null) 
				ipAddress = (String) currentProperties.get(PROP_IP);
			
			if(portName==null)
				return null;		
			if(baudRate==null) 
				baudRate="9600";
			if(stopBits==null) 
				stopBits="1";
			if(parity==null) 
				parity="0";
			if(bitsPerWord==null) 
				bitsPerWord="8";
			if(mode==null) 
				mode="RTU";
			if(timeout==null) 
				timeout="1000";
			
			if(serialMode!=null) {
				if("RS232".equalsIgnoreCase(serialMode) || "RS485".equalsIgnoreCase(serialMode)) {
					prop.setProperty("connectionType", "SERIAL");
					prop.setProperty("serialMode", serialMode);
					prop.setProperty("port", portName);
					prop.setProperty("exclusive", "false");
					prop.setProperty("mode", "0");
					prop.setProperty("baudRate", baudRate);
					prop.setProperty("stopBits", stopBits);
					prop.setProperty("parity", parity);
					prop.setProperty("bitsPerWord", bitsPerWord);
				} else {
					prop.setProperty("connectionType", "ETHERTCP");
					prop.setProperty("ipAddress", ipAddress);
					prop.setProperty("port", portName);
				}
			}
			prop.setProperty("transmissionMode", mode);
			prop.setProperty("respTimeout", timeout);

			return prop;
		} else {
			return null;
		}
	}
	
	private void configureDevice() throws ModbusProtocolException {
		if(protocolDevice!=null){
			protocolDevice.disconnect();

			protocolDevice.configureConnection(modbusProperties);

			configured = true;
		}
	}
	
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
					int reg1 = protocolDevice.readHoldingRegisters(i, 1, 1)[0];
					int reg2 = protocolDevice.readHoldingRegisters(i, 2, 1)[0];
					for (Entry<String,Map<Integer,Integer>> entry : scannerConfig.getModels().entrySet()) {
						if (entry.getValue().get(1).equals(reg1) && entry.getValue().get(2).equals(reg2)) {
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
	
	private String searchModel(Integer slaveAddress) {
		String model = "";
		for (Entry<String,List<Integer>> entry : devices.entrySet()) {
			if (entry.getValue().contains(slaveAddress)) {
				model = entry.getKey();
			}
		}
		return model;
	}
	
	@Override
	protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

		s_logger.debug("EXEC received!");
		
		String[] resources = reqTopic.getResources();
		
		if (resources == null || resources.length != 3) {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Expected one resource but found {}",
					resources != null ? resources.length : "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		Integer slaveAddress = Integer.parseInt(resources[1]);
		String model = searchModel(slaveAddress);
		if (model.isEmpty()) {
			s_logger.warn("No model found for command.");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			return;
		}
		
		Map<String,Command> availableCommands = commands.get(model);
		if (availableCommands == null) {
			s_logger.warn("No commands found for {}.", model);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			return;
		}
		
		Command c = availableCommands.get(resources[2]); 
		if (c == null) {
			s_logger.warn("Command {} not supported for model {}.", resources[2], model);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;			
		}
		
		try {
			if ("HR".equals(c.getType())) {
				if (c.getCommandValue() == null) {
					// If the value is null, search it in the request
					Float value = (Float) reqPayload.getMetric("value");
					int[] data = {Math.round((value - c.getOffset()) / c.getScale())};
					writeMultipleRegister(slaveAddress.intValue(), c.getAddress().intValue(), data);
				} else {
					int[] data = {c.getCommandValue()};
					writeMultipleRegister(slaveAddress.intValue(), c.getAddress().intValue(), data);
				}
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			} else if ("C".equals(c.getType())) {
				writeSingleCoil(slaveAddress.intValue(), c.getAddress().intValue(), c.getCommandValue() == 1 ? true : false);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			} else {
				s_logger.error("Bad request topic: {}", reqTopic.toString());
				s_logger.error("Cannot find resource with name: {}", resources[0]);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
				return;				
			}
		} catch (ModbusProtocolException e) {
			s_logger.error("Modbus write command failed.", e);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
		}

	}
	
	private synchronized void writeSingleRegister(int unitAddr, int dataAddress, int data) throws ModbusProtocolException {
		protocolDevice.writeSingleRegister(unitAddr, dataAddress, data);
	}
	
	private synchronized void writeMultipleRegister(int unitAddr, int dataAddress, int[] data) throws ModbusProtocolException {
		protocolDevice.writeMultipleRegister(unitAddr, dataAddress, data);
	}
	
	private synchronized void writeSingleCoil(int unitAddr, int dataAddress, boolean data) throws ModbusProtocolException {
		protocolDevice.writeSingleCoil(unitAddr, dataAddress, data);
	}
	
	private synchronized void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data) throws ModbusProtocolException {
		protocolDevice.writeMultipleCoils(unitAddr, dataAddress, data);
	}
	
}