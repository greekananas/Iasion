package orufeo.iasion.data.objects;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MobileAverageData {

	private List<Long> times = new ArrayList<Long>();
	private List<Double> close = new ArrayList<Double>();
	private List<Double> ema = new ArrayList<Double>();
	private List<Double> sma = new ArrayList<Double>();
	
}
