package org.eclipse.kura.example.modbus;

public class Metric {

	private String metricName;
	private String publishGroup;
	private Integer slaveAddress;
	private Object data;

	public Metric() {
		this.metricName = "";
		this.publishGroup = "";
		this.slaveAddress = -1;
	}
	
	public Metric(String metricName, String publishGroup, int slaveAddress, Object data) {
		this.metricName = metricName;
		this.publishGroup = publishGroup;
		this.slaveAddress = slaveAddress;
		this.data = data;
	}
	
	public Metric(Metric metric) {
		this.metricName = new String(metric.getMetricName());
		this.publishGroup = new String(metric.getPublishGroup());
		this.slaveAddress = new Integer(metric.getSlaveAddress());
		if (metric.getData() instanceof Float) 
			this.data = new Float((Float) metric.getData());
		else if (metric.getData() instanceof Boolean) 
			this.data = new Boolean((Boolean) metric.getData());
		else if (metric.getData() instanceof String)
			this.data = new String((String) metric.getData());
	}
	
	public String getMetricName() {
		return metricName;
	}
	
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}
	
	public String getPublishGroup() {
		return publishGroup;
	}
	
	public void setPublishGroup(String publishGroup) {
		this.publishGroup = publishGroup;
	}
	public Integer getSlaveAddress() {
		return slaveAddress;
	}
	
	public void setSlaveAddress(int slaveAddress) {
		this.slaveAddress = slaveAddress;
	}
	
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
