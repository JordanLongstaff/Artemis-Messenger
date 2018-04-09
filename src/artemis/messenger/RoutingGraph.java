package artemis.messenger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

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
	private static final int NO_DESTINATION = -1;
	
	// Variables used by the graph
	private final SparseArray<HashSet<Integer>> paths;
	private final SystemManager manager;
	private final int shipId;
	private float minCost;
	private final ArrayList<Integer> pointsList;
	private final Random random;

	// Set up a new routing graph, with player ship as the only node
	public RoutingGraph(SystemManager m, int playerShip) {
		paths = new SparseArray<HashSet<Integer>>();
		manager = m;
		shipId = playerShip;
		minCost = Float.POSITIVE_INFINITY;
		pointsList = new ArrayList<Integer>();
		random = new Random();
	}
	
	// Create routing graph derived from a previous graph, used for calculations
	private RoutingGraph(RoutingGraph previous, int currentNode) {
		// Copy previous graph's paths
		paths = new SparseArray<HashSet<Integer>>();
		for (int i = 0; i < previous.paths.size(); i++) {
			paths.put(previous.paths.keyAt(i), new HashSet<Integer>(previous.paths.valueAt(i)));
		}
		
		// Reuse previous graph's Artemis system manager
		manager = previous.manager;
		
		// Reuse randomizer
		random = previous.random;
		
		// Update minimum cost
		minCost = previous.minCost - getDistance(previous.shipId, currentNode);
		
		// Update minimum number of points
		pointsList = new ArrayList<Integer>(previous.pointsList);
		if (!pointsList.isEmpty()) pointsList.remove(0);
		
		// Put player ship where we specify
		shipId = currentNode;
		
		// Remove from list of places we need to go
		paths.remove(shipId);
		
		// Add to list of places we need to go, where we need to go from current location
		for (int dest: previous.paths.get(shipId)) {
			if (dest >= 0 && paths.indexOfKey(dest) < 0) {
				paths.put(dest, new HashSet<Integer>());
			}
		}
	}
	
	/**
	 * Return the Euclidean distance between two objects.
	 * @param src the source object
	 * @param dest the destination object
	 * @return the distance between the source and destination objects
	 */
	public float getDistance(int src, int dest) {
		return manager.getObject(src).distance(manager.getObject(dest));
	}
	
	/**
	 * Returns the current minimum cost calculated by the graph.
	 * @return the current minimum cost threshold
	 */
	public float getMinimumCost() {
		return minCost;
	}
	
	/**
	 * Sets the minimum cost threshold for the graph.
	 * @param cost the minimum cost threshold
	 */
	public void setMinimumCost(float cost) {
		minCost = cost;
	}
	
	/**
	 * Returns the current minimum number of points to visit as calculated by the graph's algorithm.
	 * @return the current minimum number of points to visit
	 */
	public int getMinimumPoints() {
		if (pointsList.isEmpty()) return Integer.MAX_VALUE;
		else return pointsList.size();
	}
	
	/**
	 * Sets the minimum point contact threshold for the graph.
	 * @param points the minimum contact threshold
	 */
	public void resetMinimumPoints() {
		pointsList.clear();
	}
	
	/**
	 * Removes a node, and all associated paths and distance data, from the graph.
	 * @param node the node to remove
	 */
	public void removeNode(ArtemisObject node) {
		// Remove all paths from the node
		paths.remove(node.getId());
		
		// Removes all paths to the node
		for (int i = 0; i < paths.size(); i++) {
			paths.valueAt(i).remove(node.getId());
		}
	}
	
	/**
	 * Adds a requisite path to the graph. Paths added using this method indicate that we must go to {@code src}
	 * at least once before we go to {@code dest}. If {@code dest} is {@code null}, then this means simply that we must
	 * go to {@code src} at some point.
	 * @param src the point to go from
	 * @param dest the point to go to
	 * @throws IllegalArgumentException if either {@code src} or {@code dest} is null
	 */
	public void addPath(ArtemisObject src, ArtemisObject dest) throws IllegalArgumentException {
		// Assert that neither source nor destination is null
		if (src  == null) throw new IllegalArgumentException("Path source cannot be null");
		if (dest == null) throw new IllegalArgumentException("Path destination cannot be null");
		
		// Add src to list of points to go to
		if (paths.indexOfKey(src.getId()) < 0)
			paths.put(src.getId(), new HashSet<Integer>());
		
		// Add dest to list of points to go to after going to src, if not null
		paths.get(src.getId()).add(dest.getId());
	}
	
	/**
	 * Adds a requisite path to the graph. Paths added using this method indicate that we must go to {@code dest} at
	 * some point.
	 * @param dest the point to go to
	 * @throws IllegalArgumentException if {@code dest} is null
	 */
	public void addPath(ArtemisObject dest) {
		// Assert that destination is not null
		if (dest == null) throw new IllegalArgumentException("Path destination cannot be null");
		
		// Add dest to list of points to go to
		if (paths.indexOfKey(dest.getId()) < 0)
			paths.put(dest.getId(), new HashSet<Integer>());
		paths.get(dest.getId()).add(NO_DESTINATION);
	}
	
	/**
	 * Returns the size of the graph, which is equal to the number of points that currently must be visited.
	 * @return the size of the graph
	 */
	public int size() {
		return paths.size();
	}
	
	/**
	 * Resets the routing graph.
	 */
	public void resetGraph() {
		paths.clear();
	}
	
	/**
	 * Purges all points from the current itinerary that we need to go to after somewhere else, but that we don't need
	 * to go anywhere after those points. This is intended for efficiency and accuracy.
	 */
	public void purgePaths() {
		// Find all of the destination points
		HashSet<Integer> destinations = new HashSet<Integer>();
		for (int i = 0; i < paths.size(); i++) {
			destinations.addAll(paths.valueAt(i));
		}
		
		// Remove the "null point"
		destinations.remove(-1);
		
		// Remove all places that we need to go after somewhere else, but nowhere after them
		for (int i: destinations) {
			if (paths.get(i) != null && paths.get(i).size() == 1 && paths.get(i).contains(-1)) {
				paths.remove(i);
			}
		}
	}
	
	public void recalculateCurrentRoute() {
		int currentPoint = shipId;
		float cost = 0f;
		if (pointsList.isEmpty()) cost = Float.POSITIVE_INFINITY;
		else for (int i: pointsList) {
			cost += getDistance(currentPoint, i);
			currentPoint = i;
		}
		minCost = cost;
	}
	
	/**
	 * Calculates the route of minimum distance to all points in the graph so that all currently running side missions
	 * will be completed. The {@code minCost} parameter is used for pruning purposes; when called by the app, it is
	 * given a starting value of positive infinity; as shorter routes are found, {@code minCost} will decrease. This
	 * way, the algorithm rejects routes that are known to be longer than the current minimum distance route.
	 * @param minCost the distance traveled along the current minimum route
	 * @return the list of points, in order, that must be visited according to the obtained route
	 */
	public ArrayList<Integer> guessRoute() {
		// If current route is too long, snap it off
		if (minCost <= 0) return null;
		
		// If we have to visit too many points, snap it off
		if (minCost < Float.POSITIVE_INFINITY && paths.size() > pointsList.size()) return null;
		
		// If route is finished, accept it
		if (paths.size() == 0) {
			minCost = 0;
			return new ArrayList<Integer>();
		}
		
		// Pick a random node to move to
		int nextNode = random.nextInt(paths.size());
		
		// Set up route simulation in which player ship has traveled to this point
		RoutingGraph next = new RoutingGraph(this, paths.keyAt(nextNode));
		ArrayList<Integer> route = new ArrayList<Integer>();
		route.add(paths.keyAt(nextNode));
		
		// Take this route as far as it goes - this is depth-first search
		ArrayList<Integer> subRoute = next.guessRoute();
		
		// If we cannot find a short enough route, then go back and pick a different node
		if (subRoute == null) return null;
		
		// Analyze selected route
		route.addAll(subRoute);
		
		// Calculate total distance traveled
		float dist = getDistance(shipId, paths.keyAt(nextNode)) + next.minCost;
		
		// Update minimum cost and number of points
		minCost = dist;
		pointsList.clear();
		pointsList.addAll(route);
		
		// Return what we ended up with
		return route;
	}
}