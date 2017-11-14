package orufeo.iasion.wao;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import lombok.Setter;
import orufeo.iasion.data.dto.CryptoPairDto;

public class HistoryDataWaoImpl implements HistoryDataWao {

	@Setter private RestTemplate restTemplate;
	@Setter	private ObjectMapper mapper;
		
	private static Logger log = Logger.getLogger(HistoryDataWaoImpl.class);
	
	@Override
	public void init() {
		restTemplate = new RestTemplate();
		mapper =  new ObjectMapper();	
	}
	
	@Override
	public CryptoPairDto getHistoHour(String currency, String quoteCurrency, int aggregate, String exchange) {

		log.error(" getHistoHour");
		
		String url = "https://min-api.cryptocompare.com/data/histohour?fsym="+currency.toUpperCase()+"&tsym="+quoteCurrency.toUpperCase()+"&aggregate="+aggregate+"&e="+exchange;
		
		log.error(" url="+url);
		
		CryptoPairDto dto = null;
		
		try {
	
		  	String forObject = restTemplate.getForObject(url, String.class);
				  	
		    dto = mapper.readValue(forObject, CryptoPairDto.class);
		 		
		} catch (Exception e) {
	 		log.error("Problem getHistoHour:",e);
	 	
	 	} 
		
		return dto;
	}


}
