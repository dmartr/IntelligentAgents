package template;

import logist.topology.Topology.City;


public class ReactiveState {
	
	public City origin;
	public City destination;
	public boolean pickup;
	public String id;
	
	public ReactiveState(City origin) {
		this.pickup = false;
		this.origin = origin;
		this.destination = null;
		this.id = Integer.toString(origin.id) + "-*";
	}
	
	public ReactiveState(City origin, City destination) {
		this.pickup = true;
		this.origin = origin;
		this.destination = destination;
		this.id = Integer.toString(origin.id) + "-" + Integer.toString(destination.id);
	}
	
	public boolean isPickup() {
		return pickup;
	}
}
