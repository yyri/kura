package org.eclipse.kura.example.modbus;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.example.modbus.register.ModbusResources;

public class ModbusConfiguration {
	
	private String name = "";
	private int interval = 0;
	private ArrayList<ModbusResources> registers;
	
	public ModbusConfiguration() {
		this.name = "";
		this.interval = 0;
		this.registers = new ArrayList<ModbusResources>();
	}
	
	public ModbusConfiguration(String name, int interval) {
		this.name = name;
		this.interval = interval;
		this.registers = new ArrayList<ModbusResources>();
	}
	
	public void addRegister(ModbusResources register) {
		this.registers.add(register);
	}
	
	public void addRegisters(List<ModbusResources> registers) {
		this.registers.addAll(registers);
	}

	public List<ModbusResources> getRegisters() {
		return this.registers;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public void clear() {
		this.name = "";
		this.interval = 0;
		this.registers.clear();
	}
}
