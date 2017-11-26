package orufeo.iasion.exception;

public class BuyException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String errorCode="BUY_EXCEPTION";
	
	public BuyException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}
	
	public BuyException(String message){
		super(message);
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}
	
}