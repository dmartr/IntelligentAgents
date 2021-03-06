package template;

import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Arrays;
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
	public double totalDistance;

	/**
	 * Initializer for a state
	 * 
	 * @param current City where the Agent is located
	 * @param capacity: Free storing space left in the vehicle
	 * @param costPerKm: cost to pay for every km driven
	 * @param totalDistance: accumulate of distance driven by the vehicle
	 * @param benefits: cost of the last movement (Benefits (+) vs Losses (-))
	 * @param totalBenefits: accumulate of benefits 
	 * @param toPickUpList: list of Tasks ready to be picked up
	 * @param toDeliverList: list of Tasks the vehicle is carrying
	 * @param parent: parent state 
	 * 
	 */
	public DeliberativeState(City currentCity, int capacity, int costPerKm, double totalDistance, double benefits, double totalBenefits, List<Task> toPickupList, List<Task> toDeliverList, List<DeliberativeAction> actionHistory, DeliberativeState parent) {
		this.currentCity = currentCity;
		this.capacity = capacity;
		this.costPerKm = costPerKm;
		this.benefits = benefits;
		this.toPickupList = toPickupList;
		this.toDeliverList = toDeliverList;
		this.actionHistory = actionHistory;
		this.totalBenefits = totalBenefits;
		this.parent = parent;
		this.totalDistance = totalDistance;
		
	}
	
	/**
	 * Action Move for a State
	 * Creates a new State with the new values
	 * @param nextCity: neighbor city to be moved to
	 * 
	 */
	public DeliberativeState move(City nextCity) {
		double d = currentCity.distanceTo(nextCity);
		double newBenefits = -d*costPerKm;
		double newTotalBenefits = this.totalBenefits + newBenefits;
		double newTotalDistance = this.totalDistance + d;
		DeliberativeAction action = new DeliberativeAction(nextCity);
		List<DeliberativeAction> newActionHistory = new ArrayList<DeliberativeAction>(this.actionHistory);
		newActionHistory.add(action);
		DeliberativeState newState = new DeliberativeState(nextCity, capacity, costPerKm, newTotalDistance, newBenefits, newTotalBenefits, toPickupList, toDeliverList, newActionHistory, this);
		return newState;
	}
	
	/**
	 * Action Pickup for a State
	 * Creates a new State with the new values
	 * @param pickedUp: Task that has been picked up
	 * 
	 */
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
		DeliberativeState newState = new DeliberativeState(currentCity, newCapacity, costPerKm, totalDistance, newBenefits, totalBenefits, newToPickupList, newToDeliverList, newActionHistory, this);
		return newState;
	}
	
	/**
	 * Action Deliver for a State
	 * Creates a new State with the new values
	 * @param delivered: Task delivered to the city
	 * 
	 */
	public DeliberativeState deliver(Task delivered) {
		int newCapacity = capacity + delivered.weight;
		double newBenefits = delivered.reward;
		double newTotalBenefits = this.totalBenefits + newBenefits;
		List<Task> newToDeliverList = new ArrayList<Task>(toDeliverList);
		newToDeliverList.remove(delivered);
		DeliberativeAction action = new DeliberativeAction("DELIVERY", delivered);
		List<DeliberativeAction> newActionHistory = this.actionHistory;
		newActionHistory.add(action);
		DeliberativeState newState = new DeliberativeState(currentCity, newCapacity, costPerKm, totalDistance, newBenefits, newTotalBenefits, toPickupList, newToDeliverList, newActionHistory, this);
		return newState;
	}
	
	/**
	 * Checks if the vehicle has storing space enough to pick up a new Task
	 * @param toPickup: Task that the vehicle can pick up
	 * @returns True if it can be picked up
	 */
	public boolean canPickup(Task toPickup) {
		return capacity-toPickup.weight >= 0;
	}
	
	/**
	 * Obtain the next States of the node tree for the current state
	 * 
	 * @returns a List of States that can be next from the current State
	 * 
	 */
	public List<DeliberativeState> getNextStates() {
		List<DeliberativeState> nextStates = new ArrayList<DeliberativeState>();
		List<Task> toDeliverListCopy = new ArrayList<Task>(this.toDeliverList);

		// If there is a task to be delivered in the current city then the only possible State is deliver it
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
			}
		}
		
		// If there are tasks to be picked up then the only possible State is pick one of them up
		if (!nextStates.isEmpty()) {
			return nextStates;
		}
		
		// If there aren't tasks to be delivered or picked up, then we can move to a neighbor city 
		// Impossible movements:
		//		1. Parent State
		//		2. State that doesn't get the vehicle closer either to pick up or deliver a task
		for (City neighborCity : currentCity.neighbors()){
			if (this.parent == null || !neighborCity.equals(this.parent.currentCity) || isGettingCloser(neighborCity)) {
				DeliberativeState newState = move(neighborCity);
				nextStates.add(newState);
			}
		}

		return nextStates;
	}
	
	/**
	 * The State is a final State (no more Tasks to be delivered or picked up)
	 * 
	 * @returns True if it's a final State
	 * 
	 */	
	public boolean isGoal() {
		boolean isGoal = toPickupList.isEmpty() && toDeliverList.isEmpty();
		return isGoal;
	}
	
	
	/**
	 * How many actions have been taken from the initial State to arrive to the current State
	 * 
	 * @returns number of actions
	 * 
	 */
	public int getLevelOfDepth() {
		return actionHistory.size();
	}
	
	
	/**
	 * Detect if the State has already been evaluated before to avoid loops and unnecessary repetitions
	 * 
	 * @returns True if the State is known
	 * 
	 */
    public boolean knownState() {
        DeliberativeState stateB = parent;
        while (stateB != null) {
            if (stateB.isEquivalentlTo(this)) {
                return true;
            }
            stateB = stateB.parent;
        }
        return false;
    }
    
	
	/**
	 * Discover if moving to a city is making the vehicle go closer to a goal (pickup or deliver)
	 * 
	 * @returns False if the movement to the next city doesn't help the vehicle
	 * 
	 */
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
    
	
	/**
	 * Compare two States to know if they are equivalent
	 * 
	 * @returns True if the State has already been evaluated before
	 * 
	 */
    public boolean isEquivalentlTo(DeliberativeState stateB) {
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
    
	
	/**
	 * The total of rewards pending in the map (once they've been all picked up and delivered)
	 * 
	 * @returns total amount of money 
	 * 
	 */
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
	
	/**
	 * Get the distance driven to arrive to the current State from the parent State
	 * 
	 * @returns a List of States that can be next from the current State
	 * 
	 */
    public double getDistance() {
    	double distance = 0.0;
    	if (parent != null) {
    		distance = currentCity.distanceTo(parent.currentCity);
    	}
    	return distance;
    }
    
	/**
	 * Get the minimum distance needed to achieve a Goal State from the Current City
	 * 
	 * @returns Distance needed
	 * 
	 */
    public double getMinimumFutureDistance() {
    	double min_future_distance = Double.NEGATIVE_INFINITY;
    	for (Task pickupTask : this.toPickupList) {
    		double task_distance = currentCity.distanceTo(pickupTask.pickupCity) + pickupTask.pickupCity.distanceTo(pickupTask.deliveryCity);
    		if (task_distance > min_future_distance) {
    			min_future_distance = task_distance;
    		}
    	}
    	for (Task deliveryTask : this.toDeliverList) {
    		double task_distance = currentCity.distanceTo(deliveryTask.deliveryCity);
    		if (task_distance > min_future_distance) {
    			min_future_distance = task_distance;
    		}    	
    	}
    	return min_future_distance;
    }

}
