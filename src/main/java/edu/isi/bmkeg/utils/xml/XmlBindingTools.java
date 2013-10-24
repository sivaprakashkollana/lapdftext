package edu.isi.bmkeg.utils.xml;

//Copyright 2008 by Basil Vandegriend.  All rights reserved.

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;

import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Tools for working with the JAXB (XML Binding) library.
 */
public class XmlBindingTools {

	/**
	 * Parse the XML supplied by the reader into the corresponding tree of Java
	 * objects.
	 * 
	 * @param reader
	 *            Cannot be null. The source of the XML.
	 * @param rootElementClass
	 *            Cannot be null. The type of the root element.
	 * @return the Java object that is the root of the tree, of type
	 *         rootElement.
	 * @throws JAXBException
	 *             if an error occurs parsing the XML.
	 * @throws SAXException 
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Object> E parseXML(Reader reader,
			Class<E> rootElementClass) throws JAXBException, SAXException {

		if (rootElementClass == null)
			throw new IllegalArgumentException("rootElementClass is null");
		if (reader == null)
			throw new IllegalArgumentException("reader is null");

		JAXBContext context = JAXBContext.newInstance(rootElementClass);
		Unmarshaller unmarshaller = context.createUnmarshaller();
	   
		CollectingValidationEventHandler handler = new CollectingValidationEventHandler();
		unmarshaller.setEventHandler(handler);
			    
		E object = (E) unmarshaller.unmarshal(reader);

		if (!handler.getMessages().isEmpty()) {
			String errorMessage = "XML parse errors:";
			for (String message : handler.getMessages()) {
				errorMessage += "\n" + message;
			}
			throw new JAXBException(errorMessage);
		}

		return object;
	}

    public static void saveAsXml(Object rootElement, File xmlFile) throws Exception {

    	StringWriter writer = new StringWriter();
		XmlBindingTools.generateXML(rootElement, writer);		
		String str = writer.toString();

		BufferedWriter out = new BufferedWriter(new FileWriter(xmlFile));
		out.write(str);
		out.close();

    }
		
	/**
	 * Generate XML using the supplied root element as the root of the object
	 * tree and write the resulting XML to the specified writer
	 * 
	 * @param rootElement
	 *            Cannot be null.
	 * @param writer
	 *            Cannot be null.
	 * @throws JAXBException
	 */
	public static void generateXML(Object rootElement, Writer writer)
			throws JAXBException {

		if (rootElement == null)
			throw new IllegalArgumentException("rootElement is null");
		if (writer == null)
			throw new IllegalArgumentException("writer is null");

		JAXBContext context = JAXBContext.newInstance(rootElement.getClass());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(rootElement, writer);
	}

	private static class CollectingValidationEventHandler implements
			ValidationEventHandler {
		private List<String> messages = new ArrayList<String>();

		public List<String> getMessages() {
			return messages;
		}

		public boolean handleEvent(ValidationEvent event) {
			if (event == null)
				throw new IllegalArgumentException("event is null");

			// calculate the severity prefix and return value
			String severity = null;
			boolean continueParsing = false;
			switch (event.getSeverity()) {
			case ValidationEvent.WARNING:
				severity = "Warning";
				continueParsing = true; // continue after warnings
				break;
			case ValidationEvent.ERROR:
				severity = "Error";
				continueParsing = true; // terminate after errors
				break;
			case ValidationEvent.FATAL_ERROR:
				severity = "Fatal error";
				continueParsing = false; // terminate after fatal errors
				break;
			default:
				assert false : "Unknown severity.";
			}

			String location = getLocationDescription(event);
			String message = severity + " parsing " + location + " due to "
					+ event.getMessage();
			messages.add(message);

			return continueParsing;
		}

		private String getLocationDescription(ValidationEvent event) {
			ValidationEventLocator locator = event.getLocator();
			if (locator == null) {
				return "XML with location unavailable";
			}

			StringBuffer msg = new StringBuffer();
			URL url = locator.getURL();
			Object obj = locator.getObject();
			Node node = locator.getNode();
			int line = locator.getLineNumber();

			if (url != null || line != -1) {
				msg.append("line " + line);
				if (url != null)
					msg.append(" of " + url);
			} else if (obj != null) {
				msg.append(" obj: " + obj.toString());
			} else if (node != null) {
				msg.append(" node: " + node.toString());
			}

			return msg.toString();
		}

	}

	/**
	 * This code copied from Joshua Caplan (http://jazzjuice.blogspot.com) to
	 * solve the problem of anomalous elements generated by JAXB during parsing
	 * of mixed content
	 * 
	 * http://jazzjuice.blogspot.com/2009/06/jaxb-xmlmixed-and-white-space-
	 * anomalies.html
	 * 
	 * Not sure how to implement this. Just clean this up for now.
	 * 
	 * @author burns
	 * 
	 */
	public class WhitespaceAwareUnmarshallerHandler implements ContentHandler {

		private final UnmarshallerHandler uh;

		public WhitespaceAwareUnmarshallerHandler(UnmarshallerHandler uh) {
			this.uh = uh;
		}

		/**
		 * Replace all-whitespace character blocks with the character '\u000B',
		 * which satisfies the following properties:
		 * 
		 * 1. "\u000B".matches( "\\s" ) == true 2. when parsing XmlMixed
		 * content, JAXB does not suppress the whitespace
		 **/
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			for (int i = start + length - 1; i >= start; --i)
				if (!Character.isWhitespace(ch[i])) {
					uh.characters(ch, start, length);
					return;
				}
			Arrays.fill(ch, start, start + length, '\u000B');
			uh.characters(ch, start, length);
		}

		/* what follows is just blind delegation monkey code */
		public void ignorableWhitespace(char[] ch, int start, int length)
				throws SAXException {
			uh.characters(ch, start, length);
		}

		public void endDocument() throws SAXException {
			uh.endDocument();
		}

		public void endElement(String uri, String localName, String name)
				throws SAXException {
			uh.endElement(uri, localName, name);
		}

		public void endPrefixMapping(String prefix) throws SAXException {
			uh.endPrefixMapping(prefix);
		}

		public void processingInstruction(String target, String data)
				throws SAXException {
			uh.processingInstruction(target, data);
		}

		public void setDocumentLocator(Locator locator) {
			uh.setDocumentLocator(locator);
		}

		public void skippedEntity(String name) throws SAXException {
			uh.skippedEntity(name);
		}

		public void startDocument() throws SAXException {
			uh.startDocument();
		}

		public void startElement(String uri, String localName, String name,
				Attributes atts) throws SAXException {
			uh.startElement(uri, localName, name, atts);
		}

		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
			uh.startPrefixMapping(prefix, uri);
		}
	}
}