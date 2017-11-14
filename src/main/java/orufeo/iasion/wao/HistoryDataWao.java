package orufeo.iasion.wao;

import orufeo.iasion.data.dto.CryptoPairDto;

public interface HistoryDataWao {

	CryptoPairDto getHistoHour(String currency, String quoteCurrency, int aggregate, String exchange);
	
	void init();
	
}
