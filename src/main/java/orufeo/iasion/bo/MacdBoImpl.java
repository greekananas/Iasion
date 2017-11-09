package orufeo.iasion.bo;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import orufeo.iasion.data.dto.CryptoPair;
import orufeo.iasion.data.objects.MacdSettings;
import orufeo.iasion.data.objects.MobileAverageData;
import orufeo.iasion.data.objects.Ohclv;
import orufeo.iasion.data.objects.Order;
import orufeo.iasion.data.objects.WalletSettings;
import orufeo.iasion.utils.MathFunctions;
import orufeo.iasion.utils.MathFunctionsImpl;
import orufeo.iasion.wao.HistoryDataWao;
import orufeo.iasion.wao.HistoryDataWaoImpl;

public class MacdBoImpl implements MacdBo {

	@Setter private HistoryDataWao historyDataWao;
	@Setter private MathFunctions mathFunctions;

	
	@Override
	public void init() {
		
		historyDataWao = new HistoryDataWaoImpl();
		historyDataWao.init();
		mathFunctions = new MathFunctionsImpl();
		
	}
	
	@Override
	public Order analyseCryptoPair(String currency, String quoteCurrency, int aggregate, String exchange, MacdSettings macdSettings, WalletSettings walletSettings) {

		CryptoPair data = historyDataWao.getHistoHour(currency, quoteCurrency, aggregate, exchange);

		//simple average SMA of the n first lines to initiate the EMA
		/*	List<Double> slowValues = getInitValues(data, macdSettings.getSlowLength(),0);
		List<Double> fastValues = getInitValues(data, macdSettings.getFastLength(),0);

		Double meanInitSlow = mathFunctions.mean(slowValues);
		Double meanInitFast = mathFunctions.mean(fastValues);
		 */	
		
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
				amacd.add(mathFunctions.exponentialMobileAverage(macd.get(i), macdSettings.getMACDLength(),amacd.get(i-1)));

		}

		List<Double> delta = new ArrayList<Double>();

		for( int i=0; i < macd.size(); i++  ) {

			delta.add(macd.get(i)-amacd.get(i));

		}

		List<String> order = new ArrayList<String>();

		for( int i=0; i < macd.size(); i++  ) {

			if (delta.get(i) > 0 && delta.get(i-1) < 0)
				order.add("long");
			else if (delta.get(i) < 0 && delta.get(i-1) > 0)
					order.add("short");
				else
					order.add("none");

		}

		display(data, macd, amacd, delta, order);
		
		return null;
	}


	/******************
	 * 
	 * 
	 *  PRIVATE METHODS
	 * 
	 * 
	 ******************/

	private List<Double> getInitValues(CryptoPair data, Integer length, Integer startIndex) {
		
		List<Double> values = new ArrayList<Double>();		
		for ( Ohclv d : data.getData().subList(startIndex, startIndex+length)) {

			values.add(d.getClose());

		}

		return values;
	}

	private MobileAverageData initMobileAverageData(CryptoPair data, Integer length) {

		MobileAverageData ma = new MobileAverageData();

		int index = 0;

		for (Ohclv d : data.getData()) {
					
			ma.getTimes().add(d.getTime());
			ma.getClose().add(d.getClose());
			
			if (index < length) 
				ma.getSma().add(0d);
			else
				ma.getSma().add(mathFunctions.mean(getInitValues(data, length, index-length)));
			
			if (index==0)
				ma.getEma().add(d.getClose());	
			else
				ma.getEma().add(mathFunctions.exponentialMobileAverage(d.getClose(), length, ma.getEma().get(index-1)));

			index++;
		}

		return ma;
	}

	private void display(CryptoPair data, List<Double> macd,List<Double> amacd,List<Double> delta,List<String> order) {
		
		for (int i=0; i< macd.size(); i++) {
			
			System.out.println(data.getData().get(i).getTime() +","+ data.getData().get(i).getClose()+","+macd.get(i)+","+amacd.get(i)+","+delta.get(i)+","+order.get(i));
			
		}
		
	}
	
}
