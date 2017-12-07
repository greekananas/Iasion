package orufeo.iasion.bo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Setter;
import orufeo.iasion.data.dto.CryptoPairDto;
import orufeo.iasion.data.objects.analysis.MacdResult;
import orufeo.iasion.data.objects.analysis.MacdSettings;
import orufeo.iasion.data.objects.analysis.MacdTrend;
import orufeo.iasion.data.objects.analysis.MacdTrigger;
import orufeo.iasion.data.objects.analysis.MobileAverageData;
import orufeo.iasion.data.objects.mapping.Ohclv;
import orufeo.iasion.utils.MathFunctions;
import orufeo.iasion.wao.HistoryDataWao;
import orufeo.iasion.wao.HistoryDataWaoImpl;

public class MacdBoImpl implements MacdBo {

	@Setter private HistoryDataWao historyDataWao;

	@Override
	public void init() {

		historyDataWao = new HistoryDataWaoImpl();
		historyDataWao.init();

	}

	@Override
	public MacdTrend analyzeTrend(String currency, String quoteCurrency, int aggregate, String exchange, MacdSettings macdSettings) {

		MacdResult result = computeMacd( currency,  quoteCurrency,  aggregate,  exchange,  macdSettings); 

		List<String> trend  = new ArrayList<String>();
		
		for( int i=0; i < result.getMacd().size(); i++  ) {

			if (result.getDelta().get(i) > 0 && result.getDelta().get(i-1) < 0)
				trend.add("long");
			else if (result.getDelta().get(i) < 0 && result.getDelta().get(i-1) > 0)
				trend.add("short");
			else
				trend.add("none");

		}

		MacdTrend trends = new MacdTrend(result);
		trends.setTrend(trend);

		displayTrend(trends);

		return trends;
	}

	@Override
	public MacdTrigger analyzeTrigger(String currency, String quoteCurrency, int aggregate, String exchange, MacdSettings macdSettings, MacdTrend trends) {

		MacdResult result = computeMacd(currency, quoteCurrency, aggregate, exchange, macdSettings); 

		MacdTrigger triggers = new MacdTrigger(result);

		fillTrends(triggers, trends);

		List<String> trigger = new ArrayList<String>();
		trigger.add("none");
		
		for( int i=1; i < result.getMacd().size(); i++  ) {

			if (triggers.getTrend().get(i) != triggers.getTrend().get(i-1))
					trigger.add(triggers.getTrend().get(i) );
			else if (triggers.getTrend().get(i).equals("long")) {
				if (triggers.getDelta().get(i) > 0 && triggers.getDelta().get(i-1) < 0 )
					trigger.add("prise long");
				else if (triggers.getDelta().get(i) < 0 && triggers.getDelta().get(i-1) > 0 )
					trigger.add("cloture long");
				else 
					trigger.add("none");
			} else if (triggers.getTrend().get(i).equals("short")) {
				if (triggers.getDelta().get(i) < 0 && triggers.getDelta().get(i-1) > 0 )
					trigger.add("prise short");
				else if (triggers.getDelta().get(i) > 0 && triggers.getDelta().get(i-1) < 0 )
					trigger.add("cloture short");
				else
					trigger.add("none");
			} else
					trigger.add("none");

		}

		triggers.setTrigger(trigger);

		displayTrigger(triggers);

		return triggers;
	}





	/******************
	 * 
	 * 
	 *  PRIVATE METHODS
	 * 
	 * 
	 ******************/

	private void fillTrends(MacdTrigger triggers, MacdTrend trends) {

		List<String> simplifiedTrends = new ArrayList<String>();

		int firstValueIndex = 0;

		for (int i=0; i< trends.getTrend().size(); i++) {
			
			if ( firstValueIndex==0) {
				if (trends.getTrend().get(i).equals("none"))
					simplifiedTrends.add("n/a");
				else {
					simplifiedTrends.add(trends.getTrend().get(i));
					firstValueIndex = i;
				}
			} else {

				if (!trends.getTrend().get(i).equals("none"))
					simplifiedTrends.add(trends.getTrend().get(i));
				else
					simplifiedTrends.add(simplifiedTrends.get(i-1));
			}
			
		}

		triggers.setTrend(Arrays.asList(new String[triggers.getMacd().size()]));		

		for (int j = 0; j < triggers.getMacd().size(); j++) {

			Long triggerTime = triggers.getData().getData().get(j).getTime();

			for (int i = 0; i < trends.getMacd().size(); i++) {

				String trendValue = simplifiedTrends.get(i);
				Long trendTime = trends.getData().getData().get(i).getTime();

				if (triggerTime >= trendTime) {
					if (!trendValue.equals("none")  ) {
						triggers.getTrend().set(j, trendValue);
					}

				} else 
					break;

			}

		}


	}

	private MacdResult computeMacd(String currency, String quoteCurrency, int aggregate, String exchange, MacdSettings macdSettings) {

		CryptoPairDto data = historyDataWao.getHistoHour(currency, quoteCurrency, aggregate, exchange);

		//exponential moving average computation
		MobileAverageData slowEma = initMobileAverageData(data, macdSettings.getSlowLength());
		MobileAverageData fastEma = initMobileAverageData(data, macdSettings.getFastLength());

		List<Double> macd = new ArrayList<Double>();

		for( int i=0; i < slowEma.getTimes().size(); i++  ) {

			macd.add(fastEma.getEma().get(i)-slowEma.getEma().get(i));

		}

		List<Double> amacd = new ArrayList<Double>();

		for( int i=0; i < macd.size(); i++  ) {

			if (i==0)
				amacd.add(macd.get(i));	
			else
				amacd.add(MathFunctions.exponentialMobileAverage(macd.get(i), macdSettings.getMACDLength(),amacd.get(i-1)));

		}

		List<Double> delta = new ArrayList<Double>();

		for( int i=0; i < macd.size(); i++  ) {

			delta.add(macd.get(i)-amacd.get(i));

		}

		MacdResult result = new MacdResult(data, macd, amacd, delta);

		return result;

	}

	private List<Double> getInitValues(CryptoPairDto data, Integer length, Integer startIndex) {

		List<Double> values = new ArrayList<Double>();		
		for ( Ohclv d : data.getData().subList(startIndex, startIndex+length)) {

			values.add(d.getClose());

		}

		return values;
	}

	private MobileAverageData initMobileAverageData(CryptoPairDto data, Integer length) {

		MobileAverageData ma = new MobileAverageData();

		int index = 0;

		for (Ohclv d : data.getData()) {

			ma.getTimes().add(d.getTime());
			ma.getClose().add(d.getClose());

			if (index < length) 
				ma.getSma().add(0d);
			else
				ma.getSma().add(MathFunctions.mean(getInitValues(data, length, index-length)));

			if (index==0)
				ma.getEma().add(d.getClose());	
			else
				ma.getEma().add(MathFunctions.exponentialMobileAverage(d.getClose(), length, ma.getEma().get(index-1)));

			index++;
		}

		return ma;
	}

	private void displayTrend(MacdTrend matrix) {

		for (int i=0; i< matrix.getMacd().size(); i++) {

			System.out.println(matrix.getData().getData().get(i).getTime() +","+ matrix.getData().getData().get(i).getClose()+","+matrix.getMacd().get(i)+","+matrix.getAmacd().get(i)+","+matrix.getDelta().get(i)+","+matrix.getTrend().get(i));

		}

	}

	private void displayTrigger(MacdTrigger matrix) {

		for (int i=0; i< matrix.getMacd().size(); i++) {
			try {
				System.out.println(matrix.getData().getData().get(i).getTime() +","+ matrix.getData().getData().get(i).getClose()+","+matrix.getMacd().get(i)+","+matrix.getAmacd().get(i)+","+matrix.getDelta().get(i)+","+matrix.getTrend().get(i)+","+matrix.getTrigger().get(i));
			
			} catch (Exception e) {
				System.out.println("Erreur: "+i+" - "+e.getMessage());
			}
		}

	}

}
