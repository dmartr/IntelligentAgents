package template;

import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

import logist.plan.Action;
import logist.task.Task;

/**
 * State Class for the Deliberative Agent
 * @author Ignacio Aguado, Darío Martínez
 */
public class DeliberativeState {
	
	public City currentCity;
	public int capacity;
	public int costPerKm;
	public double benefits;
	public double totalBenefits;
	public List<Task> toPickupList;
	public List<Task> toDeliverList;
	public List<DeliberativeAction> actionHistory = new ArrayList<DeliberativeAction>();
	public DeliberativeState parent;

	/**
	 * Initializer for a state
	 * 
	 * @param origin City where the agent is located without a task
	 */
	public DeliberativeState(City currentCity, int capacity, int costPerKm, double benefits, double totalBenefits, List<Task> toPickupList, List<Task> toDeliverList, List<DeliberativeAction> actionHistory, DeliberativeState parent) {
		this.currentCity = currentCity;
		this.capacity = capacity;
		this.costPerKm = costPerKm;
		this.benefits = benefits;
		this.toPickupList = toPickupList;
		this.toDeliverList = toDeliverList;
		this.actionHistory = actionHistory;
		this.totalBenefits = totalBenefits;
		this.parent = parent;
		
	}
	
	public DeliberativeState move(City nextCity) {
		double newBenefits = -currentCity.distanceTo(nextCity)*costPerKm;
		double newTotalBenefits = this.totalBenefits + newBenefits;
		DeliberativeAction action = new DeliberativeAction(nextCity);
		List<DeliberativeAction> newActionHistory = new ArrayList<DeliberativeAction>(this.actionHistory);
		newActionHistory.add(action);
		DeliberativeState newState = new DeliberativeState(nextCity, capacity, costPerKm, newBenefits, newTotalBenefits, toPickupList, toDeliverList, newActionHistory, this);
		return newState;
	}
	
	public DeliberativeState pickup(Task pickedup) {
		
		int newCapacity = capacity - pickedup.weight;
		double newBenefits = 0;
		List<Task> newToPickupList = new ArrayList<Task>(toPickupList);
		newToPickupList.remove(pickedup);
		List<Task> newToDeliverList = new ArrayList<Task>(toDeliverList);
		newToDeliverList.add(pickedup);
		DeliberativeAction action = new DeliberativeAction("PICKUP", pickedup);
		List<DeliberativeAction> newActionHistory = new ArrayList<DeliberativeAction>(this.actionHistory);
		newActionHistory.add(action);
		DeliberativeState newState = new DeliberativeState(currentCity, newCapacity, costPerKm, newBenefits, totalBenefits, newToPickupList, newToDeliverList, newActionHistory, this);
		return newState;
	}
	
	public DeliberativeState deliver(Task delivered) {
		int newCapacity = capacity + delivered.weight;
		double newBenefits = delivered.reward;
		double newTotalBenefits = this.totalBenefits + newBenefits;
		List<Task> newToDeliverList = new ArrayList<Task>(toDeliverList);
		newToDeliverList.remove(delivered);
		DeliberativeAction action = new DeliberativeAction("DELIVERY", delivered);
		List<DeliberativeAction> newActionHistory = this.actionHistory;
		newActionHistory.add(action);
		DeliberativeState newState = new DeliberativeState(currentCity, newCapacity, costPerKm, newBenefits, newTotalBenefits, toPickupList, newToDeliverList, newActionHistory, this);
		return newState;
	}
	
	public boolean canPickup(Task toPickup) {
		return capacity-toPickup.weight >= 0;
	}
	
	public List<DeliberativeState> getNextStates() {
		List<DeliberativeState> nextStates = new ArrayList<DeliberativeState>();
		List<Task> toDeliverListCopy = new ArrayList<Task>(this.toDeliverList);

		for (Task deliverTask : toDeliverListCopy) {
			if (deliverTask.deliveryCity.equals(currentCity)) {
				DeliberativeState newState = deliver(deliverTask);
				nextStates.add(newState);
				return nextStates;
			}
		}
		List<Task> toPickupListCopy = new ArrayList<Task>(toPickupList);
		for (Task pickupTask : toPickupListCopy) {
			if (pickupTask.pickupCity.equals(currentCity) && canPickup(pickupTask)) {
				DeliberativeState newState = pickup(pickupTask);
				nextStates.add(newState);
				return nextStates;
			}
		}
		
		for (City neighborCity : currentCity.neighbors()){
			if (this.parent == null || !neighborCity.equals(this.parent.currentCity) || isGettingCloser(neighborCity)) {
				DeliberativeState newState = move(neighborCity);
				nextStates.add(newState);
			}
		}

		return nextStates;
	}
	
	public boolean isGoal() {
		boolean isGoal = toPickupList.isEmpty() && toDeliverList.isEmpty();
		return isGoal;
	}
	
	public int getLevelOfDepth() {
		return actionHistory.size();
	}
	
    public boolean knownState() {
        DeliberativeState s = parent;
        while (s != null) {
            if (s.isEqualTo(this)) {
                return true;
            }
            s = s.parent;
        }
        return false;
    }
    
    public boolean isGettingCloser(City nextCity) {
    	
    	for (Task task : this.toPickupList) {
    		if (currentCity.distanceTo(task.pickupCity) >= nextCity.distanceTo(task.pickupCity)) {
    			return true;
    		}
    	}
    	
    	for (Task task : this.toDeliverList) {
    		if (currentCity.distanceTo(task.deliveryCity) >= nextCity.distanceTo(task.deliveryCity)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public boolean isEqualTo(DeliberativeState stateB) {
    	if (this.currentCity.equals(stateB.currentCity) && this.capacity == stateB.capacity) {
    		for (Task t : stateB.toPickupList) {
    			if (this.toPickupList.indexOf(t) < 0) {
    				return false;
    			}
    		}
    		for (Task t : stateB.toDeliverList) {
    			if (this.toDeliverList.indexOf(t) < 0) {
    				return false;
    			}
    		}
    		return true;
    	} else {
    		return false;
    	}
    	
    }
    
    public int getMaxRewards() {
    	int maxRewards = 0;
    	for (Task pickupTask : this.toPickupList) {
    		maxRewards += pickupTask.reward;
    	}
    	for (Task deliveryTask : this.toDeliverList) {
    		maxRewards += deliveryTask.reward;
    	}
    	return maxRewards;
    }
}
