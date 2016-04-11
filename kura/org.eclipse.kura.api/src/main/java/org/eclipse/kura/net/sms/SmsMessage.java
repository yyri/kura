package org.eclipse.kura.net.sms;

import java.util.Date;

public class SmsMessage {

	public enum MessageType
	{
		INBOUND,
		OUTBOUND,
		UNKNOWN
	}
	
	private String messageUUID;  // message uuid
	private String modemID;      // identification number for the modem
	private String content;      // content of the message
	private String endpoint;     // sender or receiver
	private MessageType type;    // type of message
	private Date date;           // creation date
	private int memIndex;        // GSM Modem memory index (only for INBOUND messages)
	private String memLocation;  // GSM Modem memory location (only for INBOUND messages)

	public SmsMessage() {
		this.modemID = "";
		this.messageUUID = "";
		this.content = "";
		this.endpoint = "";
		this.type = MessageType.UNKNOWN;
		this.date = new Date();
		this.memIndex = -1;
		this.memLocation = "";
	}
	
	public SmsMessage(String modemID, String content, String endpoint, MessageType type, Date date) {
		this.modemID = modemID;
		this.messageUUID = "";
		this.content = content;
		this.endpoint = endpoint;
		this.type = type;
		this.date = date;
		this.memIndex = -1;
		this.memLocation = "";
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	
	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public String getMessageUUID() {
		return messageUUID;
	}

	public void setMessageUUID(String messageUUID) {
		this.messageUUID = messageUUID;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getModemID() {
		return modemID;
	}

	public void setModemID(String modemID) {
		this.modemID = modemID;
	}

	public int getMemIndex() {
		return memIndex;
	}

	public void setMemIndex(int memIndex) {
		this.memIndex = memIndex;
	}

	public String getMemLocation() {
		return memLocation;
	}

	public void setMemLocation(String memLocation) {
		this.memLocation = memLocation;
	}

}
