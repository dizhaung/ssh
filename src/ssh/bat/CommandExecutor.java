package ssh.bat;

import host.Host;
import host.Host.Database;
import host.Host.Middleware;
import host.TinyHost;
import host.command.CollectCommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.json.JSONObject;
import ssh.SSHClient;
import ssh.Shell;
import ssh.ShellException;
import ssh.collect.HostCollector;
import collect.CollectedResource;
import collect.dwr.DwrPageContext;
import collect.model.HintMsg;
import constants.regex.Regex;
import constants.regex.Regex.AixRegex;
import constants.regex.Regex.CommonRegex;

public class CommandExecutor {

	private static Log logger = LogFactory.getLog(CommandExecutor.class);

	/**
	 * 
	 * @param list	批处理的主机
	 * @param command	要在所有主机上执行的命令
	 */
	private  void batHosts(final List<TinyHost> list,final String command,final boolean isNotCollected){
		
		final int maxNum = list.size(), nowNum = 0;
		final CollectedResource resource = new CollectedResource(0); 
		if(list.size() > 0){
			for (Iterator<TinyHost> it = list.iterator();it.hasNext();) {
				final TinyHost h = it.next();
				
				Thread thread = new Thread(new Runnable(){
	
					@Override
					public void run() {
						// TODO Auto-generated method stub
						
						logger.info(h);
		    			 
		    			// 初始化服务器连接信息
		    			SSHClient ssh = new SSHClient(h);
	
		    			// 建立连接
		    			Shell shell;
		    			try {
		    				shell = new Shell(h);
		    				shell.setTimeout(2*1000);
		    				
		    				logger.error("	连接到 	"+h.getIp());
			    			
			    			//grantRoot(shell,h);
		    				//第一次执行命令  需要采集设备信息
			    			if(isNotCollected){
			    				
				    			collectOs(shell,h);
				    			
				    			//获取主机名
				    			shell.executeCommands(new String[] { "uname -n" });
				    			String cmdResult = shell.getResponse();
				    			logger.info(h.getIp()+cmdResult);
				    			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.HOST_NAME);
				    			logger.info(h.getIp()+"主机名="+shell.parseInfoByRegex(Regex.AixRegex.HOST_NAME,cmdResult,1));
				    			h.setHostName(shell.parseInfoByRegex(Regex.AixRegex.HOST_NAME,cmdResult,1));
				    			
				    			/*********************
				    			 * 检测是否安装了Oracle数据库
				    			 *********************/
				    			List<Host.Database> dList = new ArrayList<Host.Database>(); 
				    			shell.executeCommands(new String[] { "ps -ef|grep tnslsnr" });
				    			cmdResult = shell.getResponse();
				    			logger.info(h.getIp()+cmdResult);
				    			 
				    			boolean isExistOracle = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString()).length >=4?true:false;
				    			
				    			/*********************
				    			 * 检查是否安装了weblogic中间件
				    			 *********************/
				    			List<Host.Middleware> mList = new ArrayList<Host.Middleware>();  
				    			shell.executeCommands(new String[] { "ps -ef|grep weblogic" });
				    			cmdResult = shell.getResponse();
	
				    			logger.info(h.getIp()+cmdResult); 
				    			 
				    			boolean isExistWeblogic = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString()).length >=4?true:false;
				    			h.setHostType((isExistOracle?"数据库服务器":"")+(isExistWeblogic?" 应用服务器":""));
			    			}
			    				
				    			
				    			/*********************
				    			 * 执行命令
				    			 **********************/
				    			
				    			shell.executeCommands(new String[]{command});
				    			String cmdResult = shell.getResponse();
				    			String[] resultLines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
				    			String commandPrompt = Pattern.quote(resultLines[resultLines.length-1]);
				    			
				    			logger.info(h.getIp()+cmdResult);
				    			logger.info(h.getIp()+"正则表达式="+command+"\\s+([\\s\\S]+?)"+commandPrompt);
				    			logger.info(h.getIp()+"命令执行结果="+shell.parseInfoByRegex(Pattern.quote(command)+"\\s+([\\s\\S]+?)"+commandPrompt,cmdResult,1));
				    		 
				    			h.setCommandResult(shell.parseInfoByRegex(Pattern.quote(command)+"\\s+([\\s\\S]+?)"+commandPrompt,cmdResult,1));
				    			
				    			 
			    			/*if("AIX".equalsIgnoreCase(h.getOs())){
			    				 
			    			}else if("LINUX".equalsIgnoreCase(h.getOs())){
			    				 
			    			}else if("HP-UNIX".equalsIgnoreCase(h.getOs())){
			    				
			    			}*/
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
					HintMsg msg = new HintMsg(resource.getNumber(),maxNum,"","当前命令执行进度,已完成"+resource.getNumber()+"个,共"+maxNum+"个");
					DwrPageContext.realtimeBat(JSONObject.fromObject(msg).toString()); 
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
		
		HintMsg msg = new HintMsg(resource.getNumber(),maxNum,"采集完毕","当前命令执行进度");
		DwrPageContext.realtimeBat(JSONObject.fromObject(msg).toString());
		logger.info(msg);
	}

	protected  void collectOs(Shell shell, TinyHost h) {
		// TODO Auto-generated method stub
		 
		shell.executeCommands(new String[] { CollectCommand.CommonCommand.HOST_OS.toString() });
		String cmdResult = shell.getResponse();
		//获取操作系统的类型
		logger.info(h.getIp()+"---操作系统的类型---");
		logger.info(h.getIp()+"执行命令的结果="+cmdResult);
		logger.info(h.getIp()+"正则表达式		\\s*uname\r\n(.*)\r\n");
		h.setOs(shell.parseInfoByRegex("\\s*uname\\s+(\\w+?)\\s+",cmdResult,1));
		logger.info(h.getIp()+"操作系统类型="+shell.parseInfoByRegex("\\s*uname\\s+(\\w+?)\\s+",cmdResult,1));
	}

	public  static void startBat(final List<TinyHost> list,final String command,final boolean isNotCollected){
		//向用户传递命令执行进度
		int maxNum = list.size();
		
		if(maxNum == 0){
			HintMsg msg = new HintMsg(0,0,"无","");
			DwrPageContext.realtimeBat(JSONObject.fromObject(msg).toString());
			logger.info(msg);
			return;
		} 
		//执行命令 并批处理主机
		(new CommandExecutor()).batHosts(list,command,isNotCollected);
		
	}

}
