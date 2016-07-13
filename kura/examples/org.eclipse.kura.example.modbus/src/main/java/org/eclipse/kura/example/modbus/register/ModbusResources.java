package org.eclipse.kura.example.modbus.register;

import java.util.ArrayList;
import java.util.List;

public class ModbusResources {

	private List<Integer> slaveAddresses;
	private String registerAddress;
	private String type;
	private List<Register> registers;
	
	public ModbusResources() {
		this.slaveAddresses = new ArrayList<Integer>();
		this.registerAddress = "";
		this.type = "";
		this.registers = new ArrayList<Register>();
	}
	
	public List<Integer> getSlaveAddress() {
		return slaveAddresses;
	}
	
	public void setSlaveAddress(List<Integer> slaveAddresses) {
		this.slaveAddresses = slaveAddresses;
	}
	
	public void addSlaveAddress(Integer slaveAddress) {
		this.slaveAddresses.add(slaveAddress);
	}
	
	public String getRegisterAddress() {
		return registerAddress;
	}
	
	public void setRegisterAddress(String registerAddress) {
		this.registerAddress = registerAddress;
	}
	
	public List<Register> getRegisters() {
		return registers;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public void setRegisters(List<Register> registers) {
		this.registers = registers;
	}
	
	public void addRegister(Register register) {
		this.registers.add(register);
	}
	
}
