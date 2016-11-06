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
public class SLS {
	
	public List<Vehicle> vehicles = new ArrayList<Vehicle>();
	public List<Task> tasks = new ArrayList<Task>();

	public SLS(List<Vehicle> vehicles, List<Task> tasks) {
		this.vehicles = vehicles;
		this.tasks = tasks;
	}
	
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
	
	public ArrayList<CentralizedPlan> changeVehicle(CentralizedPlan plan, int initialVehicle) {

		ArrayList<CentralizedPlan> neighbors = new ArrayList<CentralizedPlan>();
		CentralizedTask movedPickupTask = plan.planTasks.get(initialVehicle).pollFirst();
		CentralizedTask movedDeliveryTask = null;
		LinkedList<CentralizedTask> tasksCopy = new LinkedList<CentralizedTask>(plan.planTasks.get(initialVehicle));

		for (CentralizedTask t : tasksCopy ) {
			if (t.task.id == movedPickupTask.task.id) {
				movedDeliveryTask = plan.planTasks.get(initialVehicle).remove(plan.planTasks.get(initialVehicle).indexOf(t));
			}
		}
		
		for (Integer vehicle : plan.planTasks.keySet()){

			if (vehicle != initialVehicle) {
				CentralizedPlan neighbor = new CentralizedPlan(plan);
				LinkedList<CentralizedTask> tasksNeighbor = neighbor.planTasks.get(vehicle);
				tasksNeighbor.addFirst(movedDeliveryTask);
				tasksNeighbor.addFirst(movedPickupTask);
				neighbor.planTasks.put(vehicle, tasksNeighbor);
				if (neighbor.validConstraints()) {
					//System.out.println(neighbor.planTasks.get(0).size() + " " + neighbor.planTasks.get(1).size() + " " + neighbor.planTasks.get(2).size() + " " + neighbor.planTasks.get(3).size());
					neighbors.add(neighbor);
				}
			}
		}

		return neighbors;

	}
	
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

	public CentralizedPlan localChoice(CentralizedPlan oldPlan, ArrayList<CentralizedPlan> neighbors) {
		int bestCost = oldPlan.planCost();
		CentralizedPlan chosenPlan = oldPlan;
		for (CentralizedPlan neighbor : neighbors) {

			if (neighbor.planCost() < bestCost) {
				chosenPlan = neighbor;
				bestCost = neighbor.planCost();
				//System.out.println("WE HAVE NEW PLAN with cost " +  chosenPlan.planCost());
				
			}
		}

		Random r = new Random();
		int choice = r.nextInt(100);
		
		if (choice <= 30) {
			//System.out.println(chosenPlan.planTasks.get(0).size() + " " +  chosenPlan.planTasks.get(1).size() + " " + chosenPlan.planTasks.get(2).size() + " "+ chosenPlan.planTasks.get(3).size() );

			return chosenPlan;
		} else {
			return oldPlan;
		}
		
	}
	
}
