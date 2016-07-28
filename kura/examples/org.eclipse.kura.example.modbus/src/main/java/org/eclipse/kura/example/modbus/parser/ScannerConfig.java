package org.eclipse.kura.example.modbus.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScannerConfig {
	
	private Boolean disabled;
	private Integer interval;
	private Integer minRange;
	private Integer maxRange;
	private Map<String,Map<Integer,Integer>> models;
	private Map<Integer,String> assets;
	private List<Integer> blacklist;
	
	public ScannerConfig() {
		this.disabled = true;
		this.interval = 0;
		this.minRange = 0;
		this.maxRange = 255;
		this.models = new HashMap<String,Map<Integer,Integer>>();
		this.assets = new HashMap<Integer,String>();
		this.blacklist = new ArrayList<Integer>();
	}
	
	public Boolean getDisabled() {
		return disabled;
	}
	
	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}
	
	public Integer getInterval() {
		return interval;
	}
	
	public void setInterval(Integer interval) {
		this.interval = interval;
	}
	
	public Integer getMinRange() {
		return minRange;
	}
	
	public void setMinRange(Integer minRange) {
		this.minRange = minRange;
	}
	
	public Integer getMaxRange() {
		return maxRange;
	}
	
	public Map<String, Map<Integer, Integer>> getModels() {
		return models;
	}

	public void setModels(Map<String, Map<Integer, Integer>> models) {
		this.models = models;
	}

	public void setMaxRange(Integer maxRange) {
		this.maxRange = maxRange;
	}
	
	public Map<Integer,String> getAssets() {
		return assets;
	}
	
	public void setAssets(Map<Integer,String> assets) {
		this.assets = assets;
	}
	
	public void putAsset(Integer id, String model) {
		this.assets.put(id, model);
	}
	
	public List<Integer> getBlacklist() {
		return blacklist;
	}
	
	public void setBlacklist(List<Integer> blacklist) {
		this.blacklist = blacklist;
	}
	
	public void addBlacklisted(Integer blacklisted) {
		this.blacklist.add(blacklisted);
	}
	
	public void clear() {
		this.disabled = true;
		this.interval = 0;
		this.minRange = 0;
		this.maxRange = 255;
		this.assets.clear();
		this.blacklist.clear();
	}
}
