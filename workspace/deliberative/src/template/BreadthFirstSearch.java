package template;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BreadthFirstSearch {
	
	private Queue<DeliberativeState> toVisit = new LinkedList<DeliberativeState>();
	private DeliberativeState goal;
	
	public DeliberativeState search(DeliberativeState initialState, int maxDepth) {
		toVisit.add(initialState);
		double bestBenefits = Double.NEGATIVE_INFINITY;
		while (!toVisit.isEmpty()){
			System.out.println(toVisit.element());

			DeliberativeState currentState = toVisit.poll();
			if (currentState.isGoal() && currentState.totalBenefits > bestBenefits) {
				goal = currentState;
				bestBenefits = currentState.totalBenefits;
			} else {
				if (currentState.getLevelOfDepth() <= maxDepth) {
					List<DeliberativeState> nextStates = currentState.getNextStates();
					for (DeliberativeState state : nextStates) {
						toVisit.add(state);
					}
				}
			}
		}
		return goal;
	}
}
