package template;

public class DistanceHeuristic implements Heuristic {
	
	public double getG(DeliberativeState state) {
		double g = state.benefits;
		if (state.parent != null) {
			g += getG(state.parent);
		}
		return -g;
	}
	
	public double getH(DeliberativeState state) {
		return state.getDistance();
	}
	
	public double getF(DeliberativeState state) {
		return getG(state) + getH(state);
	}

}
