package com.walkertribe.ian;

import com.walkertribe.ian.vesseldata.VesselData;

/**
 * Interface for classes which can return Artemis resources.
 * @author rjwut
 */
public interface ArtemisContext {
	/**
	 * Returns a VesselData object describing all the information in
	 * vesselData.xml.
	 */
	public VesselData getVesselData();
}