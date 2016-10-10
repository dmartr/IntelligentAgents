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
	public double [][] probabilities;
	public List<City> cities;
	public int [][] rewards;
	public double [][] costs;
	public double [][][] transitions;
	public  Map<String, ReactiveState> states = new HashMap<String, ReactiveState>();
	public  Map<String, ReactiveAction> actions = new HashMap<String, ReactiveAction>();

	public Map<String, Double> neighbours_prob = new HashMap<String, Double>();
	public Map<String, Double> cities_prob = new HashMap<String, Double>();
	public Map<String, Integer> neighbours_num = new HashMap<String, Integer>();
	public Topology topology;
	public double[] V;
	public double[][] Q;
	public int[] pi;
	
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
	}
	
	public void generateActions(Topology topology) {
		ReactiveAction pickup_action = new ReactiveAction();
		actions.put(pickup_action.id, pickup_action);
		for (City city : topology){
			ReactiveAction move_action = new ReactiveAction(city);
			actions.put(move_action.id, move_action);
		}
	}
	
	public double getTransitionValue(ReactiveState initState, ReactiveAction action, ReactiveState nextState) {
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
	
	public void generatePolicy() {
		V = new double[states.size()];
		Q = new double[2][states.size()];
		pi = new int[states.size()];
		
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
		generatePolicy();
		
//		probabilities = new double[cities.size()][cities.size()];
//		rewards = new int[cities.size()][cities.size()];
//		costs = new double[cities.size()][cities.size()];
		
//		int position = 0;
//		for (City from : topology){
//			for (City to : topology){
//				if (!to.equals(from)) {
//					String state_id = from.name + "-" + to.name;
//					states.put(state_id, position);
//					position++;
//				}
//				probabilities[from.id][to.id] = td.probability(from, to);
//				rewards[from.id][to.id] = td.reward(from, to);
//				costs[to.id][from.id] = from.distanceTo(to)*5;
//			}
//		}

//		for (City from : topology) {
//			neighbours_prob.put(from.name, 0.0);
//			neighbours_num.put(from.name, 0);
//			cities_prob.put(from.name, 0.0);
//
//			for (City to : topology) {
//					double val_city = cities_prob.get(from.name);
//					val_city += probabilities[from.id][to.id];
//					cities_prob.put(from.name, val_city);
//				if (from.hasNeighbor(to)) {
//					double val_prob = neighbours_prob.get(from.name);
//					val_prob += probabilities[from.id][to.id];
//					neighbours_prob.put(from.name, val_prob);
//					int val_num = neighbours_num.get(from.name);
//					val_num++;
//					neighbours_num.put(from.name, val_num);
//				}
//			}
//		}
//		transitions = new double[2][states.size()][states.size()];
//		V = new double[states.size()];
//		Q = new double[2][states.size()];
//		pi = new int[states.size()];
//		for(Map.Entry<String, Integer> initials : states.entrySet()) {
//		    String id_initial = initials.getKey();
//		    int state_initial = initials.getValue();
//		    String[] parts_initial = id_initial.split("-");
//		    
//		    City from_initial = topology.parseCity(parts_initial[0]);
//		    City to_initial = topology.parseCity(parts_initial[1]);
//
//		    for(Map.Entry<String, Integer> finals : states.entrySet()) {
//			    String id_final = finals.getKey();
//			    int state_final = finals.getValue();
//			    String[] parts_final = id_final.split("-");
//			    
//			    City from_final = topology.parseCity(parts_final[0]);
//			    City to_final = topology.parseCity(parts_final[1]);
//			    if (from_final.equals(from_initial) || !to_initial.equals(from_final) ) {
//			    	transitions[0][state_final][state_initial] = 0;
//			    	transitions[1][state_final][state_initial] = 0;
//			    } else if (to_initial.equals(from_final)){
//			    	if (from_final.hasNeighbor(to_final)) {
//			    		transitions[0][state_final][state_initial] = probabilities[from_final.id][to_final.id] + ((1-cities_prob.get(from_final.name))/neighbours_num.get(from_final.name));
//			    	} else {
//			    		transitions[0][state_final][state_initial] = probabilities[from_final.id][to_final.id];
//			    	}
//			    	if (!from_final.hasNeighbor(to_final)){
//			    		transitions[1][state_final][state_initial] = 0;
//			    	} else {
//			    		transitions[1][state_final][state_initial] = probabilities[from_final.id][to_final.id] / neighbours_prob.get(from_final.name);
//			    	}
//			    }
//		    }
//		}	
		int[] a = getPi();
		for (int i=0; i<pi.length; i++) {
			System.out.println(a[i]);
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
	
	public double getR(String state, int action){
		double r = 0; 
		String[] parts = state.split("-");
		int from = topology.parseCity(parts[0]).id;
		int to = topology.parseCity(parts[1]).id;
		if (action == 0){
			r = rewards[from][to] - costs[from][to];
		} else {
			r = -costs[from][to];
		}
		return r;
	}
	
//	public double getT(int pos_initial, int action, int pos_final){
//		double t;
//		// If move
//		if (action == 1) {
//			t = transitions[1][pos_final][pos_initial];
//		// If pickup
//		} else {
//			t = transitions[0][pos_final][pos_initial];
//		}
//		return t;
//	}

	public double[] getV() {
		boolean keep_going = true;
		int counter = 0;
		while (keep_going) {
			counter++;
			double[] old_v = Arrays.copyOf(V, V.length);
			for(Map.Entry<String, Integer> initial_states : states.entrySet()) {
			    String initial_state = initial_states.getKey();
			    int pos_initial = initial_states.getValue();
	
				for(int action = 0; action<=1; action++) {
					double R = getR(initial_state, action);
					double T = 0;
					for(Map.Entry<String, Integer> final_states : states.entrySet()) {
					    String final_state = final_states.getKey();
					    int pos_final = final_states.getValue();
					    double sum = getT(pos_initial, action, pos_final);
					    sum *= V[pos_final];
					    T += sum;
					}
					Q[action][pos_initial] = R + pPickup*T;
				}
				if (Q[0][pos_initial] > Q[1][pos_initial]) V[pos_initial] = Q[0][pos_initial];
				else V[pos_initial] = Q[1][pos_initial];
			}
			double max_difference = 0.0;
			for (int i=0; i<V.length; i++) {
				double difference = Math.abs(V[i] - old_v[i]);
				if (difference > max_difference) max_difference = difference;
			}
			
			if (max_difference < 1) keep_going = false;
		}
		return V;
	}
	public int[] getPi() {
		double[] my_v = getV();
		for(Map.Entry<String, Integer> initial_states : states.entrySet()) {
		    String initial_state = initial_states.getKey();
		    int pos_initial = initial_states.getValue();

			for(int action = 0; action<=1; action++) {
				double R = getR(initial_state, action);
				double T = 0;
				for(Map.Entry<String, Integer> final_states : states.entrySet()) {
				    String final_state = final_states.getKey();
				    int pos_final = final_states.getValue();
				    double sum = getT(pos_initial, action, pos_final);
				    sum *= my_v[pos_initial];
				    T += sum;
				    
				}
				Q[action][pos_initial] = R + pPickup*T;
			}
			if (Q[0][pos_initial] > Q[1][pos_initial]) {
				System.out.println(Q[0][pos_initial] + "********" + Q[1][pos_initial]);
				pi[pos_initial] = 0;
			}
			else {
				System.out.println(Q[0][pos_initial] + "+++++++++" + Q[1][pos_initial]);
				pi[pos_initial] = 1;
			}
		}
		return pi;
	}
}


