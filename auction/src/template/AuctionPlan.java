package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import logist.task.Task;
import logist.task.TaskSet;

public class AuctionPlan {
	
	public AuctionVehicle biggestVehicle;
	public CentralizedPlan actualPlan;
	public CentralizedPlan bestPlan;	
	
	public AuctionPlan(List<AuctionVehicle> vehicles){
		double capacity = 0;
		this.actualPlan = new CentralizedPlan(vehicles);
		for(AuctionVehicle v : vehicles){
			if(capacity < v.getCapacity()){
				capacity = v.getCapacity();
				biggestVehicle = v;
			}
		}
		this.bestPlan = new CentralizedPlan(vehicles);
	}
	
	public AuctionVehicle getBiggestVehicle(){
		return biggestVehicle;
	}
	

	
	public List<CentralizedPlan> insertTask(Task auctionedTask, CentralizedPlan plan){
		List<CentralizedPlan> newPlanList = new ArrayList<CentralizedPlan>();
		AuctionTask auctionedPickUp = new AuctionTask("PICKUP", auctionedTask);
		AuctionTask auctionedDeliver = new AuctionTask("DELIVERY", auctionedTask);	
		CentralizedPlan newPlan = new CentralizedPlan(plan);
		for(Entry<Integer, LinkedList<AuctionTask>> entry : newPlan.planTasks.entrySet()){
			LinkedList<AuctionTask> taskList = entry.getValue();
			for(int pos1=0; pos1<taskList.size()+1; pos1++){
				LinkedList <AuctionTask> newTaskList = new LinkedList<AuctionTask>(taskList);
				newTaskList.add(pos1, auctionedPickUp);
				for(int pos2=pos1+1; pos2<newTaskList.size()+1; pos2++){
					CentralizedPlan finalPlan = new CentralizedPlan(newPlan);
					LinkedList <AuctionTask> finalTaskList = new LinkedList<AuctionTask>(newTaskList);
					finalTaskList.add(pos2, auctionedDeliver);
					finalPlan.planTasks.put(entry.getKey(), finalTaskList);
					CentralizedPlan  newCentralizedPlan = new CentralizedPlan(finalPlan.planTasks, plan.vehicles);
					if (newCentralizedPlan.validConstraints()) {
						newPlanList.add(newCentralizedPlan);
					}
				}
			}
		}
		return newPlanList;
	}
	private CentralizedPlan chooseBestPlan(List<CentralizedPlan> newPLanList){
		CentralizedPlan cheapest = null; 
		double minCost = Integer.MAX_VALUE;
		for(CentralizedPlan plan : newPLanList){
			double cost = plan.planCost();
			if(cost < minCost){
				cheapest = plan;
				minCost = cost;
			}
		}
		return cheapest;
	}
	
	public CentralizedPlan getNewPlan(Task task){
		List<CentralizedPlan> newPlanList = insertTask(task, bestPlan);
		actualPlan = chooseBestPlan(newPlanList);
		return actualPlan;
	}
	
	public CentralizedPlan getFinalPlan(TaskSet tasks, List<AuctionVehicle> vehicles){
		CentralizedPlan finalPlan = new CentralizedPlan(vehicles);
		for(Task t : tasks){
			List<CentralizedPlan> newPlanList = insertTask(t, finalPlan);
			finalPlan = chooseBestPlan(newPlanList);
		}
		return finalPlan;
	}
	
	public CentralizedPlan getBestPlan() {
		return bestPlan;
	}
	public void updatePlan(){
		bestPlan = actualPlan;
	}
	
	public int getVehicle(Task task){
		for(Entry<Integer, LinkedList<AuctionTask>> entry : actualPlan.planTasks.entrySet()){
			LinkedList<AuctionTask> taskList = entry.getValue();
			for (AuctionTask t : taskList){
				if (t.task.equals(task)) return entry.getKey();
			}
		}
		return -1;
	}
}