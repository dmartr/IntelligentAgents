package template;

import logist.topology.Topology.City;

/**
 * State Class for the Reactive Agent
 * @author Ignacio Aguado, Darío Martínez
 */
public class ReactiveState {
	
	public City origin;
	public City destination;
	public boolean pickup;
	public String id;
	
	/**
	 * Initializer for a state with no available tasks
	 * 
	 * @param origin City where the agent is located without a task
	 */
	public ReactiveState(City origin) {
		this.pickup = false;
		this.origin = origin;
		this.destination = null;
		this.id = Integer.toString(origin.id) + "-*";
	}
	
	/**
	 * Initializer for a state with an available task
	 * 
	 * @param origin City where the agent has found a task
	 * @param destination City where the task has to be delivered
	 */
	public ReactiveState(City origin, City destination) {
		this.pickup = true;
		this.origin = origin;
		this.destination = destination;
		this.id = Integer.toString(origin.id) + "-" + Integer.toString(destination.id);
	}
	
	/**
	 * @return True if the State has a task to a certain city
	 */
	public boolean isPickup() {
		return pickup;
	}
}
