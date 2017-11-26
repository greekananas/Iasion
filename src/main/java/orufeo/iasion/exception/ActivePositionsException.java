package orufeo.iasion.exception;

public class ActivePositionsException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String errorCode="ACTIVEPOSITIONS_EXCEPTION";
	
	public ActivePositionsException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}
	
	public ActivePositionsException(String message){
		super(message);
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}
	
}
