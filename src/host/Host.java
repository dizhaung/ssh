package host;

/**
 * 配置文件每行的数据实体
 * @author tiger
 *
 */
public class Host {
	/**
	 * 业务名称
	 */
	private String buss;
	
	/**
	 * IP地址
	 */
	private String ip;
	
	/**
	 * 用户名
	 */
	private String rootUser;
	
	/**
	 * 密码
	 */
	private String rootUserPassword;
	
	/**
	 * jk用户名
	 */
	private String jkUser;
	
	/**
	 * jk用户密码
	 */
	
	private String jkUserPassword;
	/**
	 * 主机类型,DB为数据库主机，APP为应用主机
	 */
	private String hostType;
	public String getBuss() {
		return buss;
	}
	public void setBuss(String buss) {
		this.buss = buss;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getRootUser() {
		return rootUser;
	}
	public void setRootUser(String rootUser) {
		this.rootUser = rootUser;
	}
	public String getRootUserPassword() {
		return rootUserPassword;
	}
	public void setRootUserPassword(String rootUserPassword) {
		this.rootUserPassword = rootUserPassword;
	}
	public String getJkUser() {
		return jkUser;
	}
	public void setJkUser(String jkUser) {
		this.jkUser = jkUser;
	}
	public String getJkUserPassword() {
		return jkUserPassword;
	}
	public void setJkUserPassword(String jkUserPassword) {
		this.jkUserPassword = jkUserPassword;
	}
	public String getHostType() {
		return hostType;
	}
	public void setHostType(String hostType) {
		this.hostType = hostType;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		sb.append(this.getBuss());
		sb.append("|");
		sb.append(this.getIp());
		sb.append("|");
		sb.append(this.getJkUser());
		sb.append("|");
		sb.append(this.getJkUserPassword());
		sb.append("|");
		
		sb.append(this.getRootUser());
		sb.append("|");
		sb.append(this.getRootUserPassword());
		return sb.toString();
	}
		
	

}
