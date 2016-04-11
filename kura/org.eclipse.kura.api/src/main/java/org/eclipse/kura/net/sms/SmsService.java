package org.eclipse.kura.net.sms;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.osgi.service.io.ConnectionFactory;

public interface SmsService extends ConnectionFactory {

	public List<SmsMessage> readMessages() throws KuraException;
	
	public boolean sendMessage(SmsMessage sms) throws KuraException;
	
	public int sendMessages(Collection<SmsMessage> msgList) throws KuraException;
	
	public boolean queueMessage(SmsMessage sms) throws KuraException;
	
	public int queueMessages(Collection<SmsMessage> msgList) throws KuraException;

	public boolean queueMessageAt(SmsMessage sms, Date at) throws KuraException;
	
	public boolean queueMessageAt(SmsMessage sms, long after) throws KuraException;
	
	public boolean deleteMessage(SmsMessage sms)  throws KuraException;
	
	public boolean sendUSSDCommand(String request) throws KuraException;
	
	public int getInboundMessageCount() throws KuraException;
	
	public int getOutboundMessageCount() throws KuraException;
	
	public void setInboundSmsMessageNotification(SmsMessageNotification notification) throws KuraException;
	
	public void removeInboundSmsMessageNotification() throws KuraException;
	
	public SmsMessageNotification getInboundSmsMessageNotification() throws KuraException;
	
	public void setOutboundSmsMessageNotification(SmsMessageNotification notification) throws KuraException;
	
	public void removeOutboundSmsMessageNotification() throws KuraException;
	
	public SmsMessageNotification getOutboundSmsMessageNotification() throws KuraException;
	
	public void setUSSDNotification(USSDSmsNotification notification) throws KuraException;
	
	public void removeUSSDNotification() throws KuraException;
	
	public USSDSmsNotification getUSSDNotification() throws KuraException;
	
	public void setDeleteMessagesAfterCallback(boolean value) throws KuraException;	
	
}
