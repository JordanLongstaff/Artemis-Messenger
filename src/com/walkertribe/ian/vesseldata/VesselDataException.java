package com.walkertribe.ian.vesseldata;

/**
 * Thrown when IAN fails to parse the vesselData.xml file.
 * @author rjwut
 */
public class VesselDataException extends RuntimeException {
	private static final long serialVersionUID = -495427263065919450L;

	public VesselDataException(Exception ex) {
		super(ex);
	}
}