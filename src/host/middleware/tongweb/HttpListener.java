package host.middleware.tongweb;

public class HttpListener {
	private String id;
	private String defaultVirtualServer;
	private String port;
	private boolean securityEnabled;
	
	public boolean isSecurityEnabled() {
		return securityEnabled;
	}
	public void setSecurityEnabled(boolean securityEnabled) {
		this.securityEnabled = securityEnabled;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDefaultVirtualServer() {
		return defaultVirtualServer;
	}
	public void setDefaultVirtualServer(String defaultVirtualServer) {
		this.defaultVirtualServer = defaultVirtualServer;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((id == null) ? 0 : id.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HttpListener other = (HttpListener) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "HttpListener [id=" + id + ", defaultVirtualServer="
				+ defaultVirtualServer + ", port=" + port
				+ ", securityEnabled=" + securityEnabled + "]";
	}
	
}
