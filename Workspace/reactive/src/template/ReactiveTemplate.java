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

	private TaskDistribution td;
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
	public Map<String, ReactiveAction> old_strategy_actions = new HashMap<String, ReactiveAction>();	

	public Map<String, Double> v = new HashMap<String, Double>();
	
	public Map<String, Double> action_q = new HashMap<String, Double>();
	public Map<String, Map<String, Double>> q = new HashMap<String, Map<String, Double>>();
	
	public void generateDistributions(Topology topology, TaskDistribution td) {
		probabilities = new double[cities.size()][cities.size()];
		rewards = new int[cities.size()][cities.size()];
		costs = new double[cities.size()][cities.size()];
		for (City from : topology){
			neighbours_prob.put(from.name, 0.0);
			neighbours_num.put(from.name, 0);
			cities_prob.put(from.name, 1.0);
			
			for (City to : topology){
				probabilities[from.id][to.id] = td.probability(from, to);
				rewards[from.id][to.id] = td.reward(from, to);
				costs[from.id][to.id] = from.distanceTo(to)*5;
				double val_city = cities_prob.get(from.name);
				val_city *= 1-probabilities[from.id][to.id];
				cities_prob.put(from.name, val_city);
				if (from.hasNeighbor(to)) {
					double val_prob = neighbours_prob.get(from.name);
					val_prob += probabilities[from.id][to.id];
					neighbours_prob.put(from.name, val_prob);
					int val_num = neighbours_num.get(from.name);
					val_num++;
					neighbours_num.put(from.name, val_num);
				}
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
		for(Map.Entry<String, ReactiveAction> ac : actions.entrySet()) {
			v.put(ac.getKey(), 0.0);	
		}
		for(Map.Entry<String, ReactiveState> st : states.entrySet()) {
			strategy_values.put(st.getValue().id, 0.0);
			v.put(st.getValue().id, 0.0);
			strategy_actions.put(st.getValue().id, null);
			old_strategy_actions.put(st.getValue().id, null);
			q.put(st.getValue().id, v);

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
		
		if (action.isPickup() && initState.isPickup() && initState.destination.equals(nextState.origin)) {
			return td.probability(nextState.origin, nextState.destination);
		} else if (!initState.isPickup() && !action.isPickup() && action.destination.equals(nextState.origin)) {
			if (action.destination.hasNeighbor(initState.origin)) {
				return td.probability(nextState.origin, nextState.destination);
			}
		}
		return 0;
	}
	
	public double getR(ReactiveState state, ReactiveAction action){
		double r = 0.0;
		City from = state.origin;
		if (action.isPickup()){
			if (state.isPickup()){
				City to = state.destination;
				r = rewards[from.id][to.id] - costs[from.id][to.id];
			} else {
				r = Double.NEGATIVE_INFINITY;
			}
		} else {
			if (state.origin.hasNeighbor(action.destination)) {
				City to = action.destination;
				r = - costs[from.id][to.id];
			} else {
				r = Double.NEGATIVE_INFINITY;
			}
			
		} 
		return r;
	}
	
	public void getStrategy() {
		boolean keep_going = true;
		int counter = 0;
		while (keep_going) {
			counter++;
			Map<String, Double> old_strategy_values = new HashMap<String,Double>(strategy_values);
			for(Map.Entry<String, ReactiveState> initial_states : states.entrySet()) {
				ReactiveState initial_state = initial_states.getValue();
				String initial_state_id = initial_states.getKey();
				double max_q = Double.NEGATIVE_INFINITY;
				ReactiveAction best_action = null;
				for(Map.Entry<String, ReactiveAction> actions : actions.entrySet()) {

					ReactiveAction action = actions.getValue();
					
					double total = 0.0;
					for(Map.Entry<String, ReactiveState> final_states : states.entrySet()) {
						ReactiveState final_state = final_states.getValue();
						String final_state_id = final_states.getKey();
						double v_value = strategy_values.get(final_state_id);
						double t_value = getT(initial_state, action, final_state);
						total += v_value*t_value;
					}
					double r_value = getR(initial_state, action);
					double q_value = r_value + pPickup*total;
					action_q.put(action.id, q_value);
					//System.out.println("Acción " + action.id + " es " + q_value);
					if (q_value > max_q) {
						max_q = q_value;
						best_action = action;
					}
				}
				q.put(initial_state_id, action_q);
				strategy_values.put(initial_state_id, max_q);
				strategy_actions.put(initial_state_id, best_action);
			}
			double max_difference = 0.0;
			for(Map.Entry<String, ReactiveState> st : states.entrySet()) {
				double difference = Math.abs(strategy_values.get(st.getKey()) - old_strategy_values.get(st.getKey()));
				if (difference > max_difference) max_difference = difference;
			}
			if (max_difference <= 1) {
				keep_going = false;
			}
		}
		System.out.println("Iterations: " + counter);

	}
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
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

//		for(Map.Entry<String, ReactiveAction> st : strategy_actions.entrySet()) {
//			
//			System.out.println(st.getValue().id + " - "+ st.getKey());
//		}
		
//		for(Map.Entry<String, Map<String, Double>> test : q.entrySet()) {
//			System.out.println("Para el estado " + test.getKey() + ": ");
//			Map<String, Double> a = test.getValue();
//			for(Map.Entry<String, Double> b : a.entrySet()) {
//				System.out.println("Acción " + b.getKey() + " es " + b.getValue());
//			}
//		}
		
//		for(Map.Entry<String, ReactiveAction> a : strategy_actions.entrySet()) {
//			System.out.println("Estado: " + a.getKey() + " Acción: " + a.getValue().id);
//		}
	}
	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null) {
			City currentCity = vehicle.getCurrentCity();
			ReactiveState state = new ReactiveState(currentCity);
			ReactiveAction reactive_action = strategy_actions.get(state.id);
			//System.out.println("I am in " + currentCity.name + " and there are no tasks. Moving to " + reactive_action.destination );
			action = new Move(reactive_action.destination);
		} else {
			City currentCity = vehicle.getCurrentCity();
			ReactiveState state = new ReactiveState(currentCity, availableTask.deliveryCity);
            String state_id = state.id;
            if(strategy_actions.get(state_id).pickup) {
    			//System.out.println("I am in " + currentCity.name + " and I take the task to " + availableTask.deliveryCity );
                action = new Pickup(availableTask);
            } else {
    			System.out.println("I am in " + currentCity.name + " and I don't take the task to " + availableTask.deliveryCity + ". Moving to " + strategy_actions.get(state_id).destination );
    			action = new Move(strategy_actions.get(state_id).destination);
            }		
         }
		
		if (numActions >= 1) {
			//System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}


