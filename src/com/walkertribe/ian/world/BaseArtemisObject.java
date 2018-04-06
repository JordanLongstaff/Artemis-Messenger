package com.walkertribe.ian.world;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.walkertribe.ian.util.BoolState;
import com.walkertribe.ian.util.TextUtil;

/**
 * Base implementation for all ArtemisObjects.
 */
public abstract class BaseArtemisObject implements ArtemisObject {
	/**
	 * Puts the given int property into the indicated map, unless its value is
	 * equal to unspecifiedValue.
	 */
	public static void putProp(SortedMap<String, Object> props, String label, int value, int unspecifiedValue) {
		if (value != unspecifiedValue) props.put(label, Integer.valueOf(value));
	}

	/**
	 * Puts the given float property into the indicated map, unless its value
	 * is equal to unspecifiedValue.
	 */
	public static void putProp(SortedMap<String, Object> props, String label, float value, float unspecifiedValue) {
		if (value != unspecifiedValue) props.put(label, Float.valueOf(value));
	}

	/**
	 * Puts the given BoolState property into the indicated map, unless the
	 * value is null or BoolState.UNKNOWN.
	 */
	public static void putProp(SortedMap<String, Object> props, String label, BoolState value) {
		if (BoolState.isKnown(value)) props.put(label, value);
	}

	/**
	 * Puts the given Object property into the indicated map, unless the given
	 * value is null.
	 */
	public static void putProp(SortedMap<String, Object> props, String label, Object value) {
		if (value != null) props.put(label, value);
	}

	protected final int mId;
    public CharSequence mName;
    private float mX = Float.NaN;
    private float mY = Float.NaN;
    private float mZ = Float.NaN;
    private SortedMap<String, byte[]> unknownProps;

    public BaseArtemisObject(int objId) {
        mId = objId;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public CharSequence getName() {
        return mName;
    }

    public void setName(CharSequence name) {
    	mName = name;
    }

    @Override
    public float getX() {
        return mX;
    }

    @Override
    public void setX(float mX) {
        this.mX = mX;
    }

    @Override
    public float getY() {
        return mY;
    }

    @Override
    public void setY(float y) {
        mY = y;
    }

    @Override
    public float getZ() {
        return mZ;
    }

    @Override
    public void setZ(float z) {
        mZ = z;
    }
    
    @Override
    public boolean hasPosition() {
    	return !Float.isNaN(mX) && !Float.isNaN(mZ);
    }
    
    @Override
    public float distance(ArtemisObject obj) {
    	if (!hasPosition() || !obj.hasPosition())
    		throw new RuntimeException("Cannot compute distance for an object without a position");
    	
    	float y0 = obj.getY();
    	if (Float.isNaN(y0)) y0 = 0;
    	float y1 = Float.isNaN(mY) ? 0 : mY;
    	
    	float dX = obj.getX() - mX;
    	float dY = y0 - y1;
    	float dZ = obj.getZ() - mZ;
    	return (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

	@Override
    public void updateFrom(ArtemisObject obj) {
		CharSequence name = obj.getName();
        if (name != null) mName = name;

        float x = obj.getX();
        float y = obj.getY();
        float z = obj.getZ();

        if (!Float.isNaN(x)) mX = x;
        if (!Float.isNaN(y)) mY = y;
        if (!Float.isNaN(z)) mZ = z;

        BaseArtemisObject cast = (BaseArtemisObject) obj;
        SortedMap<String, byte[]> unknown = cast.getUnknownProps();

        if (unknown != null && !unknown.isEmpty()) {
        	if (unknownProps == null) {
        		unknownProps = new TreeMap<String, byte[]>();
        	}

        	unknownProps.putAll(unknown);
        }
    }

    @Override
    public final SortedMap<String, byte[]> getUnknownProps() {
    	return unknownProps;
    }

    @Override
    public final void setUnknownProps(SortedMap<String, byte[]> unknownProps) {
    	this.unknownProps = unknownProps;
    }

    @Override
    public final SortedMap<String, Object> getProps() {
    	SortedMap<String, Object> props = new TreeMap<String, Object>();
    	appendObjectProps(props);
    	return props;
    }

    @Override
    public final String toString() {
    	SortedMap<String, Object> props = getProps();
    	StringBuilder b = new StringBuilder();

    	for (Map.Entry<String, Object> entry : props.entrySet()) {
    		b.append("\n\t").append(entry.getKey()).append(": ");
    		Object value = entry.getValue();

    		if (value instanceof byte[]) { 
    			b.append(TextUtil.byteArrayToHexString((byte[]) value));
    		} else {
    			b.append(value);
    		}
    	}

    	return b.toString();
    }

    /**
     * Appends this object's properties to the given map. If includeUnspecified
     * is true, unspecified properties area also included (unless they are also
     * unknown properties). Subclasses must always call the superclass's
     * implementation of this method.
     */
	protected void appendObjectProps(SortedMap<String, Object> props) {
    	props.put("ID", Integer.valueOf(mId));
    	putProp(props, "Name", mName);
    	putProp(props, "Object type", getType());
    	putProp(props, "X", mX, Float.NaN);
    	putProp(props, "Y", mY, Float.NaN);
    	putProp(props, "Z", mZ, Float.NaN);

    	if (unknownProps != null) {
        	props.putAll(unknownProps);
    	}
    }
	
	/**
	 * Returns true if this object has any data.
	 */
	protected boolean hasData() {
		return mName != null || !Float.isNaN(mX) || !Float.isNaN(mY) || !Float.isNaN(mZ);
	}

    @Override
    public boolean equals(Object other) {
    	if (this == other) return true;
    	if (getClass().isInstance(other)) return mId == ((ArtemisObject) other).getId();
    	return false;
    }

    @Override
    public int hashCode() {
        return mId;
    }
}