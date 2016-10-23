package template;

public class BenefitsHeuristic implements Heuristic{
	
	public double getG(DeliberativeState state) {
		double g = state.benefits;
		if (state.parent != null) {
			g += getG(state.parent);
		}
		return -g;
	}
	
	public double getH(DeliberativeState state) {
		return -state.totalBenefits;
	}
	
	public double getF(DeliberativeState state) {	
		return getG(state) + getH(state);
	}

}
