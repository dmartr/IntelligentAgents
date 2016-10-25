package template;

/**
 * Using the cost as heuristic
 * @author Ignacio Aguado, Darío Martínez
 */
public class BenefitsHeuristic implements Heuristic{
	
	// g(s) = g(parent) + cost(s)
	// Equivalent to the total cost accumulated for this state (cost - rewards)
	// cost(s) is the opposite of the Benefit function defined in the State: the smaller the number, the better (less costs, more benefits)
	public double getG(DeliberativeState state) {
		return -state.totalBenefits;
	}
	
	// h(s): minimum cost - rewards or remaining tasks
	public double getH(DeliberativeState state) {
		return state.getMinimumFutureDistance()*state.costPerKm-state.getMaxRewards();
	}
	
	// f(s) = g(s) + h(s)
	public double getF(DeliberativeState state) {
		return getG(state) + getH(state);
	}

}
