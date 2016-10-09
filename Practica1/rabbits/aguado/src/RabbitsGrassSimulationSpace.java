import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
	private Object2DGrid grassSpace;
	private Object2DGrid agentSpace;
	
	//Will be updated when the slider changes
	private final int MAXGRASS = 100;
	public RabbitsGrassSimulationSpace(int xSize, int ySize){
		grassSpace = new Object2DGrid(xSize, ySize);
		agentSpace = new Object2DGrid(xSize, ySize);
	    for(int i = 0; i < xSize; i++){
	    	for(int j = 0; j < ySize; j++){
	        grassSpace.putObjectAt(i,j,new Integer(0));
	      }
	    }
	 }
	
	public void spreadGrass(int grass, int grassEnergy){
	    // Randomly place grass
		for(int i = 0; i < grass; i++){

			// Choose coordinates
			int x = (int)(Math.random()*(grassSpace.getSizeX()));
			int y = (int)(Math.random()*(grassSpace.getSizeY()));
			
			int currentValue = getEnergyAt(x,y);
			if (currentValue <= MAXGRASS && currentValue+grassEnergy<=MAXGRASS) {
				// Replace the Integer object with another one with the new value
				grassSpace.putObjectAt(x,y,new Integer(currentValue + grassEnergy));
			} else if (currentValue <= MAXGRASS && currentValue+grassEnergy>MAXGRASS) {
				grassSpace.putObjectAt(x,y,new Integer(MAXGRASS));
			}
	    }
	}
		
	public int getEnergyAt(int x, int y){
	    int i;
	    if(grassSpace.getObjectAt(x,y)!= null){
	      i = ((Integer) grassSpace.getObjectAt(x,y)).intValue();
	    }
	    else{
	      i = 0;
	    }
	    return i;
	}
	
	public RabbitsGrassSimulationAgent getAgentAt(int x, int y){
		RabbitsGrassSimulationAgent retVal = null;
		if(agentSpace.getObjectAt(x, y) != null){
			retVal = (RabbitsGrassSimulationAgent)agentSpace.getObjectAt(x,y);
		}
		return retVal;
	}
	
	public Object2DGrid getCurrentGrassSpace(){
		return grassSpace;
	}
	public Object2DGrid getCurrentAgentSpace(){
	    return agentSpace;
	}
	
	public boolean isCellOccupied(int x, int y){
		boolean retVal = false;
		if(agentSpace.getObjectAt(x, y)!=null) retVal = true;
		return retVal;
	}

	public boolean addAgent(RabbitsGrassSimulationAgent agent){
		boolean retVal = false;
		int count = 0;
		int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();

		while((retVal==false) && (count < countLimit)){
			int x = (int)(Math.random()*(agentSpace.getSizeX()));
			int y = (int)(Math.random()*(agentSpace.getSizeY()));
			if(isCellOccupied(x,y) == false){
				agentSpace.putObjectAt(x,y,agent);
				agent.setXY(x,y);
				agent.setRabbitsGrassSimulationSpace(this);
				retVal = true;
			}
			count++;
		}
		return retVal;
	}
	 
	public void removeAgentAt(int x, int y){    
		agentSpace.putObjectAt(x, y, null);
	}
	public int takeEnergyAt(int x, int y){
	    int energy = getEnergyAt(x, y);
	    grassSpace.putObjectAt(x, y, new Integer(0));
	    return energy;  
	}

	public boolean moveAgentAt(int x, int y, int newX, int newY){
		boolean retVal = false;
	    if(!isCellOccupied(newX, newY)){
	    	RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentSpace.getObjectAt(x, y);
	    	removeAgentAt(x,y);
	    	rgsa.setXY(newX, newY);
	    	agentSpace.putObjectAt(newX, newY, rgsa);
	    	retVal = true;
	    }
	    return retVal;
	}
	

	  
	public int getTotalGrass(){
		int totalGrass = 0;
	    for(int i = 0; i < agentSpace.getSizeX(); i++){
	    	for(int j = 0; j < agentSpace.getSizeY(); j++){
	    		totalGrass += getEnergyAt(i,j);
	    	}
	    }	    
	    return totalGrass;
	}
	
	public int getTotalAgents(){
		int totalAgents = 0;
	    for(int i = 0; i < agentSpace.getSizeX(); i++){
	    	for(int j = 0; j < agentSpace.getSizeY(); j++){
	    		if (isCellOccupied(i, j)) {
	    			totalAgents++;
	    		}
	    	}
	    }	    
	    return totalAgents;
	}
}