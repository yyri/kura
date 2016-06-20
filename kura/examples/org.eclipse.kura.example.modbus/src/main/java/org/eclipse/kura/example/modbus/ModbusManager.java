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


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.protocol.modbus.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusManager implements ConfigurableComponent, KuraChangeListener {

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusManager.class);

	private static final String APP_ID = "ModbusManager";
	private static final String PROP_MODBUS_CONF = "modbus.variables.file";
	private static final String PROP_SERIAL_MODE = "serialMode";
	private static final String PROP_PORT = "port";
	private static final String PROP_IP = "ipAddress";
	private static final String PROP_BAUDRATE = "baudRate";
	private static final String PROP_BITPERWORD = "bitsPerWord";
	private static final String PROP_STOPBITS = "stopBits";
	private static final String PROP_PARITY = "parity";

	private CloudService m_cloudService;
	private CloudClient m_cloudClient;
	public static ModbusProtocolDeviceService m_protocolDevice;

	private ScheduledExecutorService m_executor;

	private Map<String, Object> m_properties;
	private static Properties modbusProperties;
	
	private boolean configured;

	private List<ModbusWorker> m_workers = new ArrayList<ModbusWorker>();

	public ModbusManager() {
		m_executor = Executors.newScheduledThreadPool(5);
	}

	protected void setCloudService(CloudService cloudService) {
		m_cloudService = cloudService;
	}

	protected void unsetCloudService(CloudService cloudService) {
		m_cloudService = null;
	}

	public void setModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
		this.m_protocolDevice = modbusService;
	}
	
	public void unsetModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
		this.m_protocolDevice = null;
	}
	
	protected void activate(ComponentContext ctx, Map<String, Object> properties) {
		s_logger.info("Activating ModbusManager...");

		configured = false;
		
		try {
			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
			updated(ctx, properties);
		} catch (KuraException e) {
			s_logger.error("Configuration update failed", e);
		}
	}

	protected void deactivate(ComponentContext ctx) {
		s_logger.info("Deactivating ModbusManager...");

		for (ModbusWorker w : m_workers) {
			w.stop();
		}
		m_workers.clear();

		if (m_executor != null)
			m_executor.shutdown();
		
		if(m_protocolDevice!=null)
			try {
				m_protocolDevice.disconnect();
			} catch (ModbusProtocolException e) {
				s_logger.error("Unable to disconnect from modbus device.", e);
			}
		
		configured = false;
		
		m_cloudClient.release();
	}

	protected void updated(ComponentContext ctx, Map<String, Object> properties) {
		s_logger.info("Updating ModbusManager...");

		m_properties = properties;
		modbusProperties = getModbusProperties();
		configured=false;

		doWork();

	}

	private void doWork() {

		for (ModbusWorker w : m_workers) {
			w.stop();
		}
		m_workers.clear();

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
					parseConfiguration();
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
			for (Entry<String,Object> entry : map.entrySet()) {
				s_logger.info("		{} = {}", entry.getKey(), entry.getValue());
			}
			if (enableFilter) 
				filter(map);
			publishMessage(map, topic);
		}
	}

	private void parseConfiguration() {
		// The modbus variables have the following syntax
		//		interval=;
		//		type=event|polling;
		//		<metricName>=slaveAddress,functionCode,registerAddress,length,topic;
		//		<metricName>=slaveAddress,functionCode,registerAddress,length,topic;
		//		...
		//		#
		
		File configurationFile = new File((String) m_properties.get(PROP_MODBUS_CONF));
		Scanner scanner = null;
		try {
			scanner = new Scanner(configurationFile);
			ModbusConfiguration config = new ModbusConfiguration();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] tokens = line.split(";");
				String[] keyValue;
				String[] values;
				for (String token : tokens) {
					if (line.trim().isEmpty()) {
						continue;
					} else if (token.startsWith("#")) {
						if (isConfigured(config)) {
							m_workers.add(new ModbusWorker(config, m_executor, this));
						} else {
							s_logger.warn("Modbus configuration not correct.");
						}
						config.clear();
					} else if (token.toLowerCase().startsWith("interval")) {
						keyValue = token.split("=");
						if (keyValue.length == 2)
							config.setInterval(Integer.parseInt(keyValue[1]));
					} else if (token.toLowerCase().startsWith("type")) {
						keyValue = token.split("=");
						if ((keyValue.length == 2) && ("event".equals(keyValue[1]) || "polling".equals(keyValue[1])))
							config.setType(keyValue[1]);
					} else {
						keyValue = token.split("=");
						if (keyValue.length == 2) {
							values = keyValue[1].split(",");
							Metric m = new Metric(keyValue[0], Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]), Integer.parseInt(values[3]));
							config.addMetric(m);
							config.setTopic(values[4].trim().toLowerCase());
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			s_logger.error("Configuration file not found", e);
		} finally {
			if (scanner != null)
				scanner.close();
		}
	}

	private boolean isConfigured(ModbusConfiguration config) {
		if (config.getInterval() != 0 && !config.getTopic().isEmpty() && !config.getType().isEmpty() && !config.getMetrics().isEmpty())
			return true;
		else
			return false;
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
			}
		}
		
		try {
			m_cloudClient.publish(topic, payload, qos, retain);
		} catch (KuraException e) {
			s_logger.error("Unable to publish message", e);
		}
		
	}
	
	private Properties getModbusProperties() {
		Properties prop = new Properties();

		if(m_properties!=null){
			String portName = null;
			String serialMode = null;
			String baudRate = null;
			String bitsPerWord = null;
			String stopBits = null;
			String parity = null;
			String ipAddress = null;
			String mode= null;
			String timeout= null;
			if(m_properties.get("transmissionMode") != null) 
				mode = (String) m_properties.get("transmissionMode");
			if(m_properties.get("respTimeout") != null) 
				timeout	= (String) m_properties.get("respTimeout");
			if(m_properties.get(PROP_PORT) != null) 
				portName = (String) m_properties.get(PROP_PORT);
			if(m_properties.get(PROP_SERIAL_MODE) != null) 
				serialMode = (String) m_properties.get(PROP_SERIAL_MODE);
			if(m_properties.get(PROP_BAUDRATE) != null) 
				baudRate = (String) m_properties.get(PROP_BAUDRATE);
			if(m_properties.get(PROP_BITPERWORD) != null) 
				bitsPerWord = (String) m_properties.get(PROP_BITPERWORD);
			if(m_properties.get(PROP_STOPBITS) != null) 
				stopBits = (String) m_properties.get(PROP_STOPBITS);
			if(m_properties.get(PROP_PARITY) != null) 
				parity = (String) m_properties.get(PROP_PARITY);
			if(m_properties.get(PROP_IP) != null) 
				ipAddress = (String) m_properties.get(PROP_IP);
			
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
		if(m_protocolDevice!=null){
			m_protocolDevice.disconnect();

			m_protocolDevice.configureConnection(modbusProperties);

			configured = true;
		}
	}
	
}