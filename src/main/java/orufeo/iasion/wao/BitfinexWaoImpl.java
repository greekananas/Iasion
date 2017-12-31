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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;

import lombok.Setter;
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


public class BitfinexWaoImpl implements BitfinexWao {

	@Setter  private String BITFINEX_PROTOCOL; 				// "https://";
	@Setter  private String BITFINEX_DOMAIN;   				// "api.bitfinex.com";
	@Setter private String BITFINEX_ALGORITHM_HMACSHA384; 	// "HmacSHA384"	
	@Setter	private ObjectMapper mapper;
	@Setter private RestTemplate restTemplate;

	private static Logger log = Logger.getLogger(BitfinexWaoImpl.class);

	@Override
	public List<BitFinexPosition> getActivePositions(String apiKey, String secretKey) throws ActivePositionsException {

		long nonce = System.currentTimeMillis();
		String path = "/v1/positions";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));

		String payload = jo.toString();

		try {
			//API CALL
			String response = httpCallBitFinex(BITFINEX_PROTOCOL, BITFINEX_DOMAIN, path, "POST", apiKey, secretKey, payload);

			return mapper.readValue(response, mapper.getTypeFactory().constructCollectionType(List.class, BitFinexPosition.class));
		} catch (JsonParseException e) {
			throw new ActivePositionsException("JsonParseException: "+e.getMessage());
		} catch (JsonMappingException e) {
			throw new ActivePositionsException("JsonMappingException: "+e.getMessage());
		} catch (IOException e) {
			throw new ActivePositionsException("IOException: "+e.getMessage());
		}

	}

	@Override
	public BitFinexOrderStatus closePosition(BitFinexPosition position, String apiKey, String secretKey) throws ClosePositionException {

		long nonce = System.currentTimeMillis();
		String path = "/v1/order/new";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));
		jo.put("symbol", position.getSymbol().toUpperCase());
		jo.put("amount", position.getAmount()); 
		jo.put("price",  "0"); // Use random number for market orders as mentioned in the bitfinex documentation. We set it to 0
		jo.put("exchange", "bitfinex");
		jo.put("side", "buy");
		jo.put("type",   "market"); // means "Margin market"
		jo.put("ocoorder",   "false"); 
		jo.put("buy_price_oco",   "0"); 
		jo.put("sell_price_oco",   "0"); 

		String payload = jo.toString();

		try {
			String response = httpCallBitFinex(BITFINEX_PROTOCOL, BITFINEX_DOMAIN, "/v1/order/new", "POST", apiKey, secretKey, payload);

			return mapper.readValue(response, BitFinexOrderStatus.class);
		} catch (JsonParseException e) {
			throw new ClosePositionException("JsonParseException: "+e.getMessage());
		} catch (JsonMappingException e) {
			throw new ClosePositionException("JsonMappingException: "+e.getMessage());
		} catch (IOException e) {
			throw new ClosePositionException("IOException: "+e.getMessage());
		}
	}

	@Override
	public BitFinexOrderStatus orderStatus(Long orderId, String apiKey, String secretKey) throws OrderStatusException {

		long nonce = System.currentTimeMillis();
		String path = "/v1/order/status";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));
		jo.put("order_id", Long.toString(orderId));

		String payload = jo.toString();

		try {
			String response = httpCallBitFinex(BITFINEX_PROTOCOL, BITFINEX_DOMAIN, path, "POST", apiKey, secretKey, payload);

			return mapper.readValue(response, BitFinexOrderStatus.class);
		} catch (JsonParseException e) {
			throw new OrderStatusException("JsonParseException: "+e.getMessage(),Long.toString(orderId) );
		} catch (JsonMappingException e) {
			throw new OrderStatusException("JsonMappingException: "+e.getMessage(), Long.toString(orderId));
		} catch (IOException e) {
			throw new OrderStatusException("IOException: "+e.getMessage(), Long.toString(orderId));
		}
	}

	@Override
	public BitFinexOrderStatus cancelOrder(Long orderId, String apiKey, String secretKey) throws CancelOrderException {

		long nonce = System.currentTimeMillis();
		String path = "/v1/order/cancel";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));
		jo.put("order_id", Long.toString(orderId));
		jo.put("id", Long.toString(orderId));

		String payload = jo.toString();
		try {
			String response = httpCallBitFinex(BITFINEX_PROTOCOL, BITFINEX_DOMAIN, path, "POST", apiKey, secretKey, payload);

			return mapper.readValue(response, BitFinexOrderStatus.class);
		} catch (JsonParseException e) {
			throw new CancelOrderException("JsonParseException: "+e.getMessage());
		} catch (JsonMappingException e) {
			throw new CancelOrderException("JsonMappingException: "+e.getMessage());
		} catch (IOException e) {
			throw new CancelOrderException("IOException: "+e.getMessage());
		}
	}

	@Override
	public List<BitFinexTransferStatus> transfer(String from, String to, String quoteCurrency, Double quoteCurrencyAmount, String apiKey, String secretKey) throws TransferException {

		long nonce = System.currentTimeMillis();
		String path = "/v1/transfer";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));
		jo.put("amount", Double.toString(quoteCurrencyAmount));
		jo.put("currency", quoteCurrency.toUpperCase());
		jo.put("walletfrom", from);
		jo.put("walletto", to);

		String payload = jo.toString();
		try {
			String response = httpCallBitFinex(BITFINEX_PROTOCOL, BITFINEX_DOMAIN, path, "POST", apiKey, secretKey, payload);

			return mapper.readValue(response, mapper.getTypeFactory().constructCollectionType(List.class, BitFinexTransferStatus.class));
		} catch (JsonParseException e) {
			throw new TransferException("JsonParseException: "+e.getMessage());
		} catch (JsonMappingException e) {
			throw new TransferException("JsonMappingException: "+e.getMessage());
		} catch (IOException e) {
			throw new TransferException("IOException: "+e.getMessage());
		}

	}

	@Override
	public List<BitFinexBalanceStatus> getBalances(String apiKey, String secretKey) throws BalancesException {

		long nonce = System.currentTimeMillis();
		String path = "/v1/balances";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));

		String payload = jo.toString();
		try {
			String response = httpCallBitFinex(BITFINEX_PROTOCOL, BITFINEX_DOMAIN, path, "POST", apiKey, secretKey, payload);

			return mapper.readValue(response, mapper.getTypeFactory().constructCollectionType(List.class, BitFinexBalanceStatus.class));
		} catch (JsonParseException e) {
			throw new BalancesException("JsonParseException: "+e.getMessage());
		} catch (JsonMappingException e) {
			throw new BalancesException("JsonMappingException: "+e.getMessage());
		} catch (IOException e) {
			throw new BalancesException("IOException: "+e.getMessage());
		}

	}

	@Override
	public BitFinexOrderStatus buy(String symbol, String amount, String price, String type, String apiKey, String secretKey) throws BuyException {

		long nonce = System.currentTimeMillis();
		String path = "/v1/order/new";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));
		jo.put("symbol", symbol.toUpperCase());
		jo.put("amount", amount); 
		jo.put("price",  price); 
		jo.put("exchange", "bitfinex");
		jo.put("side", "buy");
		jo.put("type",  type); 

		String payload = jo.toString();
		try {
			String response = httpCallBitFinex(BITFINEX_PROTOCOL, BITFINEX_DOMAIN, path, "POST", apiKey, secretKey, payload);

			return mapper.readValue(response, BitFinexOrderStatus.class);
		} catch (JsonParseException e) {
			throw new BuyException("JsonParseException: "+e.getMessage());
		} catch (JsonMappingException e) {
			throw new BuyException("JsonMappingException: "+e.getMessage());
		} catch (IOException e) {
			throw new BuyException("IOException: "+e.getMessage());
		}

	}

	@Override
	public BitFinexOrderStatus sell(String symbol, String amount, String price, String type, String apiKey, String secretKey) throws SellException {

		long nonce = System.currentTimeMillis();
		String path = "/v1/order/new";

		//PAYLOAD CONSTRUCTION
		JSONObject jo = new JSONObject();
		jo.put("request", path);
		jo.put("nonce", Long.toString(nonce));
		jo.put("symbol", symbol.toUpperCase());
		jo.put("amount", amount); 
		jo.put("price",  price); 
		jo.put("exchange", "bitfinex");
		jo.put("side", "sell");
		jo.put("type",  type); 
		jo.put("ocoorder" , false); 
		jo.put("buy_price_oco","0"); 
		jo.put("sell_price_oco", "0"); 

		String payload = jo.toString();
		try {
			String response = httpCallBitFinex(BITFINEX_PROTOCOL, BITFINEX_DOMAIN, path, "POST", apiKey, secretKey, payload);

			return mapper.readValue(response, BitFinexOrderStatus.class);
		} catch (JsonParseException e) {
			throw new SellException("JsonParseException: "+e.getMessage());
		} catch (JsonMappingException e) {
			throw new SellException("JsonMappingException: "+e.getMessage());
		} catch (IOException e) {
			throw new SellException("IOException: "+e.getMessage());
		}

	}

	@Override
	public BitFinexTicker getTicker(String symbol) {

		String path = "/pubticker/"+symbol.toLowerCase();

		BitFinexTicker ticker = restTemplate.getForObject(BITFINEX_PROTOCOL+BITFINEX_DOMAIN+path, BitFinexTicker.class) ;

		//return Double.valueOf(ticker.getAsk());
		
		return ticker;
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
		//String method = "GET";
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

			String payload_sha384hmac = hmacDigest(payload_base64, secretKey, BITFINEX_ALGORITHM_HMACSHA384);

			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
			conn.addRequestProperty("X-BFX-APIKEY", apiKey);
			conn.addRequestProperty("X-BFX-PAYLOAD", payload_base64);
			conn.addRequestProperty("X-BFX-SIGNATURE", payload_sha384hmac);

			if (log.isDebugEnabled()) {
				log.debug("URL: "+ protocol+domain+path);
				log.debug("X-BFX-APIKEY: "+ apiKey);
				log.debug("X-BFX-PAYLOAD: "+payload_base64);
				log.debug("X-BFX-SIGNATURE: "+ payload_sha384hmac);
				log.debug("payload: "+payload);

			}

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



}
