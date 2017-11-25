package orufeo.iasion.wao;

import java.util.List;

import orufeo.iasion.data.dto.BitFinexOrderStatus;
import orufeo.iasion.data.dto.BitFinexPosition;

public interface BitfinexWao {
/*
	Exchange createExchange(String apiKey, String secretKey);
	
	AccountService getAccountService(Exchange exchange);
	
	BitfinexMarginInfosResponse[] marginInfo(AccountService accountService)  throws IOException;
	 
	List<FundingRecord> fundingHistory(AccountService accountService)  throws IOException;
*/
	
	List<BitFinexPosition> getActivePositions(String apiKey, String secretKey) throws Exception;
	
	BitFinexOrderStatus closePosition(BitFinexPosition position, String apiKey, String secretKey) throws Exception;
	
	BitFinexOrderStatus orderStatus(Long orderId, String apiKey, String secretKey) throws Exception;
	
}
