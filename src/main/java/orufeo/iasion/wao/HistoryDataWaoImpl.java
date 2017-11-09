package orufeo.iasion.wao;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import lombok.Setter;
import orufeo.iasion.data.dto.CryptoPair;

public class HistoryDataWaoImpl implements HistoryDataWao {

	@Setter private RestTemplate restTemplate;
	@Setter	private ObjectMapper mapper;
		
	private static Logger log = Logger.getLogger(HistoryDataWaoImpl.class);
	
	@Override
	public CryptoPair getHistoHour(String currency, String quoteCurrency, int aggregate, String exchange) {

		String url = "https://min-api.cryptocompare.com/data/histohour?fsym="+currency.toUpperCase()+"&tsym="+quoteCurrency.toUpperCase()+"&aggregate="+aggregate+"&e="+exchange;
		
		 CryptoPair dto = null;
		
		try {
	
		  	String forObject = restTemplate.getForObject(url, String.class);
				  	
		    dto = mapper.readValue(forObject, CryptoPair.class);
		 		
		} catch (Exception e) {
	 		log.error("Problem getHistoHour:",e);
	 	
	 	} 
		
		return dto;
	}

}
