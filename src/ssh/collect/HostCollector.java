package ssh.collect;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
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
	public static Log logger = LogFactory.getLog(HostCollector.class);
	/**
	 * 使用pattern从cmdResult获取应用根目录，委托于另一个方法
	 * @param pattern     提取必要信息的正则
	 * @param cmdResult   shell命名执行后返回的结果
	 * @return
	 */
	public  Set<String> parseUserProjectSetByRegex(final Regex pattern, final String cmdResult) {
		
		return parseUserProjectSetByRegex(pattern.toString(),cmdResult);
	}

	/**
	 * 使用pattern从cmdResult获取应用根目录
	 * @param pattern     提取必要信息的模式
	 * @param cmdResult   shell命名执行后返回的结果
	 * @return
	 */
	public  Set<String> parseUserProjectSetByRegex(final String pattern, final String cmdResult) {
		Set<String> userProjects = new HashSet();
		//获取操作系统的类型
		Matcher m = Pattern.compile(pattern).matcher(cmdResult);
		while(m.find()){
			userProjects.add( m.group(1));
	
		}
		return userProjects;
	}

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

	

	/* (non-Javadoc)
	 * @see ssh.collect.Collectable#collect(ssh.Shell, ssh.SSHClient, host.Host, java.util.List)
	 */
	
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

	
	protected void collectHostDetail(Shell shell, SSHClient ssh, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	
	protected void collectOracle(Shell shell, Host h) {
		// TODO Auto-generated method stub
		
	}

	
	protected void collectWeblogic(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	
	protected void collectDB2(Shell shell, Host h) {
		// TODO Auto-generated method stub
		
	}

	
	protected void collectWebSphere(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	
	protected void collectTomcat(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	
	protected void collectTongweb(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	public   boolean	match(final String regex,final String string){
		return Pattern.compile(regex,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(string).find();
	}

}