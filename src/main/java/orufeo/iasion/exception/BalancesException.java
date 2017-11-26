package orufeo.iasion.exception;

public class BalancesException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String errorCode="BALANCES_EXCEPTION";
	
	public BalancesException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}
	
	public BalancesException(String message){
		super(message);
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}
	
}