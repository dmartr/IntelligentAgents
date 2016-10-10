package template;

import logist.topology.Topology.City;


public class ReactiveAction {
	
	public City destination;
	public boolean pickup;
	public String id;
	
	public ReactiveAction() {
		this.pickup = true;
		this.destination = null;
		this.id = "pickup";
	}
	
	public ReactiveAction(City destination) {
		this.pickup = false;
		this.destination = destination;
		this.id = Integer.toString(destination.id);
	}
	
	public boolean isPickup() {
		return this.pickup;
	}
}