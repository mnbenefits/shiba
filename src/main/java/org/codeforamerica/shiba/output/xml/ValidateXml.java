package org.codeforamerica.shiba.output.xml;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.IOException;

/**
 * Simple tool to validate an XML file, presumably one generated by MNbenefits,
 * against schema OnlineApplication.xsd
 * 
 * When you run this tool use the Run As -> Run Configurations option to provide
 * the input XML file path as the first argument.
 * e.g., the argument could look like this: H:\MNbenefits\docs\xml-doc.xml
 * 
 *  From https://howtodoinjava.com/jaxb/read-xml-to-java-object/:
 *  Java provides many approaches to read an XML file and use the XL content to either print,
 *  use in application or populate data in Java objects to further use in application lifecycle.
 *  The three main APIs used for this purpose are Simple API for XML (SAX),
 *  the Document Object Model (DOM) and Java Architecture for XML Binding (JAXB).
 *   - SAX or DOM parser use the JAXP API to parse an XML document. Both scan the document and
 *     logically break it up into discrete pieces (e.g. nodes, text and comment etc).
 *   - SAX parser starts at the beginning of the document and passes each piece of the document
 *     to the application in the sequence it finds it. Nothing is saved in memory so it can�t do
 *     any in-memory manipulation.
 *   - DOM parser creates a tree of objects that represents the content and organization of data
 *     in the Document object in memory. Here, application can then navigate through the tree to
 *     access the data it needs, and if appropriate, manipulate it.
 *   - JAXB unmarshals the document into Java content objects. The Java content objects
 *     represent the content and organization of the XML document, and are directly available
 *     to your program.
 */
public class ValidateXml {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Error: Provide the input XML file path as the first argument in the run config.");
			System.exit(0);
		}

		String xmlFilePath = args[0];
		File xmlFile = new File(xmlFilePath);
		if (!xmlFile.exists()) {
			System.out.println("Error: XML file " + xmlFile.getName() + " does not exist.");
			System.exit(0);
		}
		
        // First verify that the XML is "well formed" XML
		System.out.println("Verifying that file " + xmlFile.getName() + " contains well-formed XML...");
		if (!isWellFormedXML(xmlFile)) {
			System.exit(0);
		}
		
		// Validate the XML against the schema OnlineApplication.xsd
		System.out.println("Verifying that file " + xmlFile.getName() + " conforms to schema OnlineApplication.xsd...");
		validateXMLConformsToSchema(xmlFile);
	}
	
	private static boolean isWellFormedXML(File xmlFile) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			dBuilder.parse(xmlFile);
		} catch (Exception e) {
			System.out.println("...XML file " + xmlFile.getName() + " does NOT contain well formed XML");
			System.out.println(e.getMessage());
			return false;
		}
		System.out.println("...XML file " + xmlFile.getName() + " contains well formed XML");
		return true;
	}
	
	private static boolean validateXMLConformsToSchema(File xmlFile) {
		File schemaFile = new File("src\\main\\resources\\OnlineApplication.xsd");

		Source sourceFile = new StreamSource(xmlFile);
		
		SchemaFactory schemaFactory = SchemaFactory
		    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
		  Schema schema = schemaFactory.newSchema(schemaFile);
		  Validator validator = schema.newValidator();
		  validator.validate(sourceFile);
		  System.out.println("..." + sourceFile.getSystemId() + " is valid");
		} catch (SAXException e) {
			  System.out.println("..." + sourceFile.getSystemId() + " is NOT valid, SAXException");
			  System.out.println("reason:");
			  System.out.println(e);
			  return false;
		} catch (IOException e) {
			  System.out.println("..." + sourceFile.getSystemId() + " is NOT valid, I/O Exception");
			  System.out.println("reason:");
			  System.out.println(e);
			  return false;
		} catch (Exception e) {
			  System.out.println("..." + sourceFile.getSystemId() + " is NOT valid, an Exception occurred");
			  System.out.println("reason:");
			  System.out.println(e);
			  return false;
		}
		return true;
	}

}