package orufeo.iasion.exception;

public class TransferException extends Exception {

private static final long serialVersionUID = 1L;
	
	private String errorCode="TRANSFER_EXCEPTION";
	
	public TransferException(String message, String errorCode){
		super(message);
		this.errorCode=errorCode;
	}
	
	public TransferException(String message){
		super(message);
	}
	
	public String getErrorCode(){
		return this.errorCode;
	}
	
}
