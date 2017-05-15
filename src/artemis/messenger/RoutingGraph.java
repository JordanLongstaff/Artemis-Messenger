package artemis.messenger;

import java.util.ArrayList;
import java.util.HashSet;

import com.walkertribe.ian.world.ArtemisObject;
import com.walkertribe.ian.world.SystemManager;

import android.util.SparseArray;

public class RoutingGraph {
	private final SparseArray<SparseArray<Float>> nodes;
	private final SparseArray<HashSet<Integer>> paths;
	private final SystemManager manager;
	private final int shipId;
	
	public RoutingGraph(SystemManager m, int playerShip) {
		nodes = new SparseArray<SparseArray<Float>>();
		paths = new SparseArray<HashSet<Integer>>();
		manager = m;
		shipId = manager.getPlayerShip(playerShip).getId();
		nodes.put(shipId, new SparseArray<Float>());
	}
	
	private RoutingGraph(RoutingGraph previous, int currentNode) {
		nodes = previous.nodes;
		paths = new SparseArray<HashSet<Integer>>();
		for (int i = 0; i < previous.paths.size(); i++) {
			paths.put(previous.paths.keyAt(i), new HashSet<Integer>(previous.paths.valueAt(i)));
		}
		manager = previous.manager;
		shipId = currentNode;
		
		paths.remove(shipId);
		for (int dest: previous.paths.get(shipId)) {
			if (paths.indexOfKey(dest) < 0) {
				paths.put(dest, new HashSet<Integer>());
			}
		}
	}
	
	public void addNode(ArtemisObject object) {
		if (nodes.indexOfKey(object.getId()) >= 0) return;
		nodes.put(object.getId(), new SparseArray<Float>());
		for (int i = 0; i < nodes.size(); i++) {
			int id = nodes.keyAt(i);
			if (id == object.getId()) continue;

			float distX = object.getX() - manager.getObject(id).getX();
			float distY = object.getY() - manager.getObject(id).getY();
			float distZ = object.getZ() - manager.getObject(id).getZ();
			
			float distance = (float)Math.sqrt(distX * distX + distY * distY + distZ * distZ);
			nodes.get(object.getId()).put(id, distance);
			nodes.get(id).put(object.getId(), distance);
		}
	}
	
	public void addPath(ArtemisObject src, ArtemisObject dest) {
		addNode(src);
		if (paths.indexOfKey(src.getId()) < 0)
			paths.put(src.getId(), new HashSet<Integer>());
		
		if (dest != null) {
			addNode(dest);
			paths.get(src.getId()).add(dest.getId());
		}
	}
	
	public ArrayList<Integer> calculateRoute() {
		ArrayList<Integer> bestRoute = new ArrayList<Integer>();
		float minCost = Float.POSITIVE_INFINITY;
		
		for (int i = 0; i < paths.size(); i++) {
			RoutingGraph next = new RoutingGraph(this, paths.keyAt(i));
			ArrayList<Integer> route = new ArrayList<Integer>();
			route.add(paths.keyAt(i));
			route.addAll(next.calculateRoute());
			
			int src = shipId;
			int dest;
			float dist = 0.0f;
			for (int j = 0; j < route.size(); j++) {
				dest = route.get(j);
				dist += nodes.get(src).get(dest);
				src = dest;
			}
			
			if (dist < minCost) {
				bestRoute.clear();
				bestRoute.addAll(route);
				minCost = dist;
			}
		}
		
		return bestRoute;
	}
	
	public float getDistanceFromShip(int id) {
		return nodes.get(shipId).get(id);
	}
}