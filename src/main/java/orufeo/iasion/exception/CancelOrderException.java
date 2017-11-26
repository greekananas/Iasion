package orufeo.iasion.exception;

public class CancelOrderException extends Exception  {

	private static final long serialVersionUID = 1L;

	private String errorCode="CANCELORDER_EXCEPTION";

	public CancelOrderException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}

	public CancelOrderException(String message){
		super(message);
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}

}
