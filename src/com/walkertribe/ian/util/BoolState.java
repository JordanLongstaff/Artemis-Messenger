package com.walkertribe.ian.util;

/**
 * Represents the tri-state values of TRUE, FALSE and UNKNOWN, preserving the original values that
 * produced them in the bit stream.
 * @author rjwut
 */
public class BoolState {
    public static final BoolState TRUE = new BoolState(new byte[] { 1 });
    public static final BoolState FALSE = new BoolState(new byte[1]);
    public static final BoolState UNKNOWN = new BoolState(null);
    
    private final byte[] bytes;
    private final Boolean boolValue;
    
    /**
     * Creates a BoolState object that represents the value encoded in the given
     * byte array. If the given byte array is null, the BoolState will
     * represent UNKNOWN; if it is all zeroes, it will represent FALSE;
     * otherwise, it will represent TRUE.
     */
    BoolState(byte[] value) {
    	bytes = value;
    	
    	if (value == null) boolValue = null;
    	else {
    		for (byte b: bytes) {
    			if (b != 0) {
    				boolValue = Boolean.TRUE;
    				return;
    			}
    		}
    		
    		boolValue = Boolean.FALSE;
    	}
    }

    /**
     * Returns true if this BoolState represents TRUE; false otherwise.
     */
    public boolean getBooleanValue() {
        return boolValue != null && boolValue.booleanValue();
    }

    /**
     * Converts the given boolean value to a BoolState
     */
    public static BoolState from(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * Returns false if state is null or UNKNOWN; true otherwise.
     */
    public static boolean isKnown(BoolState state) {
        return state != null && state.boolValue != null;
    }

    /**
     * Returns true if the given value is TRUE; false otherwise.
     */
    public static boolean safeValue(BoolState value) {
        return value != null && value.getBooleanValue();
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (this == obj) return true;
    	if (obj instanceof BoolState) {
    		BoolState other = (BoolState) obj;
    		if (boolValue == null && other.boolValue == null) return true;
    		if (boolValue == null || other.boolValue == null) return false;
    		return boolValue.equals(other.boolValue);
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return boolValue != null ? boolValue.hashCode() : 0;
    }
    
    @Override
    public String toString() {
    	return boolValue != null ? boolValue.toString() : "UNKNOWN";
    }
    
    /**
     * Returns a byte array encoding the boolean value represented by this
     * BoolState, expressed in the indicated number of bytes. If this BoolState
     * contains a byte array representing the original value as it was read
     * from the bit stream, this value will be used (padding or truncating as
     * required). Otherwise, a byte array representing the value will be
     * synthesized, with the first as 1 or 0, and the rest as 0. If this
     * BoolState represents the value UNKNOWN, this method will return null. If
     * it is not UNKNOWN and byteCount is less than one, an
     * IllegalArgumentException will be thrown.
     */
    public byte[] toByteArray(int byteCount) {
    	if (boolValue == null) return null;

    	if (byteCount < 1) {
    		throw new IllegalArgumentException("A BoolState must be at least 1 byte long");
    	}

    	byte[] bytesToReturn = new byte[byteCount];
    	System.arraycopy(bytes, 0, bytesToReturn, 0, Math.min(bytes.length, byteCount));
    	return bytesToReturn;
    }
    
    public BoolState and(BoolState other) {
    	if (equals(BoolState.TRUE)) return other;
    	else if (equals(BoolState.FALSE) || other.equals(BoolState.FALSE)) return BoolState.FALSE;
    	else return BoolState.UNKNOWN;
    }
    
    public BoolState or(BoolState other) {
    	if (equals(BoolState.FALSE)) return other;
    	else if (equals(BoolState.TRUE) || other.equals(BoolState.TRUE)) return BoolState.TRUE;
    	else return BoolState.UNKNOWN;
    }
}