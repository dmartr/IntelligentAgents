package template;

import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	public double [][] probabilities;
	List<City> states;
	public int [][] rewards;
	
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
		
		states = topology.cities();
		
		probabilities = new double[states.size()][states.size()];
		rewards = new int[states.size()][states.size()];
		
		for (City to : topology){
			for (City from : topology){
				probabilities[to.id][from.id] = td.probability(from, to);
				rewards[to.id][from.id] = td.reward(from, to);
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
	
	public double getR(City state, Action action){
		Plan p = new Plan(state);
		p.append(action);
		int cost = (int)(5 * p.totalDistance());
		if (action instanceof Action.Pickup){
			//coger task de la accion
//			City pstate = task.deliveryCity; 
//			int r = rewards[state.id][pstate.id]-cost;
			return 0;
		} else {
			return -cost;
		}
	}
	
	public double getT(City state, Action action, City pstate){
		double p = 0;
		if(action instanceof Action.Move){
			p = probabilities[state.id][pstate.id];
		}else if(action instanceof Action.Pickup){
			if(equals(pstate)){  //añadir ciudad destino de la accion
				p = 1;
			}
		}
		return p;
	}
}
