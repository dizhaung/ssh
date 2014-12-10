package ssh;


public class ShellException extends Exception {

	
	
	public ShellException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ShellException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public ShellException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
		this.cause = cause;
	}

	private Throwable cause=null;

	@Override
	public Throwable getCause() {
		// TODO Auto-generated method stub
		return cause;
	}
	
}
