package constants;

public enum Unit {

	MB(1024*1024),
	GB(1024*1024*1024);
	private final long value;
	
	private Unit(final long value){
		this.value = value;
	}
	public long unitValue(){
		return value;
	}
}
