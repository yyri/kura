package org.eclipse.kura.example.modbus.register;

import org.eclipse.kura.example.modbus.ModbusManager;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusHandler {

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusHandler.class);
	
	private ModbusHandler() {
		// Empty constructor
	}
	
	public static synchronized void writeSingleRegister(int unitAddr, int dataAddress, int data) throws ModbusProtocolException {
		ModbusManager.protocolDevice.writeSingleRegister(unitAddr, dataAddress, data);
	}
	
	public static synchronized void writeMultipleRegister(int unitAddr, int dataAddress, int[] data) throws ModbusProtocolException {
		ModbusManager.protocolDevice.writeMultipleRegister(unitAddr, dataAddress, data);
	}
	
	public static synchronized void writeSingleCoil(int unitAddr, int dataAddress, boolean data) throws ModbusProtocolException {
		ModbusManager.protocolDevice.writeSingleCoil(unitAddr, dataAddress, data);
	}
	
	public static synchronized void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data) throws ModbusProtocolException {
		ModbusManager.protocolDevice.writeMultipleCoils(unitAddr, dataAddress, data);
	}
	
	public static synchronized Object readModbus(String type, Integer slaveAddress, Integer registerAddress, Integer count) throws ModbusProtocolException {
		Object result = null;
		try {
			Thread.sleep(500);
			if ("C".equals(type)) {
				result = ModbusManager.protocolDevice.readCoils(slaveAddress, registerAddress, count);
			} else if ("DI".equals(type)) {
				result = ModbusManager.protocolDevice.readDiscreteInputs(slaveAddress, registerAddress, count);
			} else if ("HR".equals(type)) {
				result = ModbusManager.protocolDevice.readHoldingRegisters(slaveAddress, registerAddress, count);
				// Force short conversion and back to recover sign
				for (int i = 0; i < ((int[]) result).length; i++) {
					short r = (short) ((int[]) result)[i];
					((int[]) result)[i] = r;
				}				
			} else if ("IR".equals(type)) {
				result = ModbusManager.protocolDevice.readInputRegisters(slaveAddress, registerAddress, count);
				// Force short conversion and back to recover sign
				for (int i = 0; i < ((int[]) result).length; i++) {
					short r = (short) ((int[]) result)[i];
					((int[]) result)[i] = r;
				}	
			} else {
				result = null;
			}
		} catch (InterruptedException e) {
			s_logger.error("Failed to wait for Modbus.", e);
		}
		return result;
	}
	
}
