package org.eclipse.kura.example.modbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.example.modbus.register.ModbusResources;
import org.eclipse.kura.example.modbus.register.Register;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusWorker {

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusWorker.class);
	
	private List<ModbusResources> resources;
	private ModbusRunner runner = new ModbusRunner();
	private List<Metric> data;
	
	private List<Metric> m_metrics;
	private Map<String,Object> m_data;
	private Map<String,Object> m_oldData;
	private int m_interval;
	private String m_topic;
	private boolean m_onEvent;
	private ScheduledFuture<?> m_handle;
	private ModbusRunner m_runner = new ModbusRunner();
	private volatile boolean m_stopIt = false;
	private KuraChangeListener listener;
	private boolean changed;


	public ModbusWorker(ModbusConfiguration config, ScheduledExecutorService executor, KuraChangeListener listener) {

		this.resources = config.getRegisters();
		this.data = new ArrayList<Metric>();
		this.listener = listener;
		
		
//		this.m_metrics = new ArrayList<Metric>();
////		for (Metric metric : config.getMetrics())
////			this.m_metrics.add(metric);

//		this.m_interval = config.getInterval();
//		this.m_listener = listener;
////		this.m_topic = config.getTopic();
////		this.m_onEvent = "event".equals(config.getType()) ? true : false;
//		this.m_data = new HashMap<String,Object>();
//		this.m_oldData = new HashMap<String,Object>();
//		this.changed = false;

		s_logger.info("Scheduling worker...");
		m_handle = executor.scheduleAtFixedRate(runner, 2000, config.getInterval(), TimeUnit.MILLISECONDS);
	}

	public void stop(){
		s_logger.info("Stopping worker...");
		m_stopIt = true;
		if(m_handle != null){
			m_handle.cancel(true);
		}
		changed = false;
	}

	private class ModbusRunner implements Runnable {

		@Override
		public void run() {
			if(!m_stopIt) {

				data.clear();
				
				try {
					for (ModbusResources mr : resources) {
						for (Integer slaveAddress : mr.getSlaveAddress()) {
							if ("C".equals(mr.getType())) {
								getMetric(readCoils(slaveAddress, Integer.parseInt(mr.getRegisterAddress(),16), mr.getRegisters().size()), mr, slaveAddress);
							} else if ("DI".equals(mr.getType())) {
								getMetric(readDiscreteInputs(slaveAddress, Integer.parseInt(mr.getRegisterAddress(),16), mr.getRegisters().size()), mr, slaveAddress);
							} else if ("HR".equals(mr.getType())) {
								getMetric(readHoldingRegisters(slaveAddress, Integer.parseInt(mr.getRegisterAddress(),16), mr.getRegisters().size()), mr, slaveAddress);
							} else if ("IR".equals(mr.getType())) {
								getMetric(readInputRegisters(slaveAddress, Integer.parseInt(mr.getRegisterAddress(),16), mr.getRegisters().size()), mr, slaveAddress);
							}
						}
					}
				} catch (ModbusProtocolException e) {
					s_logger.error("Unable to read from Modbus.", e);
				}
				
				if (!data.isEmpty())
					listener.stateChanged(data);
				
			}
		}

		private synchronized boolean[] readCoils(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
			return ModbusManager.protocolDevice.readCoils(unitAddr, dataAddress, count);
		}

		private synchronized boolean[] readDiscreteInputs(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
			return ModbusManager.protocolDevice.readDiscreteInputs(unitAddr, dataAddress, count);
		}

		private synchronized int[] readHoldingRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
			return ModbusManager.protocolDevice.readHoldingRegisters(unitAddr, dataAddress, count);
		}
		private synchronized int[] readInputRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
			return ModbusManager.protocolDevice.readInputRegisters(unitAddr, dataAddress, count);
		}

		private boolean isDataChanged(String metricName, boolean[] data) {
			boolean[] oldData = (boolean[]) m_oldData.get(metricName);
			if (oldData == null)
				return true;
			for (int i = 0; i < oldData.length; i++) {
				if (oldData[i] != data[i])
					return true;
			}
			return false;
		}
		
		private boolean isDataChanged(String metricName, int[] data) {
			int[] oldData = (int[]) m_oldData.get(metricName);
			if (oldData == null)
				return true;
			for (int i = 0; i < oldData.length; i++) {
				if (oldData[i] != data[i])
					return true;
			}
			return false;
		}
		
		private void getMetric(int[] modbusData, ModbusResources resource, Integer slaveAddress) {
			List<Register> rList = resource.getRegisters();
			for (int i = 0; i < rList.size(); i++) {
				Register r = rList.get(i);
				if ("int16".equals(r.getType())) {
					Float result = modbusData[i] * r.getScale() + r.getOffset();
					data.add(new Metric(r.getName(), r.getPublishGroup(), slaveAddress, result));
				} else if ("bitmap16".equals(r.getType())) {
					// TBD
				}
			}
		}

		private void getMetric(boolean[] modbusData, ModbusResources resource, Integer slaveAddress) {
			
		}
	}

}
