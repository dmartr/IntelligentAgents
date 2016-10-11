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

/**
 * Reactive Agent
 * @author Ignacio Aguado, Darío Martínez
 */
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
	
	/**
	 *Store the rewards in int [][] rewards
	 *Store the costs in double [][] costs;
	 */
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
	
	/**
	 * Generate the ReactiveState objects
	 * Store them in the HashMap<String, ReactiveState> states
	 * Initializes HashMap<String, Double> strategy_values (Stores max(Q) for every state ID)
	 * Initializes HashMap<String, Action> strategy_actions (Stores action to take for every state ID)
	 */
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
	
	/**
	 * Generate the ReactiveAction objects
	 * Store them in the HashMap<String, ReactiveAction> actions
	 */
	public void generateActions() {
		ReactiveAction pickup_action = new ReactiveAction();
		actions.put(pickup_action.id, pickup_action);
		for (City city : topology){
			ReactiveAction move_action = new ReactiveAction(city);
			actions.put(move_action.id, move_action);
		}
	}
	
	/**
	 * Transitions Table 
	 * @return Probability of being in "nextState" taking the action "action" from state "initState"
	 * 
	 * @param initState Current State
	 * @param action Action taken from the current State
	 * @param nextState Next State
	 */
	public double getT(ReactiveState initState, ReactiveAction action, ReactiveState nextState) {
		
		// If there's a task and you take it, you end up in the destination of the task
		if (action.isPickup() && initState.isPickup() && initState.destination.equals(nextState.origin)) {
			return td.probability(nextState.origin, nextState.destination);
		} 
		// If there isn't a task and you end up moving to a neighbor city
		else if (!initState.isPickup() && !action.isPickup() && action.destination.equals(nextState.origin)) {
			if (action.destination.hasNeighbor(initState.origin)) {
				return td.probability(nextState.origin, nextState.destination);
			}
		}
		return 0;
	}
	
	/**
	 * Rewards Table
	 * @return Final reward for performing "action" from "state" (either positive or negative)
	 * 
	 * @param initState Current State
	 * @param action Action taken from the current State
	 */
	public double getR(ReactiveState state, ReactiveAction action){
		double r = 0.0;
		City from = state.origin;
		//If you pick up a task
		if (action.isPickup()){
			// If there was a task available, return reward-cost
			if (state.isPickup()){
				City to = state.destination;
				r = rewards[from.id][to.id] - costs[from.id][to.id];
			} else {
				// Impossible case, must penalize it (there wasn't a task but you tried to take one)
				r = Double.NEGATIVE_INFINITY;
			}
		// You move without pickin up a task
		} else {
			// If the next city is a neighbor, return cost
			if (state.origin.hasNeighbor(action.destination)) {
				City to = action.destination;
				r = - costs[from.id][to.id];
			} else {
				// Impossible case, must penalize it (you can't move to a not neighbor city without a task)
				r = Double.NEGATIVE_INFINITY;
			}
			
		} 
		return r;
	}
	
	/**
	 * Reinforcement Learning Algorithm
	 * When finished, updates strategy_actions with the best strategy for the agent 
	 * 
	 */
	public void getStrategy() {
		boolean keep_going = true;
		int counter = 0;
		// Maximum error in the results
		double epsilon = 0.01;
		
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

					// Only save it if it's a maximum for the state
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
			// Error satisfies condition. Finish algorithm.
			if (max_difference <= epsilon) keep_going = false;
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
		
		//If there's no task available, we look for the best neighbor to move to.
		if (availableTask == null) {
			City currentCity = vehicle.getCurrentCity();
			ReactiveState state = new ReactiveState(currentCity);
			ReactiveAction reactive_action = strategy_actions.get(state.id);
			System.out.println(currentCity.name + ": NO TASKS. MOVING TO " + reactive_action.destination);
			action = new Move(reactive_action.destination);
		//If there's a task available, we check whether is better to pick it up or move to a certain neighbor
		} else {
			City currentCity = vehicle.getCurrentCity();
			ReactiveState state = new ReactiveState(currentCity, availableTask.deliveryCity);
            String state_id = state.id;
            //It's better to pick up the task
            if(strategy_actions.get(state_id).pickup) {
    			System.out.println(currentCity.name + ": TASK TO " + availableTask.deliveryCity + ". ACCEPTED.");
                action = new Pickup(availableTask);
            //better to move to a certain neighbor
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


