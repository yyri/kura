package org.eclipse.kura.example.modbus;

public class Metric {

	private String metricName;
	private int slaveAddress;
	private int functionCode;
	private int registerAddress;
	private int length;
	
	public Metric() {
		metricName = "";
		slaveAddress = 0;
		functionCode = 0;
		registerAddress = 0;
		length = 1;
	}
	
	public Metric(String metricName, int slaveAddress, int functionCode, int registerAddress, int length) {
		this.metricName = metricName;
		this.slaveAddress = slaveAddress;
		this.functionCode = functionCode;
		this.registerAddress = registerAddress;
		this.length = length;
	}
	
	public String getMetricName() {
		return metricName;
	}
	
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}
	
	public int getSlaveAddress() {
		return slaveAddress;
	}
	
	public void setSlaveAddress(int slaveAddress) {
		this.slaveAddress = slaveAddress;
	}
	
	public int getFunctionCode() {
		return functionCode;
	}
	
	public void setFunctionCode(int functionCode) {
		this.functionCode = functionCode;
	}
	
	public int getRegisterAddress() {
		return registerAddress;
	}
	
	public void setRegisterAddress(int registerAddress) {
		this.registerAddress = registerAddress;
	}
	
	public int getLength() {
		return length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}	
	
}
