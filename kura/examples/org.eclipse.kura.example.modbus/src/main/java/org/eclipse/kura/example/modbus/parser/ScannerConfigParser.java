package org.eclipse.kura.example.modbus.parser;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.kura.example.modbus.ModbusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ScannerConfigParser {
	
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusManager.class);

	public ScannerConfigParser() {
		// Empty constructor
	}
	
	public static ScannerConfig parse(String xml) {
		
		ScannerConfig config = new ScannerConfig();
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(new String((String) xml)));
			Document doc = dBuilder.parse(is);
			doc.normalize();
			
			config.setDisabled(new Boolean(doc.getElementsByTagName("disabled").item(0).getTextContent()));
			config.setInterval(Integer.parseInt(doc.getElementsByTagName("interval").item(0).getTextContent()));
			
			Node node = doc.getElementsByTagName("range").item(0);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				config.setMinRange(Integer.parseInt(element.getElementsByTagName("min").item(0).getTextContent()));
				config.setMaxRange(Integer.parseInt(element.getElementsByTagName("max").item(0).getTextContent()));
			}
			
			Node models = doc.getElementsByTagName("models").item(0);
			NodeList modelList = models.getChildNodes();
			for (int j = 0; j < modelList.getLength(); j++) {
				Node modelNode = modelList.item(j);
				if (modelNode.getNodeType() == Node.ELEMENT_NODE) {
					Element modelElement = (Element) modelNode;
					String model = modelElement.getElementsByTagName("name").item(0).getTextContent();
					Node registersNode = modelElement.getElementsByTagName("registers").item(0);
					NodeList registerList = ((Element) registersNode).getElementsByTagName("register");
					Map<Integer,Integer> modelRegister = new HashMap<Integer,Integer>();
					for (int i = 0; i < registerList.getLength(); i++) {
						Node registerNode = registerList.item(i);
						if (registerNode.getNodeType() == Node.ELEMENT_NODE) {
							Element registerElement = (Element) registerNode;
							modelRegister.put(Integer.parseInt(registerElement.getElementsByTagName("address").item(0).getTextContent(),16), 
									Integer.parseInt(registerElement.getElementsByTagName("value").item(0).getTextContent(),16));
						}
					}
					config.getModels().put(model, modelRegister);
				}
			}
			
//			NodeList assetsList = doc.getElementsByTagName("assets");
//			for (int i = 0; i < assetsList.getLength(); i++) {
//	
//				Node assets = assetsList.item(i);
//				NodeList assetList = assets.getChildNodes();
//				for (int j = 0; j < assetList.getLength(); j++) {
//					Node asset = assetList.item(j);
//					if (asset.getNodeType() == Node.ELEMENT_NODE) {
//						Element assetElement = (Element) asset;
//						config.putAsset(Integer.parseInt(assetElement.getElementsByTagName("id").item(0).getTextContent()),
//								assetElement.getElementsByTagName("model").item(0).getTextContent());
//					}
//				}
//			}
			
			Node assets = doc.getElementsByTagName("assets").item(0);
			NodeList assetList = assets.getChildNodes();
			for (int j = 0; j < assetList.getLength(); j++) {
				Node asset = assetList.item(j);
				if (asset.getNodeType() == Node.ELEMENT_NODE) {
					Element assetElement = (Element) asset;
					config.putAsset(Integer.parseInt(assetElement.getElementsByTagName("id").item(0).getTextContent()),
							assetElement.getElementsByTagName("model").item(0).getTextContent());
				}
			}
		
			String[] blacklisted = doc.getElementsByTagName("blacklist").item(0).getTextContent().split(",");
			for (String n : blacklisted)
				config.addBlacklisted(Integer.parseInt(n.trim()));
			
		} catch (Exception e) {
			s_logger.error("Parse scan configuration file failed.", e);
		}
		
		return config;
	}

}
