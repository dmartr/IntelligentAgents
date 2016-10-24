package template;

/**
 * Using the cost as heuristic
 * @author Ignacio Aguado, Darío Martínez
 */
public class BenefitsHeuristic implements Heuristic{
	
	// g(s) = g(parent) + cost(s)
	// cost(s) is the opposite of the Benefit function defined in the State: the smaller the number, the better (less costs, more benefits)
	public double getG(DeliberativeState state) {
		double g = state.benefits;
		if (state.parent != null) {
			g += getG(state.parent);
		}
		return -g;
	}
	
	// h(s): accumulated benefits from the initial state
	public double getH(DeliberativeState state) {
		return -state.totalBenefits;
	}
	
	// f(s) = g(s) + h(s)
	public double getF(DeliberativeState state) {	
		return getG(state) + getH(state);
	}

}
