package host;

/**
 * 应用端口对应的farm和虚地址
 * @author HP
 *
 */
public class PortLoadConfig{
	private String port;
	private String farm;
	private String serviceIp;
	private String servicePort;
	
	
	@Override
	public String toString() {
		return "PortLoadConfig [port=" + port + ", farm=" + farm
				+ ", serviceIp=" + serviceIp + ", servicePort="
				+ servicePort + "]";
	}
	public String getServicePort() {
		return servicePort;
	}
	public void setServicePort(String servicePort) {
		this.servicePort = servicePort;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getFarm() {
		return farm;
	}
	public void setFarm(String farm) {
		this.farm = farm;
	}
	public String getServiceIp() {
		return serviceIp;
	}
	public void setServiceIp(String serviceIp) {
		this.serviceIp = serviceIp;
	}
	
	
}