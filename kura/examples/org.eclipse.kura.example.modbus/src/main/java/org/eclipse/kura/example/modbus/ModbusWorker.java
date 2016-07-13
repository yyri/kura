package org.eclipse.kura.example.modbus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusWorker {

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusWorker.class);

	private List<Metric> m_metrics;
	private Map<String,Object> m_data;
	private Map<String,Object> m_oldData;
	private int m_interval;
	private String m_topic;
	private boolean m_onEvent;
	private ScheduledFuture<?> m_handle;
	private ModbusRunner m_runner = new ModbusRunner();
	private volatile boolean m_stopIt = false;
	private KuraChangeListener m_listener;
	private boolean changed;


	public ModbusWorker(ModbusConfiguration config, ScheduledExecutorService executor, KuraChangeListener listener) {

		this.m_metrics = new ArrayList<Metric>();
//		for (Metric metric : config.getMetrics())
//			this.m_metrics.add(metric);

		this.m_interval = config.getInterval();
		this.m_listener = listener;
//		this.m_topic = config.getTopic();
//		this.m_onEvent = "event".equals(config.getType()) ? true : false;
		this.m_data = new HashMap<String,Object>();
		this.m_oldData = new HashMap<String,Object>();
		this.changed = false;

		s_logger.info("Scheduling worker...");
		m_handle = executor.scheduleAtFixedRate(m_runner, 2000, m_interval, TimeUnit.MILLISECONDS);
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

				// functionCodes :
				// 01 : readCoils
				// 02 : readDiscreteInputs
				// 03 : readHoldingRegisters
				// 04 : readInputRegisters

				m_data.clear();
				changed = false;
				if (!m_onEvent) {
					try {
						for (Metric metric : m_metrics) {
							Thread.sleep(10);
							if (metric.getFunctionCode() == 1) {
								m_data.put(metric.getMetricName(), readCoils(metric.getSlaveAddress(), metric.getRegisterAddress(), metric.getLength()));
							} else if (metric.getFunctionCode() == 2) {
								m_data.put(metric.getMetricName(), readDiscreteInputs(metric.getSlaveAddress(), metric.getRegisterAddress(), metric.getLength()));
							} else if (metric.getFunctionCode() == 3) {
								m_data.put(metric.getMetricName(), readHoldingRegisters(metric.getSlaveAddress(), metric.getRegisterAddress(), metric.getLength()));
							} else if (metric.getFunctionCode() == 4) {
								m_data.put(metric.getMetricName(), readInputRegisters(metric.getSlaveAddress(), metric.getRegisterAddress(), metric.getLength()));
							}
						}
					} catch (ModbusProtocolException e) {
						s_logger.error("Unable to read from Modbus.", e);
					} catch (InterruptedException e) {
						s_logger.error("Unable to read from Modbus.", e);
					}

					if (!m_data.isEmpty())
						m_listener.stateChanged(m_data, m_topic);
				} else {
					try {
						for (Metric metric : m_metrics) {
							if (metric.getFunctionCode() == 1) {
								boolean[] data = readCoils(metric.getSlaveAddress(), metric.getRegisterAddress(), metric.getLength());
								if (isDataChanged(metric.getMetricName(), data)) {
									m_data.put(metric.getMetricName(), data);
									m_oldData.put(metric.getMetricName(), data);
									changed = true;
								}
							} else if (metric.getFunctionCode() == 2) {
								boolean[] data = readDiscreteInputs(metric.getSlaveAddress(), metric.getRegisterAddress(), metric.getLength());
								if (isDataChanged(metric.getMetricName(), data)) {
									m_data.put(metric.getMetricName(), data);
									m_oldData.put(metric.getMetricName(), data);
									changed = true;
								}							
							} else if (metric.getFunctionCode() == 3) {
								int[] data = readHoldingRegisters(metric.getSlaveAddress(), metric.getRegisterAddress(), metric.getLength());
								if (isDataChanged(metric.getMetricName(), data)) {
									m_data.put(metric.getMetricName(), data);
									m_oldData.put(metric.getMetricName(), data);
									changed = true;
								}										
							} else if (metric.getFunctionCode() == 4) {
								int[] data = readInputRegisters(metric.getSlaveAddress(), metric.getRegisterAddress(), metric.getLength());
								if (isDataChanged(metric.getMetricName(), data)) {
									m_data.put(metric.getMetricName(), data);
									m_oldData.put(metric.getMetricName(), data);
									changed = true;
								}
							}
						}
					} catch (ModbusProtocolException e) {
						s_logger.error("Unable to read from Modbus.", e);
					}

					if (changed && !m_data.isEmpty()) {
						m_listener.stateChanged(m_data, m_topic);
					}
				}
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

	}

}
