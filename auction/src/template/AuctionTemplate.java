package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

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
	private double distributionMean;
	private Agent agent;
	private Random random;
	private City currentCity;
	
	private AuctionPlan myPlan;
	private AuctionPlan oppPlan;
	
	private double myCost;
	private double oppCost;
	private double myDistance;
	private double oppDistance;
	
	private double myNewCost;
	private double oppNewCost;
	
	private double myNewDistance;
	private double oppNewDistance;
	
	private double myMarginalCost;
	private double oppMarginalCost;
	
	private double minBound;
	private double maxBound;
	private double oppRatioMC;	
	
	private double myCostPerKm;
	private double oppCostPerKm;
	
	private double myPayDay;
	private double oppPayDay;
	
	private int round;
	private double adjustRatio;
	private double greedyRatio;
	private double oppRatio;
	
	private ArrayList<AuctionVehicle> myVehicles;
	private ArrayList<AuctionVehicle> oppVehicles;
	private ArrayList<Task> myPlanTasks;
	private ArrayList<Task> oppPlanTasks;
	
	private long allowedTime;
	
    private long timeout_setup;
    private long timeout_plan;
    
	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.oppMarginalCost = 0;
		this.myCostPerKm = 0;
		this.oppCostPerKm = 0;
		this.round = 0;
		this.oppRatioMC = 0.2;
		this.myDistance = 0;
		this.oppDistance = 0;
		this.adjustRatio = 0.85;
		this.minBound = 0;
		this.maxBound = 0;
		this.greedyRatio = 0.5;
		
		myPlanTasks = new ArrayList<Task>();
		oppPlanTasks = new ArrayList<Task>();
		
		List<Vehicle> vehicles = agent.vehicles();
		myVehicles = new ArrayList<AuctionVehicle>(vehicles.size());
		for(Vehicle v : vehicles){
			AuctionVehicle auctionVehicle = new AuctionVehicle(v);
			myVehicles.add(auctionVehicle);
		}
		
		this.myPlan = new AuctionPlan(myVehicles);
		this.oppPlan = new AuctionPlan(myVehicles);
		
		double mean = 0;
		for (City from : topology.cities()) {
			for (City to : topology.cities()) {
				mean += distribution.probability(from, to);
			}
		}
		this.distributionMean = mean / (topology.cities().size()*topology.cities().size());
		
		//System.out.println(distributionMean);
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
		round++;
		long myBid = bids[agent.id()];
		long oppBid = bids[1-agent.id()];
		
		if (oppMarginalCost != 0) oppRatio = Math.min(oppBid/oppMarginalCost, 5);
		else oppRatio = 1;
		
		double myTempDistance = myNewDistance - myDistance;
		double oppTempDistance = oppNewDistance - oppDistance;
		System.out.println("Round " + round + ". Winner: " + winner + " " + myBid + " vs " + oppBid);
		if (round==1) {
			
			myCostPerKm = myMarginalCost/myTempDistance;
					
			oppMarginalCost = oppBid;
			if (oppTempDistance > 0) {
				oppCostPerKm = oppMarginalCost/oppTempDistance;
				oppCostPerKm = Math.max(myCostPerKm*(1-oppRatioMC), oppCostPerKm);
				oppCostPerKm = Math.min(myCostPerKm*(1+oppRatioMC), oppCostPerKm);
			}
		} else {
			if (myTempDistance > 0) {
				myCostPerKm += myMarginalCost/myTempDistance;
				myCostPerKm /= 2;
			}
			if (oppTempDistance > 0) {
					oppCostPerKm = oppMarginalCost/oppTempDistance;
					oppCostPerKm = Math.max(myCostPerKm*(1-oppRatioMC), oppCostPerKm);
					oppCostPerKm = Math.min(myCostPerKm*(1+oppRatioMC), oppCostPerKm);
			}
		}
		int x = agent.id()-1;
		oppDistance = oppNewDistance;

		if (winner == agent.id()) {
			myPlanTasks.add(previous);
			currentCity = previous.deliveryCity;
			myCost = myNewCost;
			myPlan.updatePlan();
			myPayDay += myBid;
			myDistance = myNewDistance;
			if (myPlanTasks.size() > 3)
			adjustRatio += 0.02;
		} else {
			oppPlanTasks.add(previous);
			oppCost = oppNewCost;
			oppPlan.updatePlan();
			oppPayDay += oppBid;
			oppDistance = oppNewDistance;
			if (myPlanTasks.size() > 3)
			adjustRatio -= 0.02;
			if (round < 4) greedyRatio += 0.2;
			greedyRatio = Math.min(1, greedyRatio);
		}
	}
	
	@Override
	public Long askPrice(Task task) {

		if (myPlan.getBiggestVehicle().getCapacity() < task.weight)
			return null;

		CentralizedPlan myNewPlan = myPlan.getNewPlan(task);
		CentralizedPlan oppNewPlan = oppPlan.getNewPlan(task);
		
		myNewCost = myNewPlan.planCost();
		oppNewCost = oppNewPlan.planCost();
		
		myNewDistance = myNewPlan.planDistance();
		oppNewDistance = oppNewPlan.planDistance();
		
		myMarginalCost =  myNewCost-myCost;
		oppMarginalCost = Math.max((oppNewDistance-oppDistance)*oppCostPerKm,0);

		CentralizedPlan thePlan = myPlan.actualPlan;
		int vehicle = myPlan.getVehicle(task);
		double bid;
		
		double sumProbabilities = 0;
		int numProbabilities = 0;
		boolean greater = false;
		
		for (int i=0; i< thePlan.planTasks.get(vehicle).size(); i++) {
			AuctionTask newTask = thePlan.planTasks.get(vehicle).get(i);
			if (greater) {
				if (newTask.pickup && distribution.probability(task.pickupCity, newTask.pickupCity) > distributionMean) { 
					sumProbabilities += distribution.probability(task.pickupCity, newTask.pickupCity);
					numProbabilities++;
				}
				else if (newTask.delivery && distribution.probability(task.pickupCity, newTask.pickupCity) > distributionMean) {
					sumProbabilities += distribution.probability(task.pickupCity, newTask.deliveryCity);
					numProbabilities++;
				}
			}
			if (newTask.task.equals(task)) greater = true;
		}
		
		double probabilityBonus = 1;
		if (numProbabilities > 0) probabilityBonus = 1-2*(sumProbabilities/numProbabilities - distributionMean);
		double initialBid;
		double losingRatio = 1;

		if (myPlanTasks.size() < 4) {
			bid = myMarginalCost * 0.3;
			return (long) Math.round(bid);
		} else {	
			double myRealCost = task.pickupCity.distanceTo(task.deliveryCity)*myVehicles.get(vehicle).costPerKm;
			double oppRealCost = task.pickupCity.distanceTo(task.deliveryCity)*oppCostPerKm;
			if (myMarginalCost == 0 && oppMarginalCost != 0) {
				System.out.println("Mine is 0 his is not " + oppMarginalCost + " " + oppRealCost );
				minBound = oppMarginalCost;
				maxBound = oppRealCost;
				initialBid = (minBound+maxBound)/2;
			} else if (myMarginalCost == 0 && oppMarginalCost == 0) {
				System.out.println("Mine is 0 his is 0");
				minBound = (myRealCost+oppRealCost)*0.5*greedyRatio;
				maxBound =  (myRealCost+oppRealCost)*0.5;
				initialBid = maxBound;
			} else if (myMarginalCost < oppMarginalCost) {
				System.out.println("Mine is lower");
				minBound = (myMarginalCost + oppMarginalCost) / 2;
				maxBound = myRealCost;
				initialBid = (myMarginalCost + oppMarginalCost)*0.5;
			} else {
				System.out.println("Else " + myMarginalCost + " " + oppMarginalCost);
				minBound = myMarginalCost*0.8;
				maxBound = myMarginalCost;
				initialBid = maxBound;
			}
		}
		
		bid = Math.min(maxBound, initialBid*adjustRatio*probabilityBonus);

		bid = Math.max(minBound, bid);

		return (long) Math.round(bid);

	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        double timeMargin = 0.75;
		AuctionPlan auctionPlan = new AuctionPlan(myVehicles);

		SLS sls = new SLS(myVehicles, new ArrayList<Task>(tasks));
		CentralizedPlan slsPlan = sls.selectInitialSolutionDistance();
        int MAX_ITERS = 5000;
        for (int i = 0; i<MAX_ITERS; i++) {
        	// Find all possible neighbors
            if (System.currentTimeMillis() - time_start < allowedTime*timeMargin) {
            	ArrayList<CentralizedPlan> neighbors = sls.chooseNeighbors(slsPlan);
            	if (neighbors != null) {
            		// Choose the best plan
            		CentralizedPlan newPlan = sls.localChoice(slsPlan, neighbors);
            		slsPlan = newPlan;
            	}
            }
        }
        //slsPlan = myPlan.getFinalPlan(tasks, myVehicles);

        if (slsPlan.planCost() > myPlan.bestPlan.planCost()) slsPlan = myPlan.getFinalPlan(tasks, myVehicles);

        // Final distribution of the tasks, cost and distance
        System.out.println("FINAL PLAN:");
		System.out.println("	Task distribution: " + slsPlan.toString());
		System.out.println("	Cost: " + slsPlan.planCost());
		System.out.println("	Distance: " + slsPlan.planDistance());
		double benefits = myPayDay-slsPlan.planCost();
		System.out.println("    Getting Payed: " + myPayDay);    
		System.out.println("	Benefits: " + benefits);

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
