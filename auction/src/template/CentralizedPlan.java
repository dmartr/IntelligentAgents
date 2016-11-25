package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import logist.topology.Topology.City;

/**
 * Stochastic Plan for the Auction Behaviour
 * @author Ignacio Aguado, Darío Martínez
 */
public class CentralizedPlan {
	public HashMap<Integer, LinkedList<AuctionTask>> planTasks;
	public int cost;
	public List<AuctionVehicle> vehicles;
	
	/**
	 * Initializer for an empty new Plan
	 * 
	 * @param: vehicles: List of vehicles
	 */
	public CentralizedPlan(List<AuctionVehicle> vehicles) {
		this.planTasks = new HashMap<Integer, LinkedList<AuctionTask>>();
		this.vehicles = vehicles;
	}
	
	/**
	 * Initializer for Plan from a list of tasks and vehicles
	 * 
	 * @param: vehicles: List of vehicles
	 * @ param planTasks: Sorted list of tasks
	 */
	public CentralizedPlan(HashMap<Integer, LinkedList<AuctionTask>> planTasks, List<AuctionVehicle> vehicles) {
		this.planTasks = planTasks;
		this.vehicles = vehicles;
	}
	
	/**
	 * Initializer for a plan from another plan
	 * 
	 * @param: plan: Old CentralizedPlan
	 */
	public CentralizedPlan(CentralizedPlan plan) {
		this.planTasks = cloneHashmap(plan.planTasks);
		this.vehicles = plan.vehicles;
	}
	
	/**
	 * Calculate the cost of a plan
	 * 
	 * @return: final cost
	 */
	public double planCost() {
		double tempCost = 0;
		for (Integer i : planTasks.keySet()) {
			AuctionVehicle vehicle = vehicles.get(i);
			City currentCity = vehicle.getVehicle().getCurrentCity();
			for (AuctionTask task : planTasks.get(i)) {
				if (task.pickup) {
					double distance = currentCity.distanceTo(task.pickupCity);
					double cost = distance*vehicle.getCostPerKm();
					currentCity = task.pickupCity;
					tempCost += cost;
				} else {
					double distance = currentCity.distanceTo(task.deliveryCity);
					double cost = distance*vehicle.getCostPerKm();
					currentCity = task.deliveryCity;
					tempCost += cost;
				}
			}	
		}
		return tempCost;
	}
	
	/**
	 * Calculate the distance driven of a plan
	 * 
	 * @return: final distance
	 */
	public int planDistance() {
		int distance = 0;
		for (Integer i : planTasks.keySet()) {
			AuctionVehicle vehicle = vehicles.get(i);
			City currentCity = vehicle.getVehicle().getCurrentCity();
			for (AuctionTask task : planTasks.get(i)) {
				if (task.pickup) {
					distance += currentCity.distanceTo(task.pickupCity);
					currentCity = task.pickupCity;
				} else {
					distance += currentCity.distanceTo(task.deliveryCity);
					currentCity = task.deliveryCity;
				}
			}	
		}
		return distance;
	}
	
	/**
	 * Check if the Plan follows the constraints of the problem
	 * 
	 * @return: boolean 
	 */
	public boolean validConstraints() {
		for (Integer i : planTasks.keySet()) {
			int weights = 0;
			List<String> toPickup = new ArrayList<String>();

			for (AuctionTask task : planTasks.get(i)) {
				if (task.pickup) {
					toPickup.add(Integer.toString(task.task.id));
					weights += task.weight;
					if (weights > vehicles.get(i).getVehicle().capacity()) {
						//System.out.println("Weights");
						return false;
					}
				} else {

					if (!toPickup.contains(Integer.toString(task.task.id))) {
						//System.out.println("Not pickup");
						return false;
					} else {
						toPickup.remove(Integer.toString(task.task.id));
						weights -= task.weight;
					}
				}
			}
			if (toPickup.size() > 0) {
				//System.out.println("Not empty");
				return false;
			}
		}
		return true;

	}
	
	/**
	 * Clone a HashMap from another plan (NOT a shallow copy)
	 * 
	 * @return: new hashmap
	 */
    public HashMap<Integer, LinkedList<AuctionTask>> cloneHashmap(HashMap<Integer, LinkedList<AuctionTask>> plan) {
    	HashMap<Integer, LinkedList<AuctionTask>> copy = new HashMap<Integer, LinkedList<AuctionTask>>();
    	for(Entry<Integer, LinkedList<AuctionTask>> entry : plan.entrySet()){
    	    copy.put(entry.getKey(), new LinkedList<AuctionTask>(entry.getValue()));
    	}
    	return copy;
    }
    
	/**
	 * Print ordered list of tasks assigned to each vehicle
	 * 
	 */
    public void paint() {
    	for (AuctionVehicle v : vehicles) {
    		System.out.println("Vehicle " + v.getVehicle().id());
    		for (AuctionTask t: planTasks.get(v.getVehicle().id())) {
    			System.out.println(t.task.id);
    		}
    	}
    }
    
	/**
	 * Number of tasks per vehicle
	 * 
	 * @return: String 
	 */
    public String toString() {
    	String distribution = "";
    	for (AuctionVehicle v : vehicles) {
    		distribution += planTasks.get(v.getVehicle().id()).size() + " ";
    	}
    	return distribution;
    }
    
	/**
	 * Number of vehicles working in the plan
	 * 
	 * @return: number of vehicles
	 */
	public int getVehicles() {
		int nVehicles = 0;
    	for (AuctionVehicle v : vehicles) {
    		if (planTasks.get(v.getVehicle().id()).size() > 0) {
    			nVehicles++;
    		}
    	}
    	return nVehicles;
	} 
	
}