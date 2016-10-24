package template;

/**
 * Using the distance as heuristic
 * @author Ignacio Aguado, Darío Martínez
 */
public class DistanceHeuristic implements Heuristic {
	
	// g(s) = g(parent) + cost(s)
	// cost(s) is the opposite of the Benefit function defined in the State: the smaller the number, the better (less costs, more benefits)
	public double getG(DeliberativeState state) {
		double g = state.benefits;
		if (state.parent != null) {
			g += getG(state.parent);
		}
		return -g;
	}
	
	// h(s): distance driven from the previous state
	public double getH(DeliberativeState state) {
		return state.getDistance();
	}
	
	// f(s) = g(s) + h(s)
	public double getF(DeliberativeState state) {
		return getG(state) + getH(state);
	}

}
