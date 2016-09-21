import java.awt.Color;
import java.util.ArrayList;
import java.util.Hashtable;

import uchicago.src.reflector.DescriptorContainer;
import uchicago.src.reflector.RangePropertyDescriptor;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.event.SliderListener;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.util.SimUtilities;
/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl implements DescriptorContainer{		
	
	
	  private Hashtable descriptors = new Hashtable();

	  public Hashtable getParameterDescriptors() {
	    return descriptors;
	  } 
	  
		// Default values
		private static final int XSIZE = 20;
		private static final int YSIZE = 20;
		private static final int NRABBITS = 10;
		private static final int BIRTHTHRESHOLD = 20;
		private static final int GRASSGROWTH = 5;
		private static final int GRASSENERGY = 5;
		
		private int xSize = XSIZE;
		private int ySize = YSIZE;
		private int nRabbits = NRABBITS;
		private int birthThreshold=BIRTHTHRESHOLD;
		private int grassGrowth = GRASSGROWTH;
		private int grassEnergy = GRASSENERGY;

		private Schedule schedule;

		private RabbitsGrassSimulationSpace rgSpace;
		private ArrayList agentList;
		private DisplaySurface displaySurf;

		public static void main(String[] args) {
			SimInit init = new SimInit();
		    RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();

		    init.loadModel(model, "", false);
		}
		
		private OpenSequenceGraph amountOfGrassInSpace;
		
		class grassInSpace implements DataSource, Sequence {
			public Object execute() {
				return new Double(getSValue());   
			}		
			public double getSValue() {
				return (double) rgSpace.getTotalGrass();
			}
		}
		class agentsInSpace implements DataSource, Sequence {
			public Object execute() {
				return new Double(getSValue());   
			}		
			public double getSValue() {
				return (double) rgSpace.getTotalAgents();
			}
		}
		
		public void begin() {
			// TODO Auto-generated method stub
			  buildModel();
			  buildSchedule();
			  buildDisplay();

			  displaySurf.display();
			  amountOfGrassInSpace.display();
		}
		
		public void buildModel(){
			System.out.println("Building model");
			rgSpace = new RabbitsGrassSimulationSpace(xSize, ySize);
		    rgSpace.spreadGrass(grassGrowth);
			for(int i = 0; i < nRabbits; i++){
			      addNewAgent();
			}
			for(int i = 0; i < agentList.size(); i++){
				RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
			    rgsa.report();
			 }
		}
		
		public void buildSchedule(){
			System.out.println("Building schedule");

		    class RabbitsGrassSimulationStep extends BasicAction {
		      public void execute() {
		        SimUtilities.shuffle(agentList);
		        for(int i =0; i < agentList.size(); i++){
		          RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
		          boolean reproduce = rgsa.step();
		          if (reproduce) {
		        	  addNewAgent();
		          }
		        }
		        removeDeadAgents();
			    rgSpace.spreadGrass(grassGrowth);
		        displaySurf.updateDisplay();
		      }
		    }

		    schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());
		    
		    class RabbitsGrassSimulationCountLiving extends BasicAction {
		    	public void execute(){
		        	countLivingAgents();
		        }
		    }

		      
		    schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationCountLiving());
		    
		    class RabbitsGrassSimulationUpdateGrassInSpace extends BasicAction {
		        public void execute(){
		          amountOfGrassInSpace.step();
		        }
		      }

		      schedule.scheduleActionAtInterval(1, new RabbitsGrassSimulationUpdateGrassInSpace());
		}
		
		public void buildDisplay(){
			System.out.println("Building display");
		    ColorMap map = new ColorMap();

		    for(int i = 1; i<16; i++){
		    	map.mapColor(i, new Color(0, (int)(i * 8 + 127), 0));
		    }
		    map.mapColor(0, Color.white);

		    Value2DDisplay displayGrass = new Value2DDisplay(rgSpace.getCurrentGrassSpace(), map);

		    Object2DDisplay displayAgents = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
		    displayAgents.setObjectList(agentList);

		    displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		    displaySurf.addDisplayableProbeable(displayAgents, "Agents");
		    
		    amountOfGrassInSpace.addSequence("Grass In Space", new grassInSpace());
		    amountOfGrassInSpace.addSequence("Rabbits In Space", new agentsInSpace());
		}
		
		public String[] getInitParam() {
			// TODO Auto-generated method stub
			String[] initParams = { "XSize", "YSize", "NRabbits", "BirthThreshold","GrassGrowth", "GrassEnergy"};
		    return initParams;
		}

		public String getName() {
			// TODO Auto-generated method stub
			return "Rabbits Grass Simulation";
		}
		
		private void addNewAgent(){
		    RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(birthThreshold);
		    agentList.add(a);
		    rgSpace.addAgent(a);
		}
		
		private void removeDeadAgents(){

			for(int i = (agentList.size() - 1); i >= 0 ; i--){
				RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);  
				if(rgsa.getEnergy() <= 0){
			        rgSpace.removeAgentAt(rgsa.getX(), rgsa.getY());
			        agentList.remove(i);
				}
			}
		}
		
		private int countLivingAgents(){
			    
			int livingAgents = 0;
			for(int i = 0; i < agentList.size(); i++){
				RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
				if(rgsa.getEnergy() > 0) livingAgents++;
			}
			System.out.println("Number of living agents is: " + livingAgents);
			return livingAgents;
			 
		}

		public Schedule getSchedule() {
			// TODO Auto-generated method stub
			return schedule;
		}

		public void setup() {
			// TODO Auto-generated method stub
			System.out.println("Setting up");
		
			modelManipulator = getModelManipulator();
			modelManipulator.init();
			modelManipulator.setEnabled(true);

			BirthSlider birth = new BirthSlider();
			birth.setFirstVal(birthThreshold);
			modelManipulator.addSlider("BirthThreshold", 0, 20, 5, birth);

			GrassSlider grass = new GrassSlider();
			grass.setFirstVal(grassGrowth);
			modelManipulator.addSlider("GrassGrowth", 0, 200, 20, grass);
			
			GrassEnergySlider energy = new GrassEnergySlider();
			energy.setFirstVal(grassEnergy);
			modelManipulator.addSlider("GrassEnergy", 0, 20, 5, energy);

		    if (amountOfGrassInSpace != null){
		      amountOfGrassInSpace.dispose();
		    }
		    amountOfGrassInSpace = null;
		    // Create Displays
		    displaySurf = new DisplaySurface(this, "Rabbit Grass Simulation Model Window 2");
		    amountOfGrassInSpace = new OpenSequenceGraph("Amount Of Grass In Space",this);

		    // Register Displays
		    registerDisplaySurface("Rabbit Grass Simulation Model Window 2", displaySurf);
		    this.registerMediaProducer("Plot", amountOfGrassInSpace);
		    
		    
			rgSpace = null;
			agentList = new ArrayList();
			schedule = new Schedule(1);
			if (displaySurf != null){
				displaySurf.dispose();
			}
			displaySurf = null;

			displaySurf = new DisplaySurface(this, "Rabbit Grass Simulation Model Window 1");
			registerDisplaySurface("Rabbit Grass Simulation Model Window 1", displaySurf);
		}
		

		public class BirthSlider extends SliderListener {
			public void execute() {
				birthThreshold = value;			  
			}
		}
		public class GrassSlider extends SliderListener {
			public void execute() {
				grassGrowth = value;			  
			}
		}
		public class GrassEnergySlider extends SliderListener {
			public void execute() {
				grassEnergy = value;
				if (rgSpace != null) rgSpace.setGrassEnergy(value);
			}
		}

		public int getXSize() {
			return xSize;
		}
		
		public void setXSize(int x) {
			xSize = x;
		}
		
		public int getYSize() {
			return ySize;
		}
		
		public void setYSize(int y) {
			ySize = y;
		}
		
		public int getNRabbits(){
			return nRabbits;
		}
		
		public void setNRabbits(int rabbits) {
			nRabbits = rabbits;
		}
		
		public int getBirthThreshold() {
			return birthThreshold;
		}
		
		public void setBirthThreshold(int threshold) {
			birthThreshold = threshold;
		}
		
		public int getGrassGrowth() {
			return grassGrowth;
		}
		
		public void setGrassGrowth(int rate) {
			grassGrowth = rate;
		}		
		
		public int getGrassEnergy() {
			return grassEnergy;
		}
		
		public void setGrassEnergy(int energy) {
			grassEnergy = energy;
		}	
}
