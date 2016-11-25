package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.Collections;
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
	private double myNewCost;
	private double oppCost;
	private double oppNewCost;
	private double myMarginalCost;
	private double oppMarginalCost;
	
	private double myMarginalCostPerKm;
	private double oppMarginalCostPerKm;
	
	private double myPayDay;
	private double oppPayDay;

	
	private int round = 0;
	private double adjustRatio;
	
	private ArrayList<AuctionVehicle> myVehicles;
	private ArrayList<AuctionVehicle> oppVehicles;
	private ArrayList<Task> myPlanTasks;
	private ArrayList<Task> oppPlanTasks;
	
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
		
		myPlanTasks = new ArrayList<Task>();
		oppPlanTasks = new ArrayList<Task>();

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
		this.oppPlan = new AuctionPlan(myVehicles);
		
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
		round++;
			//System.out.println("I'm agent " + agent.id() + " and I bid " + myBid + " and my marginal is " + myMarginalCost);
		//System.out.println(myBid + " vs " + oppBid);
		if (winner == agent.id()) {
			//System.out.println("Agent " + agent.id() + " won!");
			myPlanTasks.add(previous);
			currentCity = previous.deliveryCity;
			myCost = myNewCost;
			myPlan.updatePlan();
			myPayDay += myBid;
			
		} else {
			oppPlanTasks.add(previous);
			oppCost = oppNewCost;
			oppPlan.updatePlan();
			oppPayDay += oppBid;
		}
	}
	
	@Override
	public Long askPrice(Task task) {

		if (myPlan.getBiggestVehicle().getCapacity() < task.weight)
			return null;

		myNewCost = myPlan.getNewPlan(task).planCost();
		oppNewCost = oppPlan.getNewPlan(task).planCost();
		myMarginalCost =  myNewCost-myCost;
		oppMarginalCost = oppNewCost-oppCost;
		//if (round <=3) {
			
			//if (myMarginalCost < 0) myMarginalCost = 0;
			
			System.out.println("Previous:" +  myCost + " New: " + myNewCost + " Marginal:" + myMarginalCost);
			double bid = myMarginalCost + 1;
			return (long) Math.round(bid);
		//}

	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        double timeMargin = 0.8;
		AuctionPlan auctionPlan = new AuctionPlan(myVehicles);
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		auctionPlan.getFinalPlan(tasks);
		SLS sls = new SLS(myVehicles, new ArrayList<Task>(tasks));
		CentralizedPlan slsPlan = sls.selectInitialSolutionDistance();
        int MAX_ITERS = 5000;
        for (int i = 0; i<MAX_ITERS; i++) {
        	// Find all possible neighbors
            if (System.currentTimeMillis() - time_start > allowedTime*timeMargin) {
            	ArrayList<CentralizedPlan> neighbors = sls.chooseNeighbors(slsPlan);
            	if (neighbors != null) {
            		// Choose the best plan
            		CentralizedPlan newPlan = sls.localChoice(slsPlan, neighbors);
            		slsPlan = newPlan;
            	}
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
