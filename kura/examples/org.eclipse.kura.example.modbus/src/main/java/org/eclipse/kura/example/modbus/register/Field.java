package org.eclipse.kura.example.modbus.register;

import java.util.ArrayList;
import java.util.List;

public class Field {
	
	private String mask;
	private Integer shift;
	private String name;
	private List<Option> options;
	
	public Field(String mask, Integer shift, String name) {
		this.mask = mask;
		this.shift = shift;
		this.name = name;
		this.options = new ArrayList<Option>();
	}
	
	public String getMask() {
		return mask;
	}

	public void setMask(String mask) {
		this.mask = mask;
	}

	public Integer getShift() {
		return shift;
	}

	public void setShift(Integer shift) {
		this.shift = shift;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Option> getOptions() {
		return options;
	}

	public void setOptions(List<Option> options) {
		this.options = options;
	}
	
	public void addOption(Option option) {
		this.options.add(option);
	}
	
}
