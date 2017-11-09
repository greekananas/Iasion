package orufeo.iasion.utils;

import java.util.List;

public interface MathFunctions {

	Double mean(List<Double> values); 
	
	Double exponentialMobileAverage(Double close, Integer length, Double previousEma);
	
}
