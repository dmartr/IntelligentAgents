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
	
	public int [][] rewards;
	public double [][] costs;

	public  Map<String, ReactiveState> states = new HashMap<String, ReactiveState>();
	public  Map<String, ReactiveAction> actions = new HashMap<String, ReactiveAction>();

	public Map<String, Double> strategy_values = new HashMap<String, Double>();
	public Map<String, ReactiveAction> strategy_actions = new HashMap<String, ReactiveAction>();
	
	public void generateDistributions() {
		rewards = new int[cities.size()][cities.size()];
		costs = new double[cities.size()][cities.size()];
		for (City from : topology){	
			for (City to : topology){
				rewards[from.id][to.id] = td.reward(from, to);
				costs[from.id][to.id] = from.distanceTo(to)*5;
			}
		}
	}
	
	public void generateStates() {
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
	
	public void generateActions() {
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

					if (q_value > max_q) {
						max_q = q_value;
						best_action = action;
					}
				}
				strategy_values.put(initial_state_id, max_q);
				strategy_actions.put(initial_state_id, best_action);
			}
			double max_difference = 0.0;
			for(Map.Entry<String, ReactiveState> st : states.entrySet()) {
				double difference = Math.abs(strategy_values.get(st.getKey()) - old_strategy_values.get(st.getKey()));
				if (difference > max_difference) max_difference = difference;
			}
			if (max_difference <= 1) keep_going = false;
		}
		System.out.println("Final number of iterations: " + counter);

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
		
		generateDistributions();
		generateStates();
		generateActions();
		getStrategy();

//		for(Map.Entry<String, ReactiveAction> a : strategy_actions.entrySet()) {
//			System.out.println("State ID: " + a.getKey() + " and Action ID: " + a.getValue().id);
//		}
	}
	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null) {
			City currentCity = vehicle.getCurrentCity();
			ReactiveState state = new ReactiveState(currentCity);
			ReactiveAction reactive_action = strategy_actions.get(state.id);
			System.out.println(currentCity.name + ": NO TASKS. MOVING TO " + reactive_action.destination);
			action = new Move(reactive_action.destination);
		} else {
			City currentCity = vehicle.getCurrentCity();
			ReactiveState state = new ReactiveState(currentCity, availableTask.deliveryCity);
            String state_id = state.id;
            if(strategy_actions.get(state_id).pickup) {
    			System.out.println(currentCity.name + ": TASK TO " + availableTask.deliveryCity + ". ACCEPTED.");
                action = new Pickup(availableTask);
            } else {
    			System.out.println(currentCity.name + ": TASK TO " + availableTask.deliveryCity + ". DENIED. MOVING TO " + strategy_actions.get(state_id).destination);
    			action = new Move(strategy_actions.get(state_id).destination);
            }		
         }
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}


