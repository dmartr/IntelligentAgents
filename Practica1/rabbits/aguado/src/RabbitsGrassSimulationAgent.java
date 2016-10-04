import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.gui.SimGraphics;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	// We define the variables each agent has
	private int x;
	private int y;
	private int direction;
	private double energy;
	private int birthThreshold;
	private static int IDNumber = 0;
	private int ID;
	
	private RabbitsGrassSimulationSpace rgSpace;
	Image img = null;
	
	public RabbitsGrassSimulationAgent(int birthThreshold, double initEnergy){
		x = -1;
	    y = -1;
	    setDirection();
	    energy = initEnergy;
	    this.birthThreshold = birthThreshold;
	    IDNumber++;
	    ID = IDNumber;    
	    
	    // Our bunnies will be represented by a .png 
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		InputStream input = classLoader.getResourceAsStream("bunny.png");
	    try {
	    	img = ImageIO.read(input);
		} catch (IOException e) {
		}
	}
	
	public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace space) {
		rgSpace = space;
	}
	
	// North = 1; East = 2; South = 3; West = 4
	private void setDirection(){
	    direction = (int)Math.floor(Math.random() * 4) + 1;
	}
	
	public void setXY(int newX, int newY){
	    x = newX;
	    y = newY;
	}
	
	public String getID(){
		return "A-" + ID;
	}

	public double getEnergy(){
		return energy;
	}
		  
	public void report(){
		System.out.println(getID() + " at " + x + ", " + y + " has " + getEnergy() + " energy");
	}


	public void draw(SimGraphics G) {
		// TODO Auto-generated method stub
		//G.drawFastRoundRect(Color.blue);
		G.drawImageToFit(img);

	}

	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}
	
	//Returns: 0 if there hasn't been a reproduction || The energy of the new born agent
	public double step(){
		int newX = 0;
		int newY = 0;
		
		// The coordinates change depending on the new direction
		switch (direction) {
        	case 1: newX = x;
        			newY = y+1;
                 	break;
        	case 2: newX = x+1;
		        	newY = y;
		        	break;
        	case 3: newX = x;
		        	newY = y-1;
		        	break;
        	case 4: newX = x-1;
		        	newY = y;
		        	break;
		}
		
		Object2DGrid grid = rgSpace.getCurrentAgentSpace();
	    newX = (newX + grid.getSizeX()) % grid.getSizeX();
	    newY = (newY + grid.getSizeY()) % grid.getSizeY();
	    
	    // In case the move is valid, the agent takes the energy in that space
	    if (tryMove(newX, newY)) {
			energy += rgSpace.takeEnergyAt(x, y);
	    }
	    
	    setDirection();

	    // If the agent is able to reproduce, it loses half of its energy
	    // The new born agent has the same amount of energy as the father (the other half)
	    // If it can't reproduce, it loses 0.5 energy per step
	    if (tryReproduce()) {
	    	energy /= 2;
	    	if (energy < 0.5) energy = 0;
	    	return energy;
	    } else {
	    	energy -= 0.5;
	    	return 0;
	    }
	}
	
	// Returns: true if the move is valid || false if there's already an agent at that position
	private boolean tryMove(int newX, int newY){
        RabbitsGrassSimulationAgent rgsa = rgSpace.getAgentAt(newX, newY);
        if (rgsa!= null){
        	return false;
        } else {
        	return rgSpace.moveAgentAt(x, y, newX, newY);
        }
	}
	
	// If the energy of the agent is above the Birth Threshold, return true
	private boolean tryReproduce() {
		if (energy > birthThreshold) {
			return true;
		} else {
			return false;
		}
	}
}
