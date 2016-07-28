package org.eclipse.kura.example.modbus.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.kura.example.modbus.ModbusConfiguration;
import org.eclipse.kura.example.modbus.ModbusManager;
import org.eclipse.kura.example.modbus.PublishConfiguration;
import org.eclipse.kura.example.modbus.register.Command;
import org.eclipse.kura.example.modbus.register.Field;
import org.eclipse.kura.example.modbus.register.ModbusResources;
import org.eclipse.kura.example.modbus.register.Option;
import org.eclipse.kura.example.modbus.register.Register;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ModbusConfigParser {
	
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusManager.class);
	
	private static Document doc;
	
	public ModbusConfigParser() {
		// Empty constructor
	}
	
	public static void parse(String xml) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(xml));
			doc = dBuilder.parse(is);
		} catch (Exception e) {
			s_logger.error("Parse scan configuration file failed.", e);
		}
	}

	public static List<ModbusConfiguration> getPollGroups() {
		
		ArrayList<ModbusConfiguration> modbusConfigurations = new ArrayList<ModbusConfiguration>();
		Element configuration = doc.getDocumentElement();
		configuration.normalize();
		
		NodeList polls = doc.getElementsByTagName("pollGroups").item(0).getChildNodes();
		for (int index = 0; index < polls.getLength(); index++) {
			Node node = polls.item(index);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element pollGroup = (Element) node;
				String name = pollGroup.getElementsByTagName("name").item(0).getTextContent();
				String interval = pollGroup.getElementsByTagName("pollInterval").item(0).getTextContent();
				ModbusConfiguration modbusConfiguration = new ModbusConfiguration(name, Integer.parseInt(interval));
				modbusConfigurations.add(modbusConfiguration);
			}
		}
		
		return modbusConfigurations;
	}
	
	public static Map<String,List<ModbusResources>> getModbusResources(Map<String, List<Integer>> devices) {
		
		HashMap<String,List<ModbusResources>> modbusResourcesMap = new HashMap<String,List<ModbusResources>>();
		Element configuration = doc.getDocumentElement();
		configuration.normalize();
		
		NodeList assets = doc.getElementsByTagName("assets").item(0).getChildNodes();
		Element asset;
		String model;
		for (int i = 0; i < assets.getLength(); i++) {
			Node node = assets.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				asset = (Element) node;
				model = asset.getElementsByTagName("model").item(0).getTextContent();
				if (!devices.containsKey(model)) {
					continue;
				}

				NodeList resources = asset.getChildNodes();
				for (int j = 0; j < resources.getLength(); j++) {
					Node resource = resources.item(j); // Holding register or coils
					if ("holdingRegisters".equals(resource.getNodeName())) {
						addModbusResources(resource.getChildNodes(), modbusResourcesMap, devices.get(model), "HR");
					} else if ("coils".equals(resource.getNodeName())) {
						addModbusResources(resource.getChildNodes(), modbusResourcesMap, devices.get(model), "C");
					}
				}
			}
		
		}
		
		return modbusResourcesMap;
	}

	public static List<PublishConfiguration> getPublishGroups() {
		
		ArrayList<PublishConfiguration> publishConfigurations = new ArrayList<PublishConfiguration>();
		Element configuration = doc.getDocumentElement();
		configuration.normalize();
		
		NodeList publishes = doc.getElementsByTagName("publishGroups").item(0).getChildNodes();
		for (int index = 0; index < publishes.getLength(); index++) {
			Node node = publishes.item(index);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element publishGroup = (Element) node;
				PublishConfiguration publishConfiguration = new PublishConfiguration();
				publishConfiguration.setName(publishGroup.getElementsByTagName("name").item(0).getTextContent());
				publishConfiguration.setInterval(Integer.parseInt(publishGroup.getElementsByTagName("publishInterval").item(0).getTextContent()));
				publishConfiguration.setOnChange(new Boolean(publishGroup.getElementsByTagName("onChange").item(0).getTextContent()));
				publishConfiguration.setTopic(publishGroup.getElementsByTagName("topic").item(0).getTextContent());
				publishConfiguration.setQos(Integer.parseInt(publishGroup.getElementsByTagName("qos").item(0).getTextContent()));
				publishConfigurations.add(publishConfiguration);
			}
		}
		
		return publishConfigurations;
	}
	
	public static Map<String,Command> getCommands(String model) {
		Map<String,Command> commands = null;
		Element configuration = doc.getDocumentElement();
		configuration.normalize();
		
		NodeList assets = doc.getElementsByTagName("assets").item(0).getChildNodes();
		Element asset;
		for (int i = 0; i < assets.getLength(); i++) {
			Node node = assets.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				asset = (Element) node;
				if (!model.equals(asset.getElementsByTagName("model").item(0).getTextContent())) {
					continue;
				}

				NodeList resources = asset.getChildNodes();
				for (int j = 0; j < resources.getLength(); j++) {
					Node resource = resources.item(j); // Holding register or coils
					// Commands can be null!
					if ("holdingRegisters".equals(resource.getNodeName())) {
						if (((Element) resource).getElementsByTagName("commands").item(0) != null) {
							commands = getCommands(((Element) resource).getElementsByTagName("commands").item(0), "HR");
						}
					} else if ("coils".equals(resource.getNodeName())) {
						if (((Element) resource).getElementsByTagName("commands").item(0) != null) {
							commands = getCommands(((Element) resource).getElementsByTagName("commands").item(0), "C");
						}
					}
				}
			}
		
		}
		
		return commands;
	}
	
	private static void addModbusResources(NodeList registersList, HashMap<String, List<ModbusResources>> modbusResourcesMap, List<Integer> slaveAddresses, String type) {
		for (int k = 0; k < registersList.getLength(); k++) {
			Node registers = registersList.item(k);
			if (registers.getNodeType() == Node.ELEMENT_NODE && "registers".equals(registers.getNodeName())) {
				Element registersElement = (Element) registers;
				ModbusResources modbusResources = new ModbusResources();
				modbusResources.setRegisterAddress(registersElement.getElementsByTagName("address").item(0).getTextContent());
				modbusResources.setSlaveAddress(slaveAddresses);
				modbusResources.setType(type);
				NodeList registerList = registers.getChildNodes();
				for (int t = 0; t < registerList.getLength(); t++) {
					Node registerNode = registerList.item(t);
					if (registerNode.getNodeType() == Node.ELEMENT_NODE && "register".equals(registerNode.getNodeName())) {
						Register r = createRegister((Element) registerNode);
						// Add only enabled registers
						if (!r.isDisabled())
							modbusResources.addRegister(r);
					}
				}
				// Add only modbus configuration with registers
				if (!modbusResources.getRegisters().isEmpty()) {
					if (modbusResourcesMap.get(registersElement.getElementsByTagName("pollGroup").item(0).getTextContent()) == null) {
						ArrayList<ModbusResources> modbusResourcesList = new ArrayList<ModbusResources>();
						modbusResourcesList.add(modbusResources);
						modbusResourcesMap.put(registersElement.getElementsByTagName("pollGroup").item(0).getTextContent(), modbusResourcesList);
					} else {
						modbusResourcesMap.get(registersElement.getElementsByTagName("pollGroup").item(0).getTextContent()).add(modbusResources);
					}
				}
			}
		}
	}
	
	private static Register createRegister(Element register) {
		Register r = new Register();
		r.setId(Integer.parseInt(register.getElementsByTagName("id").item(0).getTextContent()));
		r.setAccess(register.getElementsByTagName("access").item(0).getTextContent());
		r.setPublishGroup(register.getElementsByTagName("publishGroup").item(0).getTextContent());
		r.setType(register.getElementsByTagName("type").item(0).getTextContent());
		r.setDisabled("true".equals(register.getElementsByTagName("disabled").item(0).getTextContent()) ? true : false );
		if ("int16".equals(r.getType())) {
			r.setName(register.getElementsByTagName("name").item(0).getTextContent());
			r.setScale(Float.parseFloat(register.getElementsByTagName("scale").item(0).getTextContent()));
			r.setOffset(Float.parseFloat(register.getElementsByTagName("offset").item(0).getTextContent()));
			r.setUnit(register.getElementsByTagName("offset").item(0).getTextContent());
			r.setMin(Float.parseFloat(register.getElementsByTagName("min").item(0).getTextContent()));
			r.setMax(Float.parseFloat(register.getElementsByTagName("max").item(0).getTextContent()));
		} else if ("bitmap16".equals(r.getType())) {
			NodeList fields = ((Element) register.getElementsByTagName("fields").item(0)).getChildNodes();
			for (int w = 0; w < fields.getLength(); w++) {
				Node fieldNode = fields.item(w);
				Element fieldElement;
				Field field = null;
				if (fieldNode.getNodeType() == Node.ELEMENT_NODE) {
					fieldElement = (Element) fieldNode;
					field = new Field(fieldElement.getElementsByTagName("mask").item(0).getTextContent(),
							Integer.parseInt(fieldElement.getElementsByTagName("shift").item(0).getTextContent()),
							fieldElement.getElementsByTagName("name").item(0).getTextContent());
					NodeList options = ((Element) fieldNode).getElementsByTagName("options");
					if (options.getLength() > 0) {
						options = options.item(0).getChildNodes();
						for (int y = 0; y < options.getLength(); y++) {
							Element optionElement;
							Node optionNode = options.item(y);
							if (optionNode.getNodeType() == Node.ELEMENT_NODE) {
								optionElement = (Element) optionNode;
								Option option = new Option(optionElement.getElementsByTagName("name").item(0).getTextContent(),
										optionElement.getElementsByTagName("value").item(0).getTextContent());
								field.addOption(option);
							}
						}
					}
				}
				if (field != null)
					r.addField(field);
			}
		}
		return r;
	}
	
	private static Map<String,Command> getCommands(Node commandsNode, String type) {
		Map<String,Command> commands = new HashMap<String,Command>();
		NodeList commandNodeList = commandsNode.getChildNodes();
		for (int i = 0; i < commandNodeList.getLength(); i++) {
			Node commandNode = commandNodeList.item(i);
			Element commandElement;
			if (commandNode.getNodeType() == Node.ELEMENT_NODE) {
				commandElement = (Element) commandNode;
				NodeList fields = commandElement.getElementsByTagName("fields").item(0).getChildNodes();
				for (int j = 0; j < fields.getLength(); j++) {
					Node fieldNode = fields.item(j);
					Element fieldElement;
					if (fieldNode.getNodeType() == Node.ELEMENT_NODE) {
						fieldElement = (Element) fieldNode;
						Command c = new Command();
						c.setType(type);
						c.setAddress(Integer.parseInt(commandElement.getElementsByTagName("address").item(0).getTextContent(),16));
						c.setCommandName(fieldElement.getElementsByTagName("name").item(0).getTextContent());
						if (fieldElement.getElementsByTagName("value").item(0).getTextContent().isEmpty()) {
							// If the value is null, the command will set a analog register with a scale and offset
							c.setCommandValue(null);
							c.setScale(Float.parseFloat(fieldElement.getElementsByTagName("scale").item(0).getTextContent()));
							c.setOffset(Float.parseFloat(fieldElement.getElementsByTagName("offset").item(0).getTextContent()));
						} else {
							c.setCommandValue(Integer.parseInt(fieldElement.getElementsByTagName("value").item(0).getTextContent(),16));
						}
						commands.put(c.getCommandName(),c);
					}
				}
			}
		}
		
		return commands;
	}
}
