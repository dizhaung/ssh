package host;


import java.util.ArrayList;
import java.util.List;

import constants.regex.Regex;

public class TinyHost extends HostBase {

	private String commandResult;
	private String hostName;
	
	
	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getCommandResult() {
		return commandResult;
	}

	public void setCommandResult(String commandResult) {
		this.commandResult = commandResult;
	}

	
	 

	@Override
	public String toString() {
		return "TinyHost [commandResult=" + commandResult + ", hostName="
				+ hostName + ", toString()=" + super.toString() + "]";
	}

	/**
	 * 从主机配置文件内容构造主机List
	 * @author HP
	 * @param fileContent  主机配置文件读取的内容，每行\n分隔
	 * @return
	 */
	public static List<TinyHost> getHostList(final String fileContent) {
		
          List<TinyHost> hostList =  new ArrayList<TinyHost>();
          
            String[] lines = fileContent.split(Regex.CommonRegex.LINE_REAR.toString());
              for(String tempString:lines){
                System.out.println(tempString);
                if(!tempString.matches(Regex.CommonRegex.LINE_COMMENT.toString())){  //注释掉的主机过滤掉不链接
                	String[] strs = tempString.split(Regex.CommonRegex.ITEM_DELIMITER.toString());
                    
                	TinyHost host = new TinyHost();
                	host.setBuss(strs[0]);
                	host.setIp(strs[1]);
                	host.setJkUser(strs[2]);
                	host.setJkUserPassword(strs[3]);
                	host.setRootUser(strs[4]);
                	host.setRootUserPassword(strs[5]);
                
                	hostList.add(host);
                }
               }
          
		return hostList;
    }
}
