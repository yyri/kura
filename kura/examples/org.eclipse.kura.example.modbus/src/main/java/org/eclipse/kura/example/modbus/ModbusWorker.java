package org.eclipse.kura.example.modbus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.example.modbus.register.Field;
import org.eclipse.kura.example.modbus.register.ModbusHandler;
import org.eclipse.kura.example.modbus.register.Option;
import org.eclipse.kura.example.modbus.register.Register;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusWorker {

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusWorker.class);
	
	private List<PollResources> resources;
	private ModbusRunner runner = new ModbusRunner();
	private List<Metric> data;
	
	private ScheduledFuture<?> handle;
	private volatile boolean stopIt = false;
	private KuraChangeListener listener;
	
	public ModbusWorker(PollConfiguration config, ScheduledExecutorService executor, KuraChangeListener listener) {

		this.resources = config.getResources();
		this.data = new ArrayList<Metric>();
		this.listener = listener;

		s_logger.info("Scheduling worker...");
		handle = executor.scheduleAtFixedRate(runner, 2000, config.getInterval(), TimeUnit.MILLISECONDS);
	}

	public void stop(){
		s_logger.info("Stopping worker...");
		stopIt = true;
		if(handle != null){
			handle.cancel(true);
		}
	}
	
	private class ModbusRunner implements Runnable {

		@Override
		public void run() {
			if(!stopIt) {

				data.clear();
				
				try {
					for (PollResources mr : resources) {
						for (Integer slaveAddress : mr.getSlaveAddress()) {
							if ("C".equals(mr.getType()) || "DI".equals(mr.getType())) {
								getMetric((boolean[]) ModbusHandler.readModbus(mr.getType(), slaveAddress, Integer.parseInt(mr.getRegisterAddress(),16), mr.getRegisters().size()), mr, slaveAddress);
							} else if ("HR".equals(mr.getType()) || "IR".equals(mr.getType())) {
								getMetric((int[]) ModbusHandler.readModbus(mr.getType(), slaveAddress, Integer.parseInt(mr.getRegisterAddress(),16), mr.getRegisters().size()), mr, slaveAddress);
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
		
		private void getMetric(int[] modbusData, PollResources resource, Integer slaveAddress) {
			for (int d : modbusData)
				s_logger.debug("Read {}", d);
			List<Register> rList = resource.getRegisters();
			for (int i = 0; i < rList.size(); i++) {
				Register r = rList.get(i);
				if ("int16".equals(r.getType())) {
					Float result = modbusData[i] * r.getScale() + r.getOffset();
					data.add(new Metric(r.getName(), r.getPublishGroup(), slaveAddress, result));
				} else if ("bitmap16".equals(r.getType())) {
					for (Field f : r.getFields()) {
						if (!f.getOptions().isEmpty()) {
							Integer value = modbusData[i] & Integer.parseInt(f.getMask(),16);
							for (Option option : f.getOptions()) {
								if (Integer.parseInt(option.getValue(),16) == value) {
									data.add(new Metric(f.getName(), r.getPublishGroup(), slaveAddress, option.getName()));
									break;
								}
							}
						} else {
							data.add(new Metric(f.getName(), r.getPublishGroup(), slaveAddress, (modbusData[i] & Integer.parseInt(f.getMask(),16)) == Integer.parseInt(f.getMask(),16) ? true : false));
						}
					}
				}
			}
		}

		private void getMetric(boolean[] modbusData, PollResources resource, Integer slaveAddress) {
			for (boolean d : modbusData)
				s_logger.debug("Read {}", d);
			List<Register> rList = resource.getRegisters();
			for (int i = 0; i < rList.size(); i++) {
				Register r = rList.get(i);
				if ("bitmap16".equals(r.getType())) {
					for (Field f : r.getFields()) {
						data.add(new Metric(f.getName(), r.getPublishGroup(), slaveAddress, modbusData[i]));
					}
				}
			}
		}
	}

}
