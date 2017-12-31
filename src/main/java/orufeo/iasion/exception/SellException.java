package orufeo.iasion.exception;

public class SellException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String errorCode="SELL_EXCEPTION";
	
	public SellException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}
	
	public SellException(String message){
		super(message);
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}
	
}