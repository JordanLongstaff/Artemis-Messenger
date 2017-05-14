package com.walkertribe.ian;

import java.util.HashMap;
import java.util.Map;

import com.walkertribe.ian.vesseldata.PathResolver;
import com.walkertribe.ian.vesseldata.VesselData;
import com.walkertribe.ian.vesseldata.VesselInternals;

/**
 * A class for containing the information needed to load Artemis resources, and
 * cache them once they are loaded.
 * @author rjwut
 */
public class Context {
	private PathResolver pathResolver;
	private VesselData vesselData;
	private Map<String, VesselInternals> internalsMap = new HashMap<String, VesselInternals>();

	/**
	 * Creates a new Context using the given PathResolver.
	 */
	public Context(PathResolver pathResolver) {
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
	public VesselData getVesselData() {
		if (vesselData == null) {
			vesselData = VesselData.load(this);
		}

		return vesselData;
	}

	/**
	 * Given the path to an .snt file, returns a VesselInternals object that
	 * describes the node grid stored in that file. The VesselInternals will be
	 * cached in this object, and the cached VesselInternals will be used for
	 * subsequent requests for the same .snt file.
	 */
	public VesselInternals getInternals(String sntPath) {
		VesselInternals internals = internalsMap.get(sntPath);

		if (internals == null) {
			internals = new VesselInternals(pathResolver, sntPath);
			internalsMap.put(sntPath, internals);
		}

		return internals;
	}
}
