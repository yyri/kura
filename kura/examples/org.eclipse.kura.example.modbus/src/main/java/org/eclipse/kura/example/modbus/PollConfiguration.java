package org.eclipse.kura.example.modbus;

import java.util.ArrayList;
import java.util.List;

public class PollConfiguration {
	
	private String name = "";
	private int interval = 0;
	private ArrayList<PollResources> resources;
	
	public PollConfiguration() {
		this.name = "";
		this.interval = 0;
		this.resources = new ArrayList<PollResources>();
	}
	
	public PollConfiguration(String name, int interval) {
		this.name = name;
		this.interval = interval;
		this.resources = new ArrayList<PollResources>();
	}
	
	public void addRegister(PollResources register) {
		this.resources.add(register);
	}
	
	public void addResources(List<PollResources> resources) {
		this.resources.addAll(resources);
	}

	public List<PollResources> getResources() {
		return this.resources;
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
		this.resources.clear();
	}
}
