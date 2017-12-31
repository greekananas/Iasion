package orufeo.iasion.wao;

import java.util.List;

import orufeo.iasion.data.dto.BitFinexBalanceStatus;
import orufeo.iasion.data.dto.BitFinexOrderStatus;
import orufeo.iasion.data.dto.BitFinexPosition;
import orufeo.iasion.data.dto.BitFinexTicker;
import orufeo.iasion.data.dto.BitFinexTransferStatus;
import orufeo.iasion.exception.ActivePositionsException;
import orufeo.iasion.exception.BalancesException;
import orufeo.iasion.exception.BuyException;
import orufeo.iasion.exception.CancelOrderException;
import orufeo.iasion.exception.ClosePositionException;
import orufeo.iasion.exception.OrderStatusException;
import orufeo.iasion.exception.SellException;
import orufeo.iasion.exception.TransferException;

public interface BitfinexWao {
	
	List<BitFinexPosition> getActivePositions(String apiKey, String secretKey) throws ActivePositionsException;
	
	BitFinexOrderStatus closePosition(BitFinexPosition position, String apiKey, String secretKey) throws ClosePositionException;
	
	BitFinexOrderStatus orderStatus(Long orderId, String apiKey, String secretKey) throws OrderStatusException;
	
	BitFinexOrderStatus cancelOrder(Long orderId, String apiKey, String secretKey) throws CancelOrderException;
	
	List<BitFinexTransferStatus> transfer(String from, String to, String currencyLabel, Double currencyAmount, String apiKey, String secretKey) throws TransferException;
	
	List<BitFinexBalanceStatus> getBalances(String apiKey, String secretKey) throws BalancesException;
	
	BitFinexOrderStatus buy(String symbol, String amount, String price, String type, String apiKey, String secretKey) throws BuyException;
	
	BitFinexOrderStatus sell(String symbol, String amount, String price, String type, String apiKey, String secretKey) throws SellException;
	
	BitFinexTicker getTicker(String symbol);
	
}
