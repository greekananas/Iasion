package orufeo.iasion.exception;

public class MaxTriesExceededException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	private String errorCode="MAXTRIES_EXCEPTION";
	
	public MaxTriesExceededException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}

}
