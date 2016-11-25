package template;

/**
 * Using the distance as heuristic
 * @author Ignacio Aguado, Darío Martínez
 */
public class DistanceHeuristic implements Heuristic {
	
	// g(s) = g(s_parent) + distance(s)
	// The same as calculating the total distance driven by the vehicle
	public double getG(DeliberativeState state) {
		return state.totalDistance;

	}
	
	// h(s): minimum distance necessary to drive to achieve all the pick ups and deliveries
	public double getH(DeliberativeState state) {
		return state.getMinimumFutureDistance();
	}
	
	// f(s) = g(s) + h(s)
	public double getF(DeliberativeState state) {
		return getG(state) + getH(state);
	}

}
