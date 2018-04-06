package com.walkertribe.ian.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.iface.Listener;
import com.walkertribe.ian.protocol.core.world.DestroyObjectPacket;
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket;

import android.util.SparseArray;

/**
 * A repository of Artemis world objects. Register an instance of SystemManager
 * with the ArtemisNetworkInterface and it will keep track of all objects as
 * they're created, updated and destroyed.
 * @author dhleong
 */
public class SystemManager {
    public interface OnObjectCountChangeListener {
        void onObjectCountChanged(int count);
    }
    
    private static final OnObjectCountChangeListener sDummyListener = 
            new OnObjectCountChangeListener() {
        @Override
        public void onObjectCountChanged(int count) {/* nop */}
    };

    private static final boolean DEBUG = false;

    private int mShipIndex = -1;
    private final SparseArray<ArtemisObject> mObjects = new SparseArray<ArtemisObject>();
    private OnObjectCountChangeListener mListener = sDummyListener;
    
    private final ArtemisPlayer[] mPlayers = new ArtemisPlayer[Artemis.SHIP_COUNT];
    
    public SystemManager() {
        clear();
    }
    
    /** Manually add an obj to the system */
    public void addObject(ArtemisObject obj) {
        synchronized(this) {
            mObjects.put(Integer.valueOf(obj.getId()), obj);
        }

        mListener.onObjectCountChanged(mObjects.size());
    }

    @Listener
    public void onPacket(DestroyObjectPacket pkt) {
        synchronized(this) {
            mObjects.remove(Integer.valueOf(pkt.getTarget()));
        }

        // signal change
        if (mObjects.size() == 1) {
            ArtemisObject last = mObjects.valueAt(0);

            if ("Artemis".equals(last.getName())) {
                // special (hack?) case;
                //  this is actually the end of the game
                clear();
                mListener.onObjectCountChanged(0);
                return;
            }
        } 

        mListener.onObjectCountChanged(mObjects.size());
        return;
    }

    @Listener
    public void onPacket(ObjectUpdatePacket pkt) {
        for (ArtemisObject p : pkt.getObjects()) {
            updateOrCreate(p);
        }
    }

    @SuppressWarnings("unused")
    private boolean updateOrCreate(ArtemisObject o) {
    	Integer id = Integer.valueOf(o.getId());
        ArtemisObject p = mObjects.get(id);

        if (p != null) {
            p.updateFrom(o);
            
            if (o instanceof ArtemisPlayer) {
                // just in case we get the ship number AFTER
                //  first creating the object, we store the
                //  updated ORIGINAL with the new ship number
                ArtemisPlayer plr = (ArtemisPlayer) o;

                if (plr.getShipIndex() != -1) {
                    mPlayers[plr.getShipIndex()] = (ArtemisPlayer) p;
                }
            }
            
            return false;
        } else if (o instanceof ArtemisNpc) {
        	ArtemisNpc npc = (ArtemisNpc) o;
        	if (npc.getSide() != getSide())
        		return false;
        }

        synchronized(this) {
            mObjects.put(id, o);
        }

        if (o instanceof ArtemisPlayer) {
            ArtemisPlayer plr = (ArtemisPlayer) o;

            if (plr.getShipIndex() >= 0) {
                mPlayers[plr.getShipIndex()] = plr;
            }
        }

        if (o.getX() == Float.MIN_VALUE) o.setX(0);
        if (o.getY() == Float.MIN_VALUE) o.setY(0);
        if (o.getZ() == Float.MIN_VALUE) o.setZ(0);

        if (DEBUG && o.getName() == null) {
            throw new IllegalStateException("Creating " + p +" without name! " + 
                    Integer.toHexString(o.getId()));
        }

        mListener.onObjectCountChanged(mObjects.size());
        return true;
    }

    public synchronized void getAll(List<ArtemisObject> dest) {
        for (int i = 0; i < mObjects.size(); i++)
        	dest.add(mObjects.valueAt(i));
    }

    /**
     * Add objects of the given type to the given list.
     */
    public synchronized int getObjects(List<ArtemisObject> dest, ObjectType type) {
        int count = 0;

        for (int i = 0; i < mObjects.size(); i++) {
        	ArtemisObject obj = mObjects.valueAt(i);
            if (obj.getType() == type) {
                dest.add(obj);
                count++;
            }
        }

        return count;
    }

    /**
     * If you don't want/need to reuse a List, this
     * will create a list for you.
     * @see #getObjects(List, int)
     */
    public List<ArtemisObject> getObjects(ObjectType type) {
        List<ArtemisObject> objs = new ArrayList<ArtemisObject>();
        getObjects(objs, type);
        return objs;
    }

    public ArtemisObject getObject(int objId) {
        return mObjects.get(Integer.valueOf(objId));
    }
    
    /**
     * Get the player ship by number. Ship values range from 1 to 8.
     * @param shipIndex
     * @return
     */
    public ArtemisPlayer getPlayerShip(int shipIndex) {
        if (shipIndex < 0 || shipIndex >= Artemis.SHIP_COUNT) {
            throw new IllegalArgumentException("Invalid ship index: " + shipIndex);
        }
        
        return mPlayers[shipIndex];
    }
    
    /**
     * Get the current player ship.
     */
    public ArtemisPlayer getPlayerShip() {
    	try { return getPlayerShip(mShipIndex); }
    	catch (IllegalArgumentException ex) { return null; }
    }
    
    /**
     * Updates the current player ship index.
     */
    public void setShipIndex(int shipIndex) {
    	mShipIndex = shipIndex;
    }
    
    /**
     * Get the side the current player ship is on.
     */
    public byte getSide() {
    	try { return getPlayerShip().getSide(); }
    	catch (NullPointerException ex) { return (byte) -1; }
    }
    
    /**
     * Get the first object with the given name
     * @param type
     * @return null if no such object or if name is null
     */
    public synchronized ArtemisObject getObjectByName(final String name) {
        if (name == null) return null;
        
        for (int i = 0; i < mObjects.size(); i++) {
        	ArtemisObject obj = mObjects.valueAt(i);
            if (name.equals(obj.getName())) return obj;
        }
        return null;
    }

    public void setOnObjectCountChangedListener(OnObjectCountChangeListener listener) {
        mListener = (listener == null) ? sDummyListener : listener;
    }

    public synchronized void clear() {
        mObjects.clear();
        Arrays.fill(mPlayers, null);
    }
}