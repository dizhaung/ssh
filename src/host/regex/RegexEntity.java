package host.regex;

public class RegexEntity  implements Regex{

	private StringBuilder buffer = new StringBuilder();
	
	/**
	 * 作为右值的时候使用此构造
	 */
	private RegexEntity(){
		
	}
	
	/**
	 * Regex enum作为运算的左值,加入连缀buffer
	 * @param buffer
	 */
	
	private RegexEntity(final String regex){
		if(buffer == null){
			buffer = new StringBuilder();
		}
		this.append(regex);
	}
	
	public static  Regex newInstance(){
		return new RegexEntity();
	}
	public static  Regex newInstance(final String regex){
		return new RegexEntity(regex);
	}
	
	
	
	
	
	public Regex append(final String regex){
		buffer.append(regex);
		return this;
	}
	@Override
	public Regex plus(Regex regex) {
		// TODO Auto-generated method stub
		if(buffer == null){
			buffer = new StringBuilder();
		}
		this.append(regex.toString());
		return this;
	}
	@Override
	public String toString() {
		return buffer.toString();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Regex r = RegexEntity.newInstance("jjjjjjjjjjjjjj").plus(Regex.LinuxRegex.HOST_TYPE).plus(Regex.CommonRegex.HOST_OS);
		System.out.println(r);
	}

	
}