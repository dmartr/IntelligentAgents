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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactiveTemplate implements ReactiveBehavior {

	
	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	
	public List<City> cities;
	public Topology topology;
	
	public double [][] probabilities;
	public int [][] rewards;
	public double [][] costs;

	public  Map<String, ReactiveState> states = new HashMap<String, ReactiveState>();
	public  Map<String, ReactiveAction> actions = new HashMap<String, ReactiveAction>();

	public Map<String, Double> neighbours_prob = new HashMap<String, Double>();
	public Map<String, Double> cities_prob = new HashMap<String, Double>();
	public Map<String, Integer> neighbours_num = new HashMap<String, Integer>();

	public Map<String, Double> strategy_values = new HashMap<String, Double>();
	public Map<String, ReactiveAction> strategy_actions = new HashMap<String, ReactiveAction>();
//	public double[] V;
//	public double[][] Q;
//	public ReactiveAction[] strategy;
	
	public void generateDistributions(Topology topology, TaskDistribution td) {
		probabilities = new double[cities.size()][cities.size()];
		rewards = new int[cities.size()][cities.size()];
		costs = new double[cities.size()][cities.size()];
		for (City from : topology){
			neighbours_prob.put(from.name, 0.0);
			neighbours_num.put(from.name, 0);
			cities_prob.put(from.name, 0.0);
			
			for (City to : topology){
				double val_city = cities_prob.get(from.name);
				val_city += probabilities[from.id][to.id];
				cities_prob.put(from.name, val_city);
				if (from.hasNeighbor(to)) {
					double val_prob = neighbours_prob.get(from.name);
					val_prob += probabilities[from.id][to.id];
					neighbours_prob.put(from.name, val_prob);
					int val_num = neighbours_num.get(from.name);
					val_num++;
					neighbours_num.put(from.name, val_num);
				}
				probabilities[from.id][to.id] = td.probability(from, to);
				rewards[from.id][to.id] = td.reward(from, to);
				costs[to.id][from.id] = from.distanceTo(to)*5;
			}
		}
		
	}
	
	public void generateStates(Topology topology) {
		for (City from : topology){
			for (City to : topology){
				if (!to.equals(from)) {
					ReactiveState pickup_state = new ReactiveState(from, to);
					states.put(pickup_state.id, pickup_state);
				}
			}
			ReactiveState move_state = new ReactiveState(from);
			states.put(move_state.id, move_state);
		}
		for(Map.Entry<String, ReactiveState> st : states.entrySet()) {
			strategy_values.put(st.getValue().id, 0.0);
			strategy_actions.put(st.getValue().id, null);
		}
	}
	
	public void generateActions(Topology topology) {
		ReactiveAction pickup_action = new ReactiveAction();
		actions.put(pickup_action.id, pickup_action);
		for (City city : topology){
			ReactiveAction move_action = new ReactiveAction(city);
			actions.put(move_action.id, move_action);
		}
	}
	
	public double getT(ReactiveState initState, ReactiveAction action, ReactiveState nextState) {
		if (action.isPickup() && initState.isPickup() && initState.destination == nextState.origin){
			if (nextState.isPickup()) {
				return probabilities[nextState.origin.id][nextState.destination.id];
			} else {
				return 1 - cities_prob.get(nextState.origin.name);
			}
		} else if (!action.isPickup() && !initState.isPickup() && initState.origin != action.destination && initState.areNeighbors(action.destination)) {
			if (nextState.isPickup()) {
				return probabilities[nextState.origin.id][nextState.destination.id];
			} else {
				return 1 - cities_prob.get(nextState.origin.name);
			}
		} else {
			return 0;		
		}
	}
	
	public double getR(ReactiveState state, ReactiveAction action){
		double r = 0.0;
		City from = state.origin;
		if (action.isPickup() && state.isPickup()){
			City to = state.destination;
			r = rewards[from.id][to.id] - costs[from.id][to.id];
		} else if (!action.isPickup() && !state.isPickup()){
			City to = action.destination;
			r = -costs[from.id][to.id];
		} else {
			r = 0.0;
		}
		return r;
	}
	public Map<String, ReactiveAction> getStrategy() {
		boolean keep_going = true;
		int counter = 0;
		while (keep_going) {
			counter++;
			Map<String, Double> old_strategy_values = new HashMap<String,Double>(strategy_values);
			for(Map.Entry<String, ReactiveState> initial_states : states.entrySet()) {
			    String initial_state_id = initial_states.getKey();
			    ReactiveState initial_state = initial_states.getValue();
			    double Q = 0.0;
			    for(Map.Entry<String, ReactiveAction> actions : actions.entrySet()) {
			    	String action_id = actions.getKey();
					ReactiveAction action = actions.getValue();
			    	double R = getR(initial_state, action);
					double sum = 0;
					for(Map.Entry<String, ReactiveState> final_states : states.entrySet()) {
						String final_state_id = final_states.getKey();
					    ReactiveState final_state = final_states.getValue();
					    double T = getT(initial_state, action, final_state);
					    T *= strategy_values.get(final_state.id);
					    sum += T;
					}
					double finalQ = R + pPickup*sum;
					if (finalQ > Q) {
						Q = finalQ;
						strategy_values.put(initial_state.id, Q);
						strategy_actions.put(initial_state.id, action);
					}
			    }
			}
			double max_difference = 0.0;
			for(Map.Entry<String, Double> values : strategy_values.entrySet()) {
				String id = values.getKey();
				double difference = Math.abs(strategy_values.get(id) - old_strategy_values.get(id));
				if (difference > max_difference) max_difference = difference;
			}
			
			if (max_difference < 0.00000001) keep_going = false;
		}
		return strategy_actions;
	}
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		
		cities = topology.cities();
		
		generateDistributions(topology, td);
		generateStates(topology);
		generateActions(topology);
		getStrategy();
	}
	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		//City pickupCity = availableTask.pickupCity;
		// City deliveryCity = availableTask.deliveryCity;
		if (availableTask == null) {
			City currentCity = vehicle.getCurrentCity();
			ReactiveState state = new ReactiveState(currentCity);
			ReactiveAction reactive_action = strategy_actions.get(state.id);
			//System.out.println("I am in " + currentCity.name + " and there are no tasks. Moving to " + reactive_action.destination );

			action = new Move(reactive_action.destination);
		} else {
			City currentCity = vehicle.getCurrentCity();
			ReactiveState state = new ReactiveState(currentCity, availableTask.deliveryCity);
            String statekey1 = state.id;
            if(strategy_actions.get(statekey1).pickup) {
    			//System.out.println("I am in " + currentCity.name + " and I take the task to " + availableTask.deliveryCity );
                action = new Pickup(availableTask);
            } else {
    			System.out.println("I am in " + currentCity.name + " and I don't take the task to " + availableTask.deliveryCity + ". Moving to " + strategy_actions.get(statekey1).destination );
                action = new Move(strategy_actions.get(statekey1).destination);
            }		
         }
		
		if (numActions >= 1) {
			//System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}


}


