package org.eclipse.kura.example.modbus.register;

import java.util.ArrayList;
import java.util.List;

public class Register {

	private Integer id;
	private String access;
	private String publishGroup;
	private String type;
	private String name;
	private boolean disabled;
	private float scale;
	private float offset;
	private String unit;
	private float min;
	private float max;
	private List<Field> fields;

	public Register() {
		this.id = -1;
		this.access = "R";
		this.publishGroup = "";
		this.type = "";
		this.name = "";
		this.disabled = true;
		this.scale = 1F;
		this.offset = 0F;
		this.unit = "";
		this.min = 0F;
		this.max = 0F;
		this.fields = new ArrayList<Field>();
	}
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getAccess() {
		return access;
	}
	
	public void setAccess(String access) {
		this.access = access;
	}
	
	public String getPublishGroup() {
		return publishGroup;
	}
	
	public void setPublishGroup(String publishGroup) {
		this.publishGroup = publishGroup;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public float getScale() {
		return scale;
	}
	
	public void setScale(float scale) {
		this.scale = scale;
	}
	
	public float getOffset() {
		return offset;
	}
	
	public void setOffset(float offset) {
		this.offset = offset;
	}
	
	public String getUnit() {
		return unit;
	}
	
	public void setUnit(String unit) {
		this.unit = unit;
	}
	
	public float getMin() {
		return min;
	}
	
	public void setMin(float min) {
		this.min = min;
	}
	
	public float getMax() {
		return max;
	}
	
	public void setMax(float max) {
		this.max = max;
	}
	
	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}
	
	public void addField(Field field) {
		this.fields.add(field);
	}

}
