package orufeo.iasion.wao;

import java.io.IOException;
import java.util.List;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexMarginInfosResponse;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.service.account.AccountService;

public interface BitfinexWao {

	Exchange createExchange(String apiKey, String secretKey);
	
	AccountService getAccountService(Exchange exchange);
	
	 BitfinexMarginInfosResponse[] marginInfo(AccountService accountService)  throws IOException;
	 
	 List<FundingRecord> fundingHistory(AccountService accountService)  throws IOException;
	
}
