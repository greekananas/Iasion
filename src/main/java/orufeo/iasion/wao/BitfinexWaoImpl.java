package orufeo.iasion.wao;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bitfinex.v1.BitfinexExchange;
import org.knowm.xchange.bitfinex.v1.BitfinexOrderType;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexMarginInfosResponse;
import org.knowm.xchange.bitfinex.v1.service.BitfinexAccountServiceRaw;
import org.knowm.xchange.bitfinex.v1.service.BitfinexTradeServiceRaw;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.trade.params.TradeHistoryParamCurrency;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.TradeHistoryParamsTimeSpan;

public class BitfinexWaoImpl implements BitfinexWao {

	@Override
	public Exchange createExchange(String apiKey, String secretKey) {
	
		    // Use the factory to get BFX exchange API using default settings
		    Exchange bfx = ExchangeFactory.INSTANCE.createExchange(BitfinexExchange.class.getName());

		    ExchangeSpecification bfxSpec = bfx.getDefaultExchangeSpecification();

		    bfxSpec.setApiKey(apiKey);
		    bfxSpec.setSecretKey(secretKey);

		    bfx.applySpecification(bfxSpec);

		    return bfx;
		  
	}

	@Override
	public AccountService getAccountService(Exchange exchange) {
		
		return exchange.getAccountService();
	}

	@Override
	public BitfinexMarginInfosResponse[] marginInfo(AccountService accountService)  throws IOException {
				
		// Get the margin information
	    BitfinexAccountServiceRaw accountServiceRaw = (BitfinexAccountServiceRaw) accountService;
	    return accountServiceRaw.getBitfinexMarginInfos();
	}

	@Override
	public List<FundingRecord> fundingHistory(AccountService accountService)  throws IOException {
		 // Get the funds information
	    TradeHistoryParams params = accountService.createFundingHistoryParams();
	    if (params instanceof TradeHistoryParamsTimeSpan) {
	      final TradeHistoryParamsTimeSpan timeSpanParam = (TradeHistoryParamsTimeSpan) params;
	      timeSpanParam.setStartTime(new Date(System.currentTimeMillis() - (1 * 12 * 30 * 24 * 60 * 60 * 1000L)));
	    }
	    if (params instanceof TradeHistoryParamCurrency) {
	      ((TradeHistoryParamCurrency) params).setCurrency(Currency.BTC);
	    }

	    return accountService.getFundingHistory(params);
	}

}
