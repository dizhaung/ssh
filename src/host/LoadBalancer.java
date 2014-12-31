package host;

import java.util.ArrayList;
import java.util.List;

/**
 * 负载均衡配置
 * @author HP
 *
 */
public class LoadBalancer {

	private String ip;
	private String userName;
	private String  password;
	
	@Override
	public String toString() {
		return "LoadBalancer [ip=" + ip + ", userName=" + userName
				+ ", password=" + password + "]";
	}

	public LoadBalancer(String ip, String userName, String password) {
		super();
		this.ip = ip;
		this.userName = userName;
		this.password = password;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * 从负载配置文件内容  构造负载list
	 * @author HP
	 * @param fileContent
	 * @return
	 */
	public static List<LoadBalancer> getLoadBalancerList(final String fileContent){
		List<LoadBalancer> list = new ArrayList<LoadBalancer>();
		String[] lines = fileContent.split("\n");
        for(String tempString:lines){
          System.out.println(tempString);
          if(!tempString.matches("^#.*")){  //注释掉的负载均衡过滤掉不链接
          	String[] strs = tempString.split("\\|");
            
          	list.add(new LoadBalancer(strs[0],strs[1],strs[2]));
          }
         }
		return list;
	}
}
