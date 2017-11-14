package orufeo.iasion.data.objects.analysis;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MacdTrend extends MacdResult {
	
	private List<String> trend;
	
	public MacdTrend(MacdResult result ) {
		
		super.setData( result.getData());
		super.setMacd(result.getMacd());
		super.setAmacd(result.getAmacd()); 
		super.setDelta(result.getDelta());
		
	}	
	
	public MacdTrend() {
		
	}

}
