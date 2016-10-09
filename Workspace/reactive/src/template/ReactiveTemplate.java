package template;

import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;
import uchicago.src.collection.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	double [][] probabilities;
	List<City> states;
	List<City> initStates;
	List<Action> actions;
	int [][] rewards;
	Map<Map<City, Action>, Integer> rewardTable;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		
		List<Vehicle> vehicles = agent.vehicles();
		states = topology.cities();
		
		for(int i = 0; i < vehicles.size(); i++){
			initStates.add(vehicles.get(i).homeCity());
		}
	
		for (City to : topology){
			for (City from : topology){
			probabilities [to.id][from.id] = td.probability(from, to);
			rewards [to.id][from.id] = td.reward(from, to);
			}
		}
		
		
	}
	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		//City pickupCity = availableTask.pickupCity;
		//City deliveryCity = availableTask.deliveryCity;
		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
}
