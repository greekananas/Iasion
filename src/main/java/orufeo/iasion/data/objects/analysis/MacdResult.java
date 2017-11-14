package orufeo.iasion.data.objects.analysis;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import orufeo.iasion.data.dto.CryptoPairDto;

@Getter
@Setter
public class MacdResult {

	protected CryptoPairDto data;
	protected List<Double> macd;
	protected List<Double> amacd;
	protected List<Double> delta;
	
	public MacdResult(CryptoPairDto data, List<Double> macd, List<Double> amacd, List<Double> delta) {
		
		this.data = data;
		this.macd = macd;
		this.amacd = amacd;
		this.delta = delta;
		
	}
	
	public MacdResult()
	{
		
	}
}
