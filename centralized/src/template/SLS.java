package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


/**
 * Stochastic Local Search Class
 * @author Ignacio Aguado, Darío Martínez
 */
public class SLS {
	
	public List<Vehicle> vehicles = new ArrayList<Vehicle>();
	public List<Task> tasks = new ArrayList<Task>();

	/**
	 * Initializer for a Pickup or Deliver action
	 * 
	 * @param: vehicles: List of vehicles 
	 * @param: tasks: List of tasks
	 */
	public SLS(List<Vehicle> vehicles, List<Task> tasks) {
		this.vehicles = vehicles;
		this.tasks = tasks;
	}
	
	/**
	 * Select the initial solution by giving all the tasks sequentally to the biggest vehicle
	 * 
	 * @returns Initial plan
	 */
	public CentralizedPlan selectInitialSolution() {
		CentralizedPlan initialPlan = new CentralizedPlan(vehicles);
		
		int maxCapacity = 0;
		Vehicle biggestVehicle = vehicles.get(0);
		for (Vehicle v : vehicles) {
			initialPlan.planTasks.put(v.id(), new LinkedList<CentralizedTask>());
			if (v.capacity() > maxCapacity) {
				maxCapacity = v.capacity();
				biggestVehicle = v;
			}
		}
		
		for (Task task : tasks) {
			CentralizedTask pickupTask = new CentralizedTask("PICKUP", task);
			CentralizedTask deliveryTask = new CentralizedTask("DELIVERY", task);
			
			LinkedList<CentralizedTask> taskList = initialPlan.planTasks.get(biggestVehicle.id());
			taskList.addLast(pickupTask);
			taskList.addLast(deliveryTask);
			initialPlan.planTasks.put(biggestVehicle.id(), taskList);
		}
		return initialPlan;
		
	}
	
	/**
	 * Select the initial solution by assigning all the tasks by Round Robin
	 * 
	 * @returns Initial plan
	 */
	public CentralizedPlan selectInitialSolutionRR() {
		CentralizedPlan initialPlan = new CentralizedPlan(vehicles);
		for (Vehicle v : vehicles) {
			initialPlan.planTasks.put(v.id(), new LinkedList<CentralizedTask>());
		}
		int vehicle_id = 0;
		for (Task task : tasks) {
			if (vehicle_id == vehicles.size()) vehicle_id = 0;
			CentralizedTask pickupTask = new CentralizedTask("PICKUP", task);
			CentralizedTask deliveryTask = new CentralizedTask("DELIVERY", task);
			
			LinkedList<CentralizedTask> taskList = initialPlan.planTasks.get(vehicle_id);
			taskList.addLast(pickupTask);
			taskList.addLast(deliveryTask);
			initialPlan.planTasks.put(vehicle_id, taskList);
			vehicle_id++;
		}
		return initialPlan;
		
	}
	
	/**
	 * Select the initial solution by assigning each task to the vehicle which is closer to it in its initial position
	 * 
	 * @returns Initial plan
	 */
	public CentralizedPlan selectInitialSolutionDistance() {
		CentralizedPlan initialPlan = new CentralizedPlan(vehicles);
		for (Vehicle v : vehicles) {
			initialPlan.planTasks.put(v.id(), new LinkedList<CentralizedTask>());
		}

		for (Task task : tasks) {

			CentralizedTask pickupTask = new CentralizedTask("PICKUP", task);
			CentralizedTask deliveryTask = new CentralizedTask("DELIVERY", task);
			int vehicle_id = -1;
			double min_distance = Double.POSITIVE_INFINITY;
			for (Vehicle v : vehicles) {
				double distance = v.getCurrentCity().distanceTo(task.pickupCity);
				if (distance < min_distance) {
					min_distance = distance;
					vehicle_id = v.id();
				}
			}
			
			LinkedList<CentralizedTask> taskList = initialPlan.planTasks.get(vehicle_id);
			taskList.addLast(pickupTask);
			taskList.addLast(deliveryTask);
			initialPlan.planTasks.put(vehicle_id, taskList);

		}
		return initialPlan;
		
	}
	
	/**
	 * Change the first task (Pickup and delivery) from vehicle1 to another vehicle
	 * 
	 * @param Initial plan
	 * @param Vehicle 1 id
	 * 
	 * @returns List of neighbors found
	 */
	public ArrayList<CentralizedPlan> changeVehicle(CentralizedPlan plan, int selectedVehicle) {

		Random r = new Random();
		ArrayList<CentralizedPlan> neighbors = new ArrayList<CentralizedPlan>();
		if (plan.planTasks.get(selectedVehicle).size() > 0) {
			CentralizedTask firstTaskV1 = plan.planTasks.get(selectedVehicle).pollFirst();
			CentralizedTask secondTaskV1 = null;
			LinkedList<CentralizedTask> tasksCopy = new LinkedList<CentralizedTask>(plan.planTasks.get(selectedVehicle));
	
			for (CentralizedTask t : tasksCopy ) {
				if (t.task.id == firstTaskV1.task.id) {
					secondTaskV1 = plan.planTasks.get(selectedVehicle).remove(plan.planTasks.get(selectedVehicle).indexOf(t));
				}
			}
			
			for (Integer vehicle : plan.planTasks.keySet()){
	
				if (vehicle != selectedVehicle) {
					CentralizedPlan neighbor = new CentralizedPlan(plan);
					LinkedList<CentralizedTask> tasksNeighborV1 = neighbor.planTasks.get(vehicle);
					tasksNeighborV1.addFirst(secondTaskV1);
					tasksNeighborV1.addFirst(firstTaskV1);
					neighbor.planTasks.put(vehicle, tasksNeighborV1);
					if (neighbor.validConstraints()) {
						neighbors.add(neighbor);
					}
				}
			}
		}

		return neighbors;

	}

	/**
	 * Find all the possible task permutations for vehicle1
	 * 
	 * @param Initial plan
	 * @param Vehicle 1 id
	 * 
	 * @returns List of neighbors found
	 */
	public ArrayList<CentralizedPlan> changeOrder(CentralizedPlan plan, int selectedVehicle) {
		ArrayList<CentralizedPlan> neighbors = new ArrayList<CentralizedPlan>();
		LinkedList<CentralizedTask> taskList = plan.planTasks.get(selectedVehicle);
		for (int i=0; i<taskList.size(); i++) {
			for (int j= i+1; j<taskList.size();j++) {
				CentralizedPlan neighbor = new CentralizedPlan(plan);
				LinkedList<CentralizedTask> neighborTaskList = neighbor.planTasks.get(selectedVehicle);
				CentralizedTask firstTask = taskList.get(i);
				CentralizedTask secondTask = taskList.get(j);
				neighborTaskList.add(i, secondTask);
				neighborTaskList.remove(i+1);
				neighborTaskList.add(j, firstTask);
				neighborTaskList.remove(j+1);
				neighbor.planTasks.put(selectedVehicle, neighborTaskList);
				if (neighbor.validConstraints()) {
					neighbors.add(neighbor);
				}
			}
		}

		return neighbors;
		

	}
	
	/**
	 * Find all the possible neighbors by changing vehicle and order
	 * 
	 * @param Initial plan
	 * 
	 * @returns List of neighbors found
	 */
	public ArrayList<CentralizedPlan> chooseNeighbors(CentralizedPlan plan) {
		Random r = new Random();
		int randomVehicle = r.nextInt(vehicles.size());
		ArrayList<CentralizedPlan> neighbors = new ArrayList<CentralizedPlan>();
		if (plan.planTasks.get(randomVehicle).size() > 1) {
			neighbors.addAll(changeVehicle(new CentralizedPlan(plan), randomVehicle));
			neighbors.addAll(changeOrder(new CentralizedPlan(plan), randomVehicle));
			return neighbors;
		} else {
			return null;
		}	
	}

	/**
	 * From a list of neighbors, find the best one in terms of cost
	 * Return the new neighbor with a certain probability or just return the old one
	 * 
	 * @param Initial plan
	 * @param List of neighbors
	 * 
	 * @returns Selected plan
	 */
	public CentralizedPlan localChoice(CentralizedPlan oldPlan, ArrayList<CentralizedPlan> neighbors) {
		double bestCost = oldPlan.planCost();
		CentralizedPlan chosenPlan = oldPlan;
		for (CentralizedPlan neighbor : neighbors) {
			double newCost = neighbor.planCost();
			if (newCost < bestCost) {
				chosenPlan = neighbor;
				bestCost = newCost;
			} else if (newCost == bestCost) {
				// If the cost is the same as the best one previously found, take it with a probability p
				Random r = new Random();
				int choice = r.nextInt(100);
				if (choice <= 30) {
					chosenPlan = neighbor;
				}
			}
		}

		// Return the new plan found with a probability of 0.3-0.5 
		Random r = new Random();
		int choice = r.nextInt(100);
		
		if (choice <= 35) {
			return chosenPlan;
		} else {
			return oldPlan;
		}
		
	}
	
}
