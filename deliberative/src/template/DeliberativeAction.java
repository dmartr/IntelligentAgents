package template;

import logist.task.Task;
import logist.topology.Topology.City;

/**
 * Action Class for the Actions
 * @author Ignacio Aguado, Darío Martínez
 */
public class DeliberativeAction {
	
	public boolean pickup;
	public boolean deliver;
	public boolean move;
	public City nextCity;
	public Task pickedupTask;
	public Task deliveredTask;
	public String id;
	
	/**
	 * Initializer for the Move action
	 * @param destination City to move 
	 */
	public DeliberativeAction(City nextCity) {
		this.pickup = false;
		this.deliver = false;
		this.move = true;
		this.nextCity = nextCity;
		this.pickedupTask = null;
		this.deliveredTask = null;
		this.id = "MOVE-" + nextCity.name;
	}
	
	/**
	 * Initializer for a Pickup or Deliver action
	 * 
	 * @param: type: Pickup or Delivery
	 * @param: task to be picked up or delivered
	 */
	public DeliberativeAction(String type, Task task) {
		if (type.equals("PICKUP")) {
			this.pickup = true;
			this.deliver = false;
			this.move = false;
			this.nextCity = null;
			this.pickedupTask = task;
			this.deliveredTask = null;
			this.id = "PICKUP-" + task.id;
		} else if (type.equals("DELIVERY")) {
			this.pickup = false;
			this.deliver = true;
			this.move = false;
			this.nextCity = null;
			this.pickedupTask = null;
			this.deliveredTask = task;
			this.id = "DELIVERY-" + task.id;
		}

	}
}