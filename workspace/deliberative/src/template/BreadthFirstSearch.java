package template;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BreadthFirstSearch {
	
	private Queue<DeliberativeState> toVisit = new LinkedList<DeliberativeState>();
	private List<DeliberativeState> goals = new ArrayList<DeliberativeState>();
	
	public List<DeliberativeState> search(DeliberativeState initialState, int maxDepth) {
		toVisit.add(initialState);

		while (!toVisit.isEmpty()){
			DeliberativeState currentState = toVisit.poll();
			
			if (currentState.isGoal()) {
				goals.add(currentState);
			} else {
				if (currentState.getLevelOfDepth() <= maxDepth) {
					List<DeliberativeState> nextStates = currentState.getNextStates();
					for (DeliberativeState state : nextStates) {
						toVisit.add(state);
					}
				}
			}
		}
		return goals;
	}
}
