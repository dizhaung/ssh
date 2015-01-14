package constants;

public enum Format {
	TWO_DIGIT_DECIMAL("#.##");
	private final String format;
	
	private Format(final String format){
		this.format = format;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.format;
	}
	
}
