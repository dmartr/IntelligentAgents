package template;

import java.util.LinkedList;

import logist.simulation.Vehicle;
import logist.topology.Topology.City;
import logist.task.*;

public class AuctionVehicle {

	public Vehicle vehicle;
	public double costPerKm;
	public int capacity;
	public City homeCity;
	public LinkedList<AuctionTask> vehicleTasks;
	
	public AuctionVehicle(Vehicle vehicle){
		this.vehicle = vehicle;
		costPerKm = vehicle.costPerKm();
		capacity = vehicle.capacity();
		homeCity = vehicle.homeCity();
		vehicleTasks = new LinkedList<AuctionTask>();
	}
	
	public AuctionVehicle(Vehicle vehicle, double costPerKm, int capacity, City homeCity){
		vehicle = this.vehicle;
		costPerKm = this.costPerKm;
		capacity = this.capacity;
		homeCity = this.homeCity;
		vehicleTasks = new  LinkedList<AuctionTask>();
	}
	
	public Vehicle getVehicle(){
		return vehicle;
	}
	
	public double getCostPerKm(){
		return costPerKm;
	}
	
	public void setCostPerKm(double costPerKm){
		this.costPerKm = costPerKm;
	}
	
	public double getCapacity(){
		return capacity;
	}
	
	public void setCapacity(int capacity){
		this.capacity = capacity;
	}
	
	public City getHome(){
		return homeCity;
	}
	
	public void setHome(City homeCity){
		this.homeCity = homeCity;
	}
	
	private double getCost(){
		double cost = 0;
		City currentCity = homeCity;
		for (AuctionTask task : vehicleTasks) {
			if (task.pickup) {
				cost += currentCity.distanceTo(task.pickupCity) * costPerKm;
				currentCity = task.pickupCity;
			}else{
				cost += currentCity.distanceTo(task.deliveryCity) * costPerKm;
				currentCity = task.deliveryCity;
			}
		}
		return cost;
	}
	
	public double getMarginalCost(Task task){
		double initCost = getCost();
		AuctionTask pickupTask = new AuctionTask("PICKUP", task);
		vehicleTasks.add(pickupTask);
		AuctionTask deliveryTask = new AuctionTask("DELIVERY", task);
		vehicleTasks.add(deliveryTask);
		double newCost = getCost();
		vehicleTasks.remove(pickupTask);
		vehicleTasks.remove(deliveryTask);		
		return newCost-initCost;
	}	
}