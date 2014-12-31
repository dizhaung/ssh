package collect;

/**
 * 
 * @author HP
 *
 */
public class CollectedResource {

	private int number;
	
	public CollectedResource(int number) {
		super();
		this.number = number;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public synchronized void increase(){
		number += 1;
		this.notify();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
