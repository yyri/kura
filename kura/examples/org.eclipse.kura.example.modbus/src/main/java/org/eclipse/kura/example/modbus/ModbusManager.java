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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.example.modbus.parser.ModbusConfigParser;
import org.eclipse.kura.example.modbus.parser.ScannerConfig;
import org.eclipse.kura.example.modbus.parser.ScannerConfigParser;
import org.eclipse.kura.example.modbus.register.ModbusResources;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.protocol.modbus.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusManager implements ConfigurableComponent, KuraChangeListener, CloudClientListener {

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

	private static final String CTRL_TOPIC = "/command";

	private CloudService cloudService;
	private CloudClient cloudClient;
	public static ModbusProtocolDeviceService protocolDevice;

	private ScheduledExecutorService executor;

	private Map<String, Object> currentProperties;
	private static Properties modbusProperties;

	private boolean configured;

	private Map<String,List<Integer>> devices;
	private Map<String,ModbusConfiguration> pollGroups;
	private Map<String,PublishConfiguration> publishGroups;

	private List<ModbusWorker> workers = new ArrayList<ModbusWorker>();

	public ModbusManager() {
		executor = Executors.newScheduledThreadPool(5);
	}

	protected void setCloudService(CloudService cloudService) {
		this.cloudService = cloudService;
	}

	protected void unsetCloudService(CloudService cloudService) {
		this.cloudService = null;
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

		try {
			cloudClient = cloudService.newCloudClient(APP_ID);
			cloudClient.addCloudClientListener(this);
			updated(ctx, properties);
		} catch (KuraException e) {
			s_logger.error("Configuration update failed", e);
		}
	}

	protected void deactivate(ComponentContext ctx) {
		s_logger.info("Deactivating ModbusManager...");

		for (ModbusWorker w : workers) {
			w.stop();
		}
		workers.clear();

		if (executor != null)
			executor.shutdown();

		if(protocolDevice!=null)
			try {
				protocolDevice.disconnect();
			} catch (ModbusProtocolException e) {
				s_logger.error("Unable to disconnect from modbus device.", e);
			}

		configured = false;

		cloudClient.release();
	}

	protected void updated(ComponentContext ctx, Map<String, Object> properties) {
		s_logger.info("Updating ModbusManager...");

		currentProperties = properties;
		modbusProperties = getModbusProperties();
		configured=false;

		doWork();

	}

	private void doWork() {

		for (ModbusWorker w : workers) {
			w.stop();
		}
		workers.clear();

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
					searchForDevices();
					parseConfiguration();
					s_logger.info("Configuration done!");
				}
			}
		}).start();
	}

	@Override
	public synchronized void stateChanged(Object e, String topic) {
		if(e instanceof Map<?, ?>){
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) e;
			s_logger.info("Publishing data for:");
			//			for (Entry<String,Object> entry : map.entrySet()) {
			//				s_logger.info("		{} = {}", entry.getKey(), entry.getValue());
			//			}
			//			publishMessage(map, topic);
			// Custom filter for test purposes only!!!
			Map<String,Object> filteredMap = new HashMap<String,Object>();
			for (Entry<String,Object> entry : map.entrySet()) {
				if (entry.getKey().equals("Room") || entry.getKey().equals("Evaporator") || entry.getKey().equals("SetPoint")) {
					float[] data = {((int[]) entry.getValue())[0]*0.1F};
					filteredMap.put(entry.getKey(), data);
					s_logger.info("		{} = {}", entry.getKey(), data[0]);
				} 
				else if (entry.getKey().equals("DigitalOutput")) {
					int data = ((int[]) entry.getValue())[0];
					if ((data & 0x0100) == 0x0100) {
						boolean[] output = {true};
						filteredMap.put("OnOffOut", output);
						s_logger.info("		{} = {}", "OnOffOut", output);
					} else {
						boolean[] output = {false};
						filteredMap.put("OnOffOut", output);
						s_logger.info("		{} = {}", "OnOffOut", output);
					}
					if ((data & 0x0200) == 0x0200) {
						boolean[] output = {true};
						filteredMap.put("Defrost", output);
						s_logger.info("		{} = {}", "Defrost", output);
					} else {
						boolean[] output = {false};
						filteredMap.put("Defrost", output);
						s_logger.info("		{} = {}", "Defrost", output);
					}
					if ((data & 0x0400) == 0x0400) {
						boolean[] output = {true};
						filteredMap.put("Defrost2", output);
						s_logger.info("		{} = {}", "Defrost2", output);
					} else {
						boolean[] output = {false};
						filteredMap.put("Defrost2", output);
						s_logger.info("		{} = {}", "Defrost2", output);
					}
					if ((data & 0x0800) == 0x0800) {
						boolean[] output = {true};
						filteredMap.put("Alarm", output);
						s_logger.info("		{} = {}", "Alarm", output);
					} else {
						boolean[] output = {false};
						filteredMap.put("Alarm", output);
						s_logger.info("		{} = {}", "Alarm", output);
					}
					if ((data & 0x1000) == 0x1000) {
						boolean[] output = {true};
						filteredMap.put("Light", output);
						s_logger.info("		{} = {}", "Light", output);
					} else {
						boolean[] output = {false};
						filteredMap.put("Light", output);
						s_logger.info("		{} = {}", "Light", output);
					}
					if ((data & 0x2000) == 0x2000) {
						boolean[] output = {true};
						filteredMap.put("Fan", output);
						s_logger.info("		{} = {}", "Fan", output);
					} else {
						boolean[] output = {false};
						filteredMap.put("Fan", output);
						s_logger.info("		{} = {}", "Fan", output);
					}
					if ((data & 0x4000) == 0x4000) {
						boolean[] output = {true};
						filteredMap.put("Aux", output);
						s_logger.info("		{} = {}", "Aux", output);
					} else {
						boolean[] output = {false};
						filteredMap.put("Aux", output);
						s_logger.info("		{} = {}", "Aux", output);
					}
				}
				else if (entry.getKey().equals("Pb1")) {
					int data = ((int[]) entry.getValue())[0];
					if ((data & 0x0003) == 0x0001) {
						filteredMap.put("Pb1", "LowValuePb1");
						s_logger.info("		{} = {}", "Pb1", "LowValuePb1");
					} else if ((data & 0x0003) == 0x0002) {
						filteredMap.put("Pb1", "HighValuePb1");
						s_logger.info("		{} = {}", "Pb1", "HighValuePb1");
					} else if ((data & 0x0003) == 0x0003) {
						filteredMap.put("Pb1", "ErrorPb1");
						s_logger.info("		{} = {}", "Pb1", "ErrorPb1");
					}
				}
				else if (entry.getKey().equals("Pb2")) {
					int data = ((int[]) entry.getValue())[0];
					if ((data & 0x0003) == 0x0001) {
						filteredMap.put("Pb2", "LowValuePb2");
						s_logger.info("		{} = {}", "Pb2", "LowValuePb2");
					} else if ((data & 0x0003) == 0x0002) {
						filteredMap.put("Pb2", "HighValuePb2");
						s_logger.info("		{} = {}", "Pb2", "LowValuePb2");
					} else if ((data & 0x0003) == 0x0003) {
						filteredMap.put("Pb2", "ErrorPb2");
						s_logger.info("		{} = {}", "Pb2", "LowValuePb2");
					}
				}
				else if (entry.getKey().equals("Alarms")) {
					int data = ((int[]) entry.getValue())[0];
					if ((data & 0x0800) == 0x0800) {
						filteredMap.put("Alarms", "OpenDoor");
						s_logger.info("		{} = {}", "Alarms", "OpenDoor");
					} else if ((data & 0x1000) == 0x1000) {
						filteredMap.put("Alarms", "SevereAlarm");
						s_logger.info("		{} = {}", "Alarms", "SevereAlarm");
					} else if ((data & 0x2000) == 0x2000) {
						filteredMap.put("Alarms", "RTCFailure");
						s_logger.info("		{} = {}", "Alarms", "RTCFailure");
					} else if ((data & 0x4000) == 0x4000) {
						filteredMap.put("Alarms", "EEPROMFailure");
						s_logger.info("		{} = {}", "Alarms", "EEPROMFailure");
					}
				}
			}			
			publishMessage(filteredMap, topic);
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
		
	}

	private boolean isConfigured(ModbusConfiguration config) {
//		if (config.getInterval() != 0 && !config.getTopic().isEmpty() && !config.getType().isEmpty() && !config.getMetrics().isEmpty())
			return true;
//		else
//			return false;
	}
	
	private void publishMessage(Map<String, Object> props, String topic){
		
		int qos = 0;
		boolean retain = false; 
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		for (Entry<String, Object> entry : props.entrySet()) {
			if (entry.getValue() instanceof boolean[]) {
				if (((boolean[]) entry.getValue()).length == 1) {
					payload.addMetric(entry.getKey(), ((boolean[]) entry.getValue())[0]);
				} else {
					for (int i = 0; i < ((boolean[]) entry.getValue()).length; i++) {
						payload.addMetric(entry.getKey() + "." + i, ((boolean[]) entry.getValue())[i]);
					}
				}
			} else if (entry.getValue() instanceof int[]) {
				if (((int[]) entry.getValue()).length == 1) {
					payload.addMetric(entry.getKey(), ((int[]) entry.getValue())[0]);
				} else {
					for (int i = 0; i < ((int[]) entry.getValue()).length; i++) {
						payload.addMetric(entry.getKey() + "." + i, ((int[]) entry.getValue())[i]);
					}
				}
			} else if (entry.getValue() instanceof float[]) {
				if (((float[]) entry.getValue()).length == 1) {
					payload.addMetric(entry.getKey(), ((float[]) entry.getValue())[0]);
				} else {
					for (int i = 0; i < ((float[]) entry.getValue()).length; i++) {
						payload.addMetric(entry.getKey() + "." + i, ((float[]) entry.getValue())[i]);
					}
				}
			} else {
				payload.addMetric(entry.getKey(), (String) entry.getValue());
			}
		}
		
		try {
			cloudClient.publish(topic, payload, qos, retain);
		} catch (KuraException e) {
			s_logger.error("Unable to publish message", e);
		}
		
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
				if(serialMode.equalsIgnoreCase("RS232") || serialMode.equalsIgnoreCase("RS485")) {
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

	@Override
	public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
		s_logger.info("EDC control message received on topic: {}", appTopic);
		
		if (appTopic.contains(CTRL_TOPIC) && msg.getMetric("command") != null) {
			String[] tokens = ((String) msg.getMetric("command")).split(",");
			// command=slaveAddress,functionCode,registerAddress,data;
			int slaveAddress = Integer.parseInt(tokens[0]);
			int registerAddress = Integer.parseInt(tokens[2]);
			int[] data = new int[tokens.length-3];
			for (int i = 3; i < tokens.length; i++)
				data[i-3] = Integer.parseInt(tokens[i]);

//			int data = Integer.parseInt(tokens[3]);
			try {
				writeMultipleRegister(slaveAddress, registerAddress, data);
//				writeSingleRegister(slaveAddress, registerAddress, data);
			} catch (ModbusProtocolException e) {
				s_logger.error("Unable to write to device", e);
			}
		}
		
	}
	
	private synchronized void writeMultipleRegister(int unitAddr, int dataAddress, int[] data) throws ModbusProtocolException {
		ModbusManager.protocolDevice.writeMultipleRegister(unitAddr, dataAddress, data);
	}

	private synchronized void writeSingleRegister(int unitAddr, int dataAddress, int data) throws ModbusProtocolException {
		ModbusManager.protocolDevice.writeSingleRegister(unitAddr, dataAddress, data);
	}
	
	@Override
	public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
		// Not implemented
	}

	@Override
	public void onConnectionLost() {
		// Not implemented
	}

	@Override
	public void onConnectionEstablished() {
		// Not implemented
	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		// Not implemented
	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		// Not implemented
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
		// Perform scan... currently not supported.
	}
	
}