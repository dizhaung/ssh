package ssh.collect;

import host.FileManager;
import host.Host;
import host.HostBase;
import host.LoadBalancer;
import host.PortLoadConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import constants.regex.Regex;
import constants.regex.Regex.CommonRegex;
import constants.regex.RegexEntity;

import net.sf.json.JSONObject;
import ssh.SSHClient;
import ssh.Shell;
import ssh.ShellException;

import collect.CollectedResource;
import collect.dwr.DwrPageContext;
import collect.model.HintMsg;

public class LoadBalancerCollector {

	private static Log logger = LogFactory.getLog(LoadBalancerCollector.class);
	/***
	 *  采集负载均衡配置
	 * @return
	 */
	public static StringBuilder collectLoadBalancer(){
		
		final StringBuilder allLoadBalancerFarmAndServerInfo = new StringBuilder();
	 	//获取负载均衡配置文件
		///加载负载均衡配置
		final List<LoadBalancer> loadBalancerList = LoadBalancer.getLoadBalancerList(FileManager.readFile("/loadBalancerConfig.txt"));
		
		///连接每个负载获取负载信息
	    final int loadBalanceNowNum = 0,loadBalanceMaxNum = loadBalancerList.size();
		final CollectedResource resource = new CollectedResource(0); 
		if(loadBalancerList.size() > 0){
			
			for(int i = 0 ,size = loadBalancerList.size();i < size;i++){
				final LoadBalancer lb = loadBalancerList.get(i);
				 
				Thread thread = new Thread(new Runnable(){
	
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Shell shell = null;
						try {
							logger.info("------"+lb.getIp()+"开始采集------");
							
							shell = new Shell(new HostBase(lb.getIp(),lb.getSshPort(),lb.getUserName(),lb.getPassword()));
							shell.setTimeout(10*1000);
							
							shell.setCommandLinePromptRegex(shell.getPromptRegexArrayByTemplateAndSpecificRegex(shell.COMMAND_LINE_PROMPT_REGEX_TEMPLATE,new String[]{"--More--","peer#"}));
							
							List<String> cmdsToExecute = new ArrayList<String>();
							
							String cmdResult;
							shell.executeCommands(new String[]{"system config immediate"});
							cmdResult = shell.getResponse();
							logger.info(lb.getIp()+"======="+cmdResult);
					
							shell.disconnect();
							
							
							synchronized(allLoadBalancerFarmAndServerInfo){
								allLoadBalancerFarmAndServerInfo.append(cmdResult);
							}
							
						} catch (ShellException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							logger.error("无法登陆到		"+lb.getIp());
							//continue;
						}finally{
							//此负载均衡采集完毕（包括采集配置完成和无法链接到主机的情况）
							
							 resource.increase();
							 logger.info(lb.getIp()+"负载均衡采集完毕---------");
							 
						}
						
					}
					
				},lb.getIp());
				
				thread.start();
			 }
			/*********************
			 * 最好的情况，当判断while时，资源全部采集完毕，即不进入while体，直接提示采集完毕
			 * 最坏情况，while循环
			 ********************/
			synchronized(resource){
				logger.info("--------等待负载均衡采集---------");
				while(resource.getNumber() < loadBalanceMaxNum){
					HintMsg msg = new HintMsg(resource.getNumber(),loadBalanceMaxNum,"","当前负载均衡配置文件下载进度,已完成"+resource.getNumber()+"个,共"+loadBalanceMaxNum+"个");
					DwrPageContext.realtimeCollect(JSONObject.fromObject(msg).toString());
					logger.info(msg);
					try {
						resource.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					  
				} 
			}
		}
		HintMsg msg = new HintMsg(resource.getNumber(),loadBalanceMaxNum,"下载完毕","当前负载均衡配置文件下载进度");
		DwrPageContext.realtimeCollect(JSONObject.fromObject(msg).toString());
		logger.info(msg);
		return allLoadBalancerFarmAndServerInfo;
	}

	   public static List<PortLoadConfig> parsePortList(final Host h,final String allLoadBalancerFarmAndServerInfo){
		   /****************
			 * 应用的负载均衡
			 ****************/
		 
			///正则匹配出Farm及对应的port
			//logger.info();
		 	Pattern farmAndPortRegex = Pattern.compile(Regex.CommonRegex.FARM_PORT_PREFIX.plus(RegexEntity.newInstance(h.getIp())).plus(Regex.CommonRegex.FARM_PORT_SUFFIX).toString());
			Matcher farmAndPortMatcher = farmAndPortRegex.matcher(allLoadBalancerFarmAndServerInfo.toString());
			
			List<PortLoadConfig> portListFromLoad = new ArrayList();
			while(farmAndPortMatcher.find()){
				PortLoadConfig port = new PortLoadConfig();
				portListFromLoad.add(port);
				port.setFarm(farmAndPortMatcher.group(1));
				port.setPort(farmAndPortMatcher.group(2));
				///匹配端口对应的服务地址（虚地址）和服务端口
				Pattern serviceIpAndPortRegex = Pattern.compile(Regex.CommonRegex.SERVICEIP_PORT.plus(RegexEntity.newInstance(port.getFarm())).toString());
				Matcher serviceIpAndPortMatcher = farmAndPortRegex.matcher(allLoadBalancerFarmAndServerInfo.toString());
				if(serviceIpAndPortMatcher.find()){
					port.setServiceIp(serviceIpAndPortMatcher.group(1));
					port.setServicePort(serviceIpAndPortMatcher.group(3));
				}
				
				
			}
			return portListFromLoad;
	   }

}
