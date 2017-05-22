package artemis.messenger;

import java.util.ArrayList;
import java.util.HashSet;

import com.walkertribe.ian.world.ArtemisObject;
import com.walkertribe.ian.world.SystemManager;

import android.util.SparseArray;

/**
 * The routing graph contains one node for each point that must be visited when completing side missions, plus one node
 * for the player ship, and calculates the minimum distance route to complete all of the side missions.
 * @author Jordan Longstaff
 *
 */
public class RoutingGraph {
	// Variables used by the graph
	private final SparseArray<SparseArray<Double>> nodes;
	private final SparseArray<HashSet<Integer>> paths;
	private final SystemManager manager;
	private final int shipId;

	// Set up a new routing graph, with player ship as the only node
	public RoutingGraph(SystemManager m, int playerShip) {
		nodes = new SparseArray<SparseArray<Double>>();
		paths = new SparseArray<HashSet<Integer>>();
		manager = m;
		shipId = playerShip;
		nodes.put(shipId, new SparseArray<Double>());
	}
	
	// Create routing graph derived from a previous graph, used for calculations
	private RoutingGraph(RoutingGraph previous, int currentNode) {
		// Reuse previous graph's node data
		nodes = previous.nodes;
		
		// Copy previous graph's paths
		paths = new SparseArray<HashSet<Integer>>();
		for (int i = 0; i < previous.paths.size(); i++) {
			paths.put(previous.paths.keyAt(i), new HashSet<Integer>(previous.paths.valueAt(i)));
		}
		
		// Reuse previous graph's Artemis system manager
		manager = previous.manager;
		
		// Put player ship where we specify
		shipId = currentNode;
		
		// Remove from list of places we need to go
		paths.remove(shipId);
		
		// Add to list of places we need to go, where we need to go from current location
		for (int dest: previous.paths.get(shipId)) {
			if (paths.indexOfKey(dest) < 0) {
				paths.put(dest, new HashSet<Integer>());
			}
		}
	}
	
	/**
	 * Add a point to the graph.
	 * @param object the object whose location is added as a point to the graph
	 */
	public void addNode(ArtemisObject object) {
		// If point already exists in the graph, do nothing
		if (nodes.indexOfKey(object.getId()) >= 0) return;
		
		// Otherwise, add it
		nodes.put(object.getId(), new SparseArray<Double>());
		
		// Then calculate distance between this point and all other points already in the graph
		for (int i = 0; i < nodes.size(); i++) {
			// Skip distance between new node and itself
			int id = nodes.keyAt(i);
			if (id == object.getId()) continue;

			// Euclidean distance
			float distX = object.getX() - manager.getObject(id).getX();
			float distY = object.getY() - manager.getObject(id).getY();
			float distZ = object.getZ() - manager.getObject(id).getZ();
			
			double distance = Math.sqrt(distX * distX + distY * distY + distZ * distZ);
			nodes.get(object.getId()).put(id, distance);
			nodes.get(id).put(object.getId(), distance);
		}
	}
	
	/**
	 * Adds a requisite path to the graph. Paths added using this method indicate that we must go to {@code src}
	 * at least once before we go to {@code dest}. If {@code dest} is {@code null}, then this means simply that we must
	 * go to {@code src} at some point.
	 * @param src the point to go from
	 * @param dest the point to go to
	 */
	public void addPath(ArtemisObject src, ArtemisObject dest) {
		// Make sure nodes exist in graph
		addNode(src);
		
		// Add src to list of points to go to
		if (paths.indexOfKey(src.getId()) < 0)
			paths.put(src.getId(), new HashSet<Integer>());
		
		// Add dest to list of points to go to after going to src, if not null
		if (dest != null) {
			addNode(dest);
			paths.get(src.getId()).add(dest.getId());
		}
	}
	
	/**
	 * Calculates the route of minimum distance to all points in the graph so that all currently running side missions
	 * will be completed. The {@code minCost} parameter is used for pruning purposes; when called by the app, it is
	 * given a starting value of positive infinity; as shorter routes are found, {@code minCost} will decrease. This
	 * way, the algorithm rejects routes that are known to be longer than the current minimum distance route.
	 * @param minCost the distance traveled along the current minimum route
	 * @return the list of points, in order, that must be visited according to the obtained route
	 */
	public ArrayList<Integer> calculateRoute(double minCost) {
		// If current route is too long, snap it off
		if (minCost < 0) return null;
		
		// If route is finished, accept it
		if (paths.size() == 0) return new ArrayList<Integer>();
		
		// Start by assuming that we will not find a route that works
		ArrayList<Integer> bestRoute = null;
		
		// Pick a node to move to
		for (int i = 0; i < paths.size(); i++) {
			// Set up route simulation in which player ship has traveled to this point
			RoutingGraph next = new RoutingGraph(this, paths.keyAt(i));
			ArrayList<Integer> route = new ArrayList<Integer>();
			route.add(paths.keyAt(i));
			
			// Get distance traveled from ship to chosen point
			double firstDist = nodes.get(shipId).get(paths.keyAt(i));
			
			// Take this route as far as it goes - this is depth-first search
			ArrayList<Integer> subRoute = next.calculateRoute(minCost - firstDist);
			
			// If we cannot find a short enough route, then go back and pick a different node
			if (subRoute == null) continue;
			
			// Analyze selected route
			route.addAll(subRoute);
			
			// Calculate total distance traveled
			int src = shipId;
			int dest;
			float dist = 0.0f;
			for (int j = 0; j < route.size(); j++) {
				dest = route.get(j);
				dist += nodes.get(src).get(dest);
				src = dest;
			}
			
			// Update current route selection and minimum cost
			if (bestRoute == null) bestRoute = new ArrayList<Integer>();
			else bestRoute.clear();
			bestRoute.addAll(route);
			minCost = dist;
		}
		
		// Return what we ended up with
		return bestRoute;
	}
}