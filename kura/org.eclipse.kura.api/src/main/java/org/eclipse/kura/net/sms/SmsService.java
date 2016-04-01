package org.eclipse.kura.net.sms;

import java.util.Collection;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.modem.ModemDevice;
import org.osgi.service.io.ConnectionFactory;

public interface SmsService extends ConnectionFactory {
	
	public void addModem(ModemDevice modem) throws KuraException;
	
	public void removeModem(ModemDevice modem) throws KuraException;
	
	public SmsMessage[] readMessages() throws KuraException;
	
	public boolean sendMessage(SmsMessage sms) throws KuraException;
	
	public int sendMessages(Collection<SmsMessage> msgList) throws KuraException;
	
	public boolean queueMessage(SmsMessage sms) throws KuraException;
	
	public int queueMessages(Collection<SmsMessage> msgList) throws KuraException;
	
	// Aggiungere altri metodi.
	
	
}
