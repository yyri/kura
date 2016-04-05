package org.eclipse.kura.net.sms;

public class SmsMessage {

	private String content;  // content of the message
	private String endpoint; // sender or receiver
	private String type;     // inbound or outbound ENUM???

	public SmsMessage() {
		content = "";
		endpoint = "";
		type = "";
	}
	
	public SmsMessage(String content, String endpoint, String type) {
		this.content = content;
		this.endpoint = endpoint;
		this.type = type;
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
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}
