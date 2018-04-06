package com.walkertribe.ian;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.walkertribe.ian.vesseldata.SAXVesselDataHandler;
import com.walkertribe.ian.vesseldata.VesselData;

/**
 * A class for containing the information needed to load Artemis resources, and
 * cache them once they are loaded.
 * @author rjwut
 */
public class DefaultContext implements ArtemisContext {
	private PathResolver pathResolver;
	private VesselData vesselData;

	/**
	 * Creates a new Context using the given PathResolver.
	 */
	public DefaultContext(PathResolver pathResolver) {
		this.pathResolver = pathResolver;
	}

	/**
	 * Returns the PathResolver for this Context.
	 */
	public PathResolver getPathResolver() {
		return pathResolver;
	}

	/**
	 * Returns a VesselData object describing all the information in
	 * vesselData.xml. The first time this method is invoked for this object,
	 * vesselData.xml will be loaded and parsed, and the result will be cached
	 * in this object for later re-use.
	 */
	@Override
	public VesselData getVesselData() {
		if (vesselData == null) {
			vesselData = loadVesselData();
		}

		return vesselData;
	}
	
	/**
	 * Loads the vesselData.xml file using the given PathResolver and returns the resulting
	 * VesselData object.
	 */
	private VesselData loadVesselData() {
		SAXVesselDataHandler handler = new SAXVesselDataHandler(this);
		parseXml("dat/vesselData.xml", handler);
		return handler.getVesselData();
	}

	/**
	 * Parses the XML file located at the indicated path using the given
	 * DefaultHandler.
	 */
	private void parseXml(String path, DefaultHandler handler) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();
			xmlReader.setContentHandler(handler);
			xmlReader.parse(new InputSource(pathResolver.get(path)));
		} catch (SAXException ex) {
			throw new RuntimeException(ex);
		} catch (ParserConfigurationException ex) {
			throw new RuntimeException(ex);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}