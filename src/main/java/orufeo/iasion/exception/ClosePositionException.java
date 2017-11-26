package orufeo.iasion.exception;

public class ClosePositionException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String errorCode="CLOSEPOSITION_EXCEPTION";
	
	public ClosePositionException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}
	
	public ClosePositionException(String message){
		super(message);
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}
	
}
