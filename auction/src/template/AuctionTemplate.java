package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private City currentCity;
	
	private AuctionPlan myPlan;
	private AuctionPlan oppPlan;
	
	private double myCost;
	private double myNCost;
	private double oppCost;
	private double oppNCost;
	private double myMarginalCost;
	
	private double payDay;
	
	private double adjustRatio;
	
	private ArrayList<AuctionVehicle> myVehicles;
	private ArrayList<AuctionVehicle> oppVehicles;
	
	private long allowedTime;
	double [][] probability;
	
    private long timeout_setup;
    private long timeout_plan;
	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;

		probability = new double[topology.size()][topology.size()];
		
		List<Vehicle> vehicles = agent.vehicles();
		myVehicles = new ArrayList<AuctionVehicle>(vehicles.size());
		for(Vehicle v : vehicles){
			System.out.println(v);
			AuctionVehicle auctionVehicle = new AuctionVehicle(v);
			myVehicles.add(auctionVehicle);
		}
		System.out.println(agent.vehicles());
		this.myPlan = new AuctionPlan(myVehicles);
		long seed = -9019554669489983951L;
		//long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		
		LogistSettings ls = null;
		try {
            ls = Parsers.parseSettings("config"+File.separator+"settings_auction.xml");
    		allowedTime = ls.get(LogistSettings.TimeoutKey.PLAN);
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the auction configuration file.");
        }
		
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		long myBid = bids[agent.id()];
		long oppBid = bids[1-agent.id()];			
		//System.out.println("I'm agent " + agent.id() + " and I bid " + myBid + " and my marginal is " + myMarginalCost);

		//System.out.println(myBid + " vs " + oppBid);
		if (winner == agent.id()) {
			//System.out.println("Agent " + agent.id() + " won!");

			currentCity = previous.deliveryCity;
			myCost = myNCost;
			myPlan.updatePlan();
			payDay += myBid;
			
		} else {
			oppCost = oppNCost;
			//oppPlan.updatePlan();
		}
	}
	
	@Override
	public Long askPrice(Task task) {

		if (myPlan.getBiggestVehicle().getCapacity() < task.weight)
			return null;

		myNCost = myPlan.getNewPlan(task).planCost();
		myMarginalCost =  myNCost-myCost;
		//if (myMarginalCost < 0) myMarginalCost = 0;
		
		System.out.println("Previous:" +  myCost + " New: " + myNCost + " Marginal:" + myMarginalCost);
		double bid = myMarginalCost + 1;
		return (long) Math.round(bid);
		/**
		//oppNCost = oppPlan.getNewPlan(task).planCost();
		//oppNCost = 0;
		//double myMCost = myNCost-myCost;
		//double oppMCost = oppNCost-oppCost;
		
		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;
		return (long) Math.round(bid);
		**/
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();

		AuctionPlan auctionPlan = new AuctionPlan(myVehicles);
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		auctionPlan.getFinalPlan(tasks);
		SLS sls = new SLS(myVehicles, new ArrayList<Task>(tasks));
		CentralizedPlan slsPlan = sls.selectInitialSolutionDistance();
        int MAX_ITERS = 5000;
        for (int i = 0; i<MAX_ITERS; i++) {
        	// Find all possible neighbors
        	ArrayList<CentralizedPlan> neighbors = sls.chooseNeighbors(slsPlan);
        	if (neighbors != null) {
        		// Choose the best plan
	        	CentralizedPlan newPlan = sls.localChoice(slsPlan, neighbors);
	        	slsPlan = newPlan;
        	}
        }
        
        // Final distribution of the tasks, cost and distance
        System.out.println("FINAL PLAN:");
		System.out.println("	Task distribution: " + slsPlan.toString());
		System.out.println("	Cost: " + slsPlan.planCost());
		System.out.println("	Distance: " + slsPlan.planDistance());
        //selectedPlan.paint();

        List<Plan> plans = new ArrayList<Plan>();
        for (Vehicle v : vehicles) {
        	Plan vehiclePlan = centralizedPlan(v, slsPlan.planTasks.get(v.id()));
    		plans.add(vehiclePlan);
        }
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in "+duration+" milliseconds.");
        
        return plans;
	}

	private Plan centralizedPlan(Vehicle vehicle, LinkedList<AuctionTask> tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);
        int distance = 0;
        for (AuctionTask task : tasks) {
        	if (task.pickup) {
            // move: current city => pickup location
	            for (City city : current.pathTo(task.pickupCity)) {
	            	//System.out.println("Move to " + city.name);
	                plan.appendMove(city);
	            }
	            //System.out.println("Pick up in " + task.task.pickupCity.name);
	            plan.appendPickup(task.task);
	            current = task.pickupCity;
        	} else {
                // move: pickup location => delivery location
                for (City city : current.pathTo(task.deliveryCity)) {
	            	//System.out.println("Move to " + city.name);
                    plan.appendMove(city);
                }
	            //System.out.println("Delivery " + task.task.deliveryCity.name);
                plan.appendDelivery(task.task);
                current = task.deliveryCity;
        	}            
        }
        return plan;
    }
}
