package org.eclipse.kura.example.modbus;

import java.util.List;

public interface KuraChangeListener {

	void stateChanged(List<Metric> metrics);
	
}
