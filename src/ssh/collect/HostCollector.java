package ssh.collect;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.json.JSONObject;
import collect.CollectedResource;
import collect.dwr.DwrPageContext;
import collect.model.HintMsg;

import host.Host;
import host.Host.Middleware.App;
import host.HostBase;
import host.PortLoadConfig;
import host.command.CollectCommand;
import host.command.CollectCommand.CommonCommand;
import ssh.SSHClient;
import ssh.Shell;
import ssh.ShellException;
import constants.regex.Regex;
import constants.regex.Regex.CommonRegex;

public  class HostCollector implements Collectable {
	private static Log logger = LogFactory.getLog(HostCollector.class);
	/**
	 * 提升为root权限
	 * @param shell
	 * @param h
	 */
	public  void grantRoot(final Shell shell, final HostBase h) {
		logger.info(h.getIp()+"	root用户登录");
		//当需要特别权限的情况下使用root用户
		String rootUser = h.getRootUser();
		String rootUserPassword = h.getRootUserPassword();
		//切换到root用户 ，提升权限
		shell.setCommandLinePromptRegex(shell.getPromptRegexArrayByTemplateAndSpecificRegex(SSHClient.COMMAND_LINE_PROMPT_REGEX_TEMPLATE,new String[]{"Password:"}));
		
		shell.executeCommands(new String[] { "su -" });
		String cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+cmdResult);
		///模拟输入root密码
		shell.executeCommands(new String[] { rootUserPassword });
		cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+cmdResult);
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		logger.info(lines.length);
		if(lines.length >2){
			logger.info(h.getIp()+"	root用户登录错误	可能是root用户密码"+rootUserPassword+"不正确");
		}
		
	}

	public HostCollector() {
		super();
	}

	/**
	 * 采集操作系统的类型   AIX  Linux
	 * @param shell
	 * @param h
	 */
	public  void collectOs(final Shell shell,final HostBase h){
		 
		shell.executeCommands(new String[] { CollectCommand.CommonCommand.HOST_OS.toString() });
		String cmdResult = shell.getResponse();
		//获取操作系统的类型
		logger.info(h.getIp()+"---操作系统的类型---");
		logger.info(h.getIp()+"执行命令的结果="+cmdResult);
		logger.info(h.getIp()+"正则表达式		\\s*uname\r\n(.*)\r\n");
		h.setOs(shell.parseInfoByRegex("\\s*uname\\s+(\\w+?)\\s+",cmdResult,1));
		logger.info(h.getIp()+"操作系统类型="+shell.parseInfoByRegex("\\s*uname\\s+(\\w+?)\\s+",cmdResult,1));
	}

	public  List<App>  searchServiceIpAndPortForEachOf(final List<App> appList,final List<PortLoadConfig> portListFromLoad){
	///主机端口和             服务IP(虚地址)-服务端口（虚端口）-端口对应表（PortLoadConfig）中的端口字段对应
		for(int i = 0 , size = appList.size();i<size;i++){
			App app = appList.get(i);
			for(PortLoadConfig port:portListFromLoad){
				if(app.getPort().equals(port.getPort())){
					app.setServiceIp(port.getServiceIp());
					app.setServicePort(port.getServicePort());
					break;
				}
			}
		}
		return appList;
	}

	public  boolean existFileOnPath(final String path,final String fileName,final Shell shell){
		
		shell.executeCommands(new String[] { "cd "+path});
		String cmdResult = shell.getResponse();
		logger.info(cmdResult);
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		if(lines.length > 2 ){
			logger.info("不存在"+path+"路径或者不是一个文件夹");
			return false;
		}
		shell.executeCommands(new String[] { "ls "+path+" |grep "+fileName});
		cmdResult = shell.getResponse();
		
		logger.info(cmdResult);
		lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		if(lines.length > 2){
			logger.info("在"+path+"文件夹下存在文件"+fileName+"文件");
			return true;
		}
		return false;
	}

	public  boolean existDirectory(final String directory,final Shell shell){
		shell.executeCommands(new String[] { "cd "+directory });
		String cmdResult = shell.getResponse();
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		if(lines.length > 2){
			return false;
		}
		return true;
	}

	public  String parseValidXmlTextFrom(final String cmdResult){
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		String serverDotXml = cmdResult.replaceAll("^"+Pattern.quote(lines[0]), "").replaceAll(Pattern.quote(lines[lines.length-1])+"$", "").trim();
		return serverDotXml;
	}

	/**
	 * 采集主机
	 * @param list	主机列表
	 * @param allLoadBalancerFarmAndServerInfo   负载均衡上采集到的配置信息
	 */
	public  void collectHosts(final List<Host> list,final StringBuilder allLoadBalancerFarmAndServerInfo){
		
		final int maxNum = list.size(), nowNum = 0;
		final CollectedResource resource = new CollectedResource(0); 
		if(list.size() > 0){
			for (Iterator<Host> it = list.iterator();it.hasNext();) {
				final Host h = it.next();
				
				Thread thread = new Thread(new Runnable(){
	
					@Override
					public void run() {
						// TODO Auto-generated method stub
						
						logger.info(h);
		    			// 登录
		    			SSHClient ssh = new SSHClient(h.getIp(), h.getJkUser(), h.getJkUserPassword());
	
		    			// 建立连接
		    			Shell shell;
		    			try {
		    				shell = new Shell(h.getIp(), SSHClient.SSH_PORT,h.getJkUser(), h.getJkUserPassword());
		    				shell.setTimeout(2*1000);
		    				
		    				logger.error("	连接到 	"+h.getIp());
			    			
			    			grantRoot(shell,h);
			    			
			    			collectOs(shell,h);
			    			
			    			List<PortLoadConfig> portListFromLoad  =   LoadBalancerCollector.parsePortList(h, allLoadBalancerFarmAndServerInfo.toString());
			    			if("AIX".equalsIgnoreCase(h.getOs())){
			    				HostCollector collector = new AixCollector();
			    				collector.collect(shell,ssh,h,portListFromLoad);
			    			}else if("LINUX".equalsIgnoreCase(h.getOs())){
			    				HostCollector collector = new LinuxCollector();
			    				collector.collect(shell,ssh,h,portListFromLoad);
			    			}else if("HP-UNIX".equalsIgnoreCase(h.getOs())){
			    				
			    			}
			    			logger.info(h);
			    			shell.disconnect();
		    			} catch (ShellException e) {
		    				// TODO Auto-generated catch block
		    				logger.error("无法采集主机"+h.getIp()+"，连接下一个主机");
		    				e.printStackTrace();
		    			}finally{
		    				synchronized(resource){
		    					logger.info(h.getIp()+"主机采集完毕---------");
		    					resource.increase();
		    				}
		    			}
		    			
					}
					
				},h.getIp());
				
				thread.start();
	
			}
			 
			synchronized(resource){
				
				while(resource.getNumber() < maxNum){
					HintMsg msg = new HintMsg(resource.getNumber(),maxNum,"","当前主机采集进度,已完成"+resource.getNumber()+"个,共"+maxNum+"个");
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
		
		HintMsg msg = new HintMsg(resource.getNumber(),maxNum,"采集完毕","当前主机采集进度");
		DwrPageContext.realtimeCollect(JSONObject.fromObject(msg).toString());
		logger.info(msg);
	}

	/**
	 * 采集
	 * @param list
	 */
	public static void startCollect(final List<Host> list){
		//向用户传递采集进度
		int maxNum = list.size();
		
		if(maxNum == 0){
			HintMsg msg = new HintMsg(0,0,"无","");
			DwrPageContext.realtimeCollect(JSONObject.fromObject(msg).toString());
			logger.info(msg);
			return;
		} 
			//采集负载均衡配置
		logger.info("------开始采集负载均衡-----");
		StringBuilder allLoadBalancerFarmAndServerInfo  = LoadBalancerCollector.collectLoadBalancer();
		logger.info("------开始采集主机-----");
		(new HostCollector()).collectHosts(list,allLoadBalancerFarmAndServerInfo);
	}

	/* (non-Javadoc)
	 * @see ssh.collect.Collectable#collect(ssh.Shell, ssh.SSHClient, host.Host, java.util.List)
	 */
	@Override
	public  void collect(Shell shell, final SSHClient ssh, final Host h,
			final List<PortLoadConfig> portListFromLoad) {
			 	collectHostDetail(shell, ssh, h, portListFromLoad); 
				collectOracle(shell, h);
				collectWeblogic(shell, h, portListFromLoad);
				collectDB2(shell,h);
				
				collectWebSphere(shell, h, portListFromLoad);
				
			
				
				
				collectTomcat(shell,h,portListFromLoad);
				collectTongweb(shell,h,portListFromLoad);
			}

	@Override
	public void collectHostDetail(Shell shell, SSHClient ssh, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void collectOracle(Shell shell, Host h) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void collectWeblogic(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void collectDB2(Shell shell, Host h) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void collectWebSphere(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void collectTomcat(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void collectTongweb(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	public   boolean	match(final String regex,final String string){
		return Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(string).find();
	}

}