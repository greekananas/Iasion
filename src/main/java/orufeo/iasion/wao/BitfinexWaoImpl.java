package orufeo.iasion.wao;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Setter;
import orufeo.iasion.data.dto.BitFinexOrderStatus;
import orufeo.iasion.data.dto.BitFinexPosition;


public class BitfinexWaoImpl implements BitfinexWao {

	@Setter  private String PROTOCOL; // "https://";
	@Setter  private String DOMAIN;   // "api.bitfinex.com";
	@Setter private String ALGORITHM_HMACSHA384; // "HmacSHA384"	
	@Setter	private ObjectMapper mapper;

	private static Logger log = Logger.getLogger(BitfinexWaoImpl.class);

	@Override
	public List<BitFinexPosition> getActivePositions(String apiKey, String secretKey) throws Exception {

		long nonce = System.currentTimeMillis();
		String path = "/v1/positions";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));

		String payload = jo.toString();

		//API CALL
		String response = httpCallBitFinex(PROTOCOL, DOMAIN, path, "POST", apiKey, secretKey, payload);

		return mapper.readValue(response, mapper.getTypeFactory().constructCollectionType(List.class, BitFinexPosition.class));

	}

	@Override
	public BitFinexOrderStatus closePosition(BitFinexPosition position, String apiKey, String secretKey) throws Exception {

		long nonce = System.currentTimeMillis();
		String path = "/v1/order/new";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));
		jo.put("symbol", position.getSymbol().toUpperCase());
		jo.put("amount", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"); //TODO
		jo.put("price",  "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"); //TODO
		jo.put("exchange", "bitfinex");
		jo.put("side", "sell");
		jo.put("type",   "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"); //TODO

		String payload = jo.toString();

		String response = httpCallBitFinex(PROTOCOL, DOMAIN, "/v1/order/new", "POST", apiKey, secretKey, payload);

		return mapper.readValue(response, BitFinexOrderStatus.class);
	}

	@Override
	public BitFinexOrderStatus orderStatus(Long orderId, String apiKey, String secretKey) throws Exception {

		long nonce = System.currentTimeMillis();
		String path = "/v1/order/new";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));
		jo.put("order_id", Long.toString(orderId));
		
		String payload = jo.toString();
		
		String response = httpCallBitFinex(PROTOCOL, DOMAIN, "/v1/order/status", "POST", apiKey, secretKey, payload);

		return mapper.readValue(response, BitFinexOrderStatus.class);
	}


	/********************
	 * 
	 * 
	 *  PRIVATE METHODS
	 * 
	 * 	
	 *******************/

	private String httpCallBitFinex(String protocol, String domain, String path, String method, String apiKey, String secretKey, String payload ) throws IOException {
		String sResponse;

		HttpURLConnection conn = null;

		//String protocol = "https://";
		//String domain ="api.bitfinex.com";

		//String path = "/v1/balances";
		// String method = "GET";
		//String method = "POST";

		try {
			URL url = new URL(protocol+domain+ path);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(method);

			if (null==apiKey) {
				String msg = "Authenticated access not possible, because key and secret was not initialized: use right constructor.";
				return msg;
			}

			conn.setDoOutput(true);
			conn.setDoInput(true);

			// this is usage for Base64 Implementation in Android. For pure java you can use java.util.Base64.Encoder
			// Base64.NO_WRAP: Base64-string have to be as one line string
			// String payload_base64 = Base64.encodeToString(payload.getBytes(), Base64.NO_WRAP);

			Encoder encoder = Base64.getEncoder() ;
			String payload_base64 = encoder.encodeToString(payload.getBytes());

			String payload_sha384hmac = hmacDigest(payload_base64, secretKey, ALGORITHM_HMACSHA384);

			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
			conn.addRequestProperty("X-BFX-APIKEY", apiKey);
			conn.addRequestProperty("X-BFX-PAYLOAD", payload_base64);
			conn.addRequestProperty("X-BFX-SIGNATURE", payload_sha384hmac);

			// read the response
			InputStream in = new BufferedInputStream(conn.getInputStream());
			return convertStreamToString(in);

		} catch (MalformedURLException e) {
			throw new IOException(e.getClass().getName(), e);
		} catch (ProtocolException e) {
			throw new IOException(e.getClass().getName(), e);
		} catch (IOException e) {

			String errMsg = e.getLocalizedMessage();

			if (conn != null) {
				try {
					sResponse = convertStreamToString(conn.getErrorStream());
					errMsg += " -> " + sResponse;
					log.error(errMsg, e);
					return sResponse;
				} catch (IOException e1) {
					errMsg += " Error on reading error-stream. -> " + e1.getLocalizedMessage();
					log.error(errMsg, e);
					throw new IOException(e.getClass().getName(), e1);
				}
			} else {
				throw new IOException(e.getClass().getName(), e);
			}
		} catch (JSONException e) {
			String msg = "Error on setting up the connection to server";
			throw new IOException(msg, e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private String convertStreamToString(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}


	private  String hmacDigest(String msg, String keyString, String algo) {
		String digest = null;
		try {
			SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), algo);
			Mac mac = Mac.getInstance(algo);
			mac.init(key);

			byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));

			StringBuffer hash = new StringBuffer();
			for (int i = 0; i < bytes.length; i++) {
				String hex = Integer.toHexString(0xFF & bytes[i]);
				if (hex.length() == 1) {
					hash.append('0');
				}
				hash.append(hex);
			}
			digest = hash.toString();
		} catch (UnsupportedEncodingException e) {
			log.error(e);
		} catch (InvalidKeyException e) {
			log.error(e);
		} catch (NoSuchAlgorithmException e) {
			log.error(e);
		}
		return digest;
	}


	/*
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
	 */
}
