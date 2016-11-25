package template;

/**
 * Interface to define multiple heuristics
 * @author Ignacio Aguado, Darío Martínez
 */
interface Heuristic {
	   public double getG(DeliberativeState state);
	   public double getH(DeliberativeState state);
	   public double getF(DeliberativeState state);
}
