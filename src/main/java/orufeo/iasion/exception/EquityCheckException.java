package orufeo.iasion.exception;

public class EquityCheckException extends Exception {

private static final long serialVersionUID = 1L;
	
	private String errorCode="EQUITYCHECK_EXCEPTION";
	
	public EquityCheckException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}
	
	public EquityCheckException(String message){
		super(message);
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}
	
}
