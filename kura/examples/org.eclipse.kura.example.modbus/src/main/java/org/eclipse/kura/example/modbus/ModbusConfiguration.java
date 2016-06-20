package org.eclipse.kura.example.modbus;

import java.util.ArrayList;
import java.util.List;

public class ModbusConfiguration {
	
	private String topic = "";
	private int interval = 0;
	private String type = "polling";
	private ArrayList<Metric> metrics;
	
	public ModbusConfiguration() {
		this.topic = "";
		this.interval = 0;
		this.type = "polling";
		this.metrics = new ArrayList<Metric>();
	}
	
	public ModbusConfiguration(String topic, int interval, String type) {
		this.topic = topic;
		this.interval = interval;
		this.type = type;
		this.metrics = new ArrayList<Metric>(); 
	}
	
	public void addMetric(Metric metric) {
		this.metrics.add(metric);
	}
	
	public void addMetrics(List<Metric> metrics) {
		this.metrics.addAll(metrics);
	}

	public List<Metric> getMetrics() {
		return this.metrics;
	}
	
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public void clear() {
		this.topic = "";
		this.interval = 0;
		this.type = "polling";
		this.metrics.clear(); 
	}
}
