package com.walkertribe.ian.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.walkertribe.ian.Context;
import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.iface.Listener;
import com.walkertribe.ian.protocol.core.world.DestroyObjectPacket;
import com.walkertribe.ian.protocol.core.world.IntelPacket;
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket;

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

    private final Context mCtx;
    private final HashMap<Integer, ArtemisObject> mObjects = 
            new HashMap<Integer, ArtemisObject>();
    private OnObjectCountChangeListener mListener = sDummyListener;
    
    private final ArtemisPlayer[] mPlayers = new ArtemisPlayer[Artemis.SHIP_COUNT];
    
    public SystemManager(Context ctx) {
    	mCtx = ctx;
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
            ArtemisObject last = mObjects.values().iterator().next();

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

    @Listener
    public void onPacket(IntelPacket pkt) {
    	ArtemisNpc npc = (ArtemisNpc) mObjects.get(Integer.valueOf(pkt.getId()));

    	if (npc != null) {
    		npc.setIntel(pkt.getIntel());
    	}
    }

    @SuppressWarnings("unused")
    private boolean updateOrCreate(ArtemisObject o) {
    	Integer id = Integer.valueOf(o.getId());
        ArtemisObject p = mObjects.get(id);

        if (p != null) {
            p.updateFrom(o, mCtx);
            
            if (o instanceof ArtemisPlayer) {
                // just in case we get the ship number AFTER
                //  first creating the object, we store the
                //  updated ORIGINAL with the new ship number
                ArtemisPlayer plr = (ArtemisPlayer) o;

                if (plr.getShipNumber() != -1) {
                    mPlayers[plr.getShipNumber() - 1] = (ArtemisPlayer) p;
                }
            }
            
            return false;
        }

        synchronized(this) {
            mObjects.put(id, o);
        }

        if (o instanceof ArtemisPlayer) {
            ArtemisPlayer plr = (ArtemisPlayer) o;

            if (plr.getShipNumber() >= 0) {
                mPlayers[plr.getShipNumber() - 1] = plr;
            }
        }

        if (DEBUG && o.getName() == null) {
            throw new IllegalStateException("Creating " + p +" without name! " + 
                    Integer.toHexString(o.getId()));
        }

        mListener.onObjectCountChanged(mObjects.size());
        return true;
    }

    public synchronized void getAll(List<ArtemisObject> dest) {
        dest.addAll(mObjects.values());
    }

    public synchronized void getAllSelectable(List<ArtemisObject> dest) {
        for (ArtemisObject obj : mObjects.values()) {
            // tentative
            if (!(obj instanceof ArtemisGenericObject)) {
                dest.add(obj);
            }
        }
    }

    /**
     * Add objects of the given type to the given list 
     * 
     * @param dest
     * @param type One of the ArtemisObject#TYPE_* constants
     * @return The number of objects added to "dest"
     */
    public synchronized int getObjects(List<ArtemisObject> dest, ObjectType type) {
        int count = 0;

        for (ArtemisObject obj : mObjects.values()) {
            if (obj.getType() == type) {
                dest.add(obj);
                count++;
            }
        }

        return count;
    }

    /**
     * If you don't want/need to reuse a List, this
     *  will create a list for you
     *  
     * @param type
     * @return
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
     * @param shipNumber
     * @return
     */
    public ArtemisPlayer getPlayerShip(int shipNumber) {
        if (shipNumber < 1 || shipNumber > 8) {
            throw new IllegalArgumentException("Invalid ship number: " + shipNumber);
        }
        
        return mPlayers[shipNumber - 1];
    }
    
    /**
     * Get the first object with the given name
     * @param type
     * @return null if no such object or if name is null
     */
    public synchronized ArtemisObject getObjectByName(final String name) {
        if (name == null) {
            return null;
        }
        
        for (ArtemisObject obj : mObjects.values()) {
            if (name.equals(obj.getName())) {
                return obj;
            }
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
