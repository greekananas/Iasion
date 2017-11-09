package orufeo.iasion.utils;

import java.util.List;

public class MathFunctionsImpl implements MathFunctions {

	@Override
	public Double mean(List<Double> values) {
		
		Integer length = values.size();
		
		Double total = 0d;
		
		for (Double d : values) {
			
			total += d;
			
		}
		
		return total / length.doubleValue();
	}

	@Override
	public Double exponentialMobileAverage(Double close, Integer length, Double previousEma) {
		
			return close*(2/(length.doubleValue()+1))+previousEma*(1-2/(length.doubleValue()+1));
		
	}

}
