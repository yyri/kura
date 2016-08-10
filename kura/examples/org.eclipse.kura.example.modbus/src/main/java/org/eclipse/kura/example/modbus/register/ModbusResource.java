package org.eclipse.kura.example.modbus.register;

public class ModbusResource {

	private String registerAddress;
	private String type;
	private Register register;
	
	public ModbusResource() {
		this.registerAddress = "";
		this.type = "";
		this.register = new Register();
	}
	
	public String getRegisterAddress() {
		return registerAddress;
	}
	
	public void setRegisterAddress(String registerAddress) {
		this.registerAddress = registerAddress;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public Register getRegister() {
		return register;
	}
	
	public void setRegister(Register register) {
		this.register = register;
	}
	
}
