package org.eclipse.kura.net.admin.modem;

import java.io.IOException;

import javax.microedition.io.Connection;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.modem.ModemDevice;

public interface AtConnectionFactory {
	
	public Connection createConnection(ModemDevice device, int mode, boolean timeouts) throws IOException, KuraException;
	
	public Connection createHipConnection(ModemDevice device, int mode, boolean timeouts) throws IOException, KuraException;

}
