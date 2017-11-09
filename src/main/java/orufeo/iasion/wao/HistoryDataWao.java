package orufeo.iasion.wao;

import orufeo.iasion.data.dto.CryptoPair;

public interface HistoryDataWao {

	CryptoPair getHistoHour(String currency, String quoteCurrency, int aggregate, String exchange);
	
}
