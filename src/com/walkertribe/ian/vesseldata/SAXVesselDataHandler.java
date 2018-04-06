package com.walkertribe.ian.vesseldata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.walkertribe.ian.ArtemisContext;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX ContentHandler implementation that can convert vesselData.xml to a
 * VesselData object.
 */
public class SAXVesselDataHandler extends DefaultHandler {
	/**
	 * Interface for classes which handle a specific element type in
	 * vesselData.xml.
	 */
	private interface Parser {
		/**
		 * Invoked when the corresponding element is encountered by the SAX
		 * parser.
		 */
		void parse(Attributes attrs);
	}

	private VesselData vesselData;
	private ArtemisContext ctx;
	private Map<String, Parser> parsers = new HashMap<String, Parser>();
	private Faction faction;
	private Vessel vessel;
	
	private static final ArrayList<String> TORPEDO_TYPE_VARIABLES = new ArrayList<String>();

	public SAXVesselDataHandler(ArtemisContext ctx) {
		this.ctx = ctx;
		
		Parser vesselPointParser = new VesselPointParser();
		Parser weaponPortParser = new WeaponPortParser();
		
		parsers.put("art", new ArtParser());
		parsers.put("beam_port", new BeamPortParser());
		parsers.put("carrierload", new CarrierLoadParser());
		parsers.put("drone_port", weaponPortParser);
		parsers.put("engine_port", vesselPointParser);
		parsers.put("fleet_ai", new FleetAiParser());
		parsers.put("hullRace", new HullRaceParser());
		parsers.put("impulse_point", vesselPointParser);
		parsers.put("internal_data", new InternalDataParser());
		parsers.put("long_desc", new LongDescParser());
		parsers.put("maneuver_point", vesselPointParser);
		parsers.put("performance", new PerformanceParser());
		parsers.put("production", new ProductionParser());
		parsers.put("shields", new ShieldsParser());
		parsers.put("taunt", new TauntParser());
		parsers.put("torpedo_station_port", weaponPortParser);
		parsers.put("torpedo_storage", new TorpedoStorageParser());
		parsers.put("torpedo_tube", vesselPointParser);
		parsers.put("vessel", new VesselParser());
		parsers.put("vessel_data", new VesselDataParser());
	}
	
	public VesselData getVesselData() {
		return vesselData;
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes attrs) throws SAXException {
		Parser parser = parsers.get(qName);

		if (parser != null) {
			parser.parse(attrs);
		} else {
			Log.e(getClass().getName(), "Unknown element: " + qName);
		}
	}

	/**
	 * Converts an XML attribute to a float value, or 0.0f if the attribute is
	 * not found.
	 */
	private static float parseFloat(Attributes attrs, String name) {
		String value = attrs.getValue(name);
		return value != null ? Float.parseFloat(value) : 0.0f;
	}

	/**
	 * Converts an XML attribute to an int value; or 0 if the attribute is not
	 * found.
	 */
	private static int parseInt(Attributes attrs, String name) {
		String value = attrs.getValue(name);
		return value != null ? getInt(value) : 0;
	}
	
	/**
	 * Attempts to parse an XML attribute to an int value. If it fails, the attribute
	 * must be a variable, so it gets the value of the variable instead.
	 */
	private static int getInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			int index = TORPEDO_TYPE_VARIABLES.indexOf(value);
			if (index < 0) {
				index = TORPEDO_TYPE_VARIABLES.size();
				TORPEDO_TYPE_VARIABLES.add(value);
			}
			return index;
		}
	}

	/**
	 * Extracts the x, y, and z attributes and writes them to the given
	 * VesselPoint object.
	 */
	private static void parseVesselPoint(Attributes attrs) {
		attrs.getValue("x");
		attrs.getValue("y");
		attrs.getValue("z");
	}

	/**
	 * Extracts weapon port attributes and writes them to the given WeaponPort
	 * object.
	 */
	public static void parseWeaponPort(Attributes attrs) {
		parseVesselPoint(attrs);
		attrs.getValue("damage");
		attrs.getValue("cycletime");
		attrs.getValue("range");
	}

	/**
	 * Parser for <art> elements.
	 */
	private class ArtParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			attrs.getValue("meshfile");
			attrs.getValue("diffuseFile");
			attrs.getValue("glowFile");
			attrs.getValue("specularFile");
			attrs.getValue("scale");
			attrs.getValue("pushRadius");
		}
	}

	/**
	 * Parser for <beam_port> elements.
	 */
	private class BeamPortParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			parseWeaponPort(attrs);
			attrs.getValue("arcwidth");
		}
	}

	/**
	 * Parser for <carrierload> elements.
	 */
	private class CarrierLoadParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			attrs.getValue("fighter");
			attrs.getValue("bomber");
		}
	}

	/**
	 * Parser for <fleet_ai> elements.
	 */
	private class FleetAiParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			attrs.getValue("commonality");
		}
	}

	/**
	 * Parser for <hullRace> elements.
	 */
	private class HullRaceParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			int id = parseInt(attrs, "ID");
			faction = new Faction(id, attrs.getValue("name"), attrs.getValue("keys"));

			while (vesselData.factions.size() <= id) {
				vesselData.factions.add(null);
			}

			vesselData.factions.set(id, faction);
		}
	}

	/**
	 * Parser for <internal_data> elements.
	 */
	private class InternalDataParser implements Parser{
		@Override
		public void parse(Attributes attrs) {
			attrs.getValue("file");
		}
	}

	/**
	 * Parser for <long_desc> elements.
	 */
	private class LongDescParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			attrs.getValue("text");
		}
	}

	/**
	 * Parser for <performance> elements.
	 */
	private class PerformanceParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			attrs.getValue("turnrate");
			attrs.getValue("topspeed");
			attrs.getValue("efficiency");
		}
	}

	/**
	 * Parser for <production> elements.
	 */
	private class ProductionParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			vessel.productionCoeff = parseFloat(attrs, "coeff");
		}
	}

	/**
	 * Parser for <shields> elements.
	 */
	private class ShieldsParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			attrs.getValue("front");
			attrs.getValue("back");
			attrs.getValue("player");
		}
	}

	/**
	 * Parser for <taunt> elements.
	 */
	private class TauntParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			attrs.getValue("immunity");
			attrs.getValue("text");
		}
	}

	/**
	 * Parser for <torpedo_storage> elements.
	 */
	private class TorpedoStorageParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			attrs.getValue("type");
			attrs.getValue("amount");
		}
	}

	/**
	 * Parser for <vessel> elements.
	 */
	private class VesselParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			Integer id = Integer.valueOf(attrs.getValue("uniqueID"));
			vessel = new Vessel(
					ctx,
					id.intValue(),
					parseInt(attrs, "side"),
					attrs.getValue("classname"),
					attrs.getValue("broadType")
			);
			vesselData.vessels.put(id, vessel);
		}
	}

	/**
	 * Parser for the main <vessel_data> element.
	 */
	private class VesselDataParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			vesselData = new VesselData(ctx, attrs.getValue("version"));
		}
	}

	/**
	 * Parser for <engine_port>, <impulse_point>, <maneuver_point> and <torpedo_tube> elements.
	 */
	private class VesselPointParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			parseVesselPoint(attrs);
		}
	}

	/**
	 * Parser for <drone_port> and <torpedo_station_port> elements.
	 */
	private class WeaponPortParser implements Parser {
		@Override
		public void parse(Attributes attrs) {
			parseWeaponPort(attrs);
		}
	}
}