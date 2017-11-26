package orufeo.iasion.exception;

public class OrderStatusException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String errorCode="ORDERSTATUS_EXCEPTION";
	
	public OrderStatusException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}
	
	public OrderStatusException(String message){
		super(message);
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}
	
	
}
