package org.eclipse.kura.example.modbus;

public class PublishConfiguration {

	private String name;
	private Integer interval;
	private Boolean onChange;
	private String topic;
	private Integer qos;
	
	public PublishConfiguration() {
		this.name = "";
		this.interval = 0;
		this.onChange = false;
		this.topic = "";
		this.qos = 0;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Integer getInterval() {
		return interval;
	}
	
	public void setInterval(Integer interval) {
		this.interval = interval;
	}
	
	public Boolean getOnChange() {
		return onChange;
	}
	
	public void setOnChange(Boolean onChange) {
		this.onChange = onChange;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public Integer getQos() {
		return qos;
	}
	
	public void setQos(Integer qos) {
		this.qos = qos;
	}
	
}
