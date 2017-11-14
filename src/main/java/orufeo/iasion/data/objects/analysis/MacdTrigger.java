package orufeo.iasion.data.objects.analysis;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MacdTrigger extends MacdTrend {

	private List<String> trigger;

	public MacdTrigger(MacdTrend trends) {

		super.setData( trends.getData());
		super.setMacd(trends.getMacd());
		super.setAmacd(trends.getAmacd()); 
		super.setDelta(trends.getDelta());
		super.setTrend(trends.getTrend());

	}	

	public MacdTrigger(MacdResult result) {

		super.setData( result.getData());
		super.setMacd(result.getMacd());
		super.setAmacd(result.getAmacd()); 
		super.setDelta(result.getDelta());

	}	

}
