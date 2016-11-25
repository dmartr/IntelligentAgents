package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import logist.task.Task;

public class AuctionPlan {
	
	private AuctionVehicle biggestVehicle;
	private CentralizedPlan actualPlan;
	private CentralizedPlan bestPlan;	
	
	public AuctionPlan(ArrayList<AuctionVehicle> vehicles){
		double capacity = 0;
		this.actualPlan = new CentralizedPlan(vehicles);
		for(AuctionVehicle v : vehicles){
			if(capacity < v.getCapacity()){
				capacity = v.getCapacity();
				biggestVehicle = v;
			}
		}
	}
	
	public AuctionVehicle getBiggestVehicle(){
		return biggestVehicle;
	}
	
	public List<CentralizedPlan> insertTask(Task auctionedTask){
		List<CentralizedPlan> newPlanList;newPlanList = new ArrayList<CentralizedPlan>();
		AuctionTask auctionedPickUp = new AuctionTask("PICKUP", auctionedTask);
		AuctionTask auctionedDeliver = new AuctionTask("DELIVER", auctionedTask);
		for(Entry<Integer, LinkedList<AuctionTask>> entry : actualPlan.planTasks.entrySet()){
			LinkedList<AuctionTask> taskList = entry.getValue();
			for(int pos1=0;pos1<=taskList.size();pos1++){				
				for(int pos2=pos1+1;pos2<=taskList.size()+1;pos2++){
					HashMap<Integer, LinkedList<AuctionTask>> newPlanTasks = actualPlan.cloneHashmap(actualPlan.planTasks);
					LinkedList <AuctionTask> newTaskList = taskList;
					newTaskList.add(pos1, auctionedPickUp);
					newTaskList.add(pos2, auctionedDeliver);
					newPlanTasks.put(entry.getKey(), newTaskList);
					newPlanList.add(new CentralizedPlan(newPlanTasks, actualPlan.vehicles));
				}
			}
		}
	return newPlanList;
	}
	
	private CentralizedPlan chooseBestPlan(CentralizedPlan oldPlan, List<CentralizedPlan> newPLanList){
		CentralizedPlan cheapest = oldPlan; 
		double minCost = Integer.MAX_VALUE;
		for(CentralizedPlan plan : newPLanList){
			double cost = plan.planCost();
			if(cost < minCost){
				cheapest = plan;
				minCost = cost;
			}
		}
		this.bestPlan = cheapest;
		return cheapest;
	}
	
	public CentralizedPlan getNewPlan(Task task){
		List<CentralizedPlan> newPlanList = insertTask(task);
		CentralizedPlan newBestPlan = chooseBestPlan(actualPlan, newPlanList);
		return newBestPlan;
	}
	
	public CentralizedPlan getBestPlan() {
		return bestPlan;
	}
	
}