package host;

public class HostBase {
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
	 * 主机类型  (数据库主机 or 应用主机)
	 */
	private String hostType;
	
	
	private String os;

	private int sshPort = 22;
	
	
	public int getSshPort() {
		return sshPort;
	}
	public void setSshPort(int sshPort) {
		if(sshPort > 0)
			this.sshPort = sshPort;
	}
	public String getOs() {
		return os;
	}
	public void setOs(String os) {
		this.os = os;
	}
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
		return "HostBase [buss=" + buss + ", ip=" + ip + ", rootUser="
				+ rootUser + ", rootUserPassword=" + rootUserPassword
				+ ", jkUser=" + jkUser + ", jkUserPassword=" + jkUserPassword
				+ ", hostType=" + hostType + ", os=" + os + "]";
	}
	
}
