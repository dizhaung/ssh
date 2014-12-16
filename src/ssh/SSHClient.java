package ssh;

import host.FileManager;
import host.Host;
import host.LoadBalancer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oro.text.regex.MalformedPatternException;

import collect.dwr.DwrPageContext;
import collect.model.HintMsg;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
 
import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
 
public class SSHClient {
 
	private static Log logger = LogFactory.getLog(SSHClient.class);
	private Session session;
	private ChannelShell channel;
	   
    private static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
    private static String ENTER_CHARACTER = "\r";
    private static final int SSH_PORT = 22;
    private List<String> lstCmds = new ArrayList<String>();
    public static final String[] LINUX_PROMPT_REGEX_TEMPLATE = new String[] { "~]#", "~#", "#",
        ":~#", "/$", ">","\\$","SQL>"};//加入\\$匹配登录后命令提示符为\$的情况
    private String[] linuxPromptRegex = null;
    private Expect4j expect = null;
    private StringBuilder buffer = null;
    private String userName;
    private String password;
    private String host;
 
    public void setLinuxPromptRegex(String[] linuxPromptRegex){
    	this.linuxPromptRegex = linuxPromptRegex;
    }
    /**
     *
     * @param host
     * @param userName
     * @param password
     */
    public SSHClient(String host, String userName, String password) {
        this.host = host;
        this.userName = userName;
        this.password = password;
    }
   /**
    * 
    * @param cmdsToExecute
    * @return
    * @throws ShellException     Shell创建或者执行失败异常    ，失败有下面几个可能得原因导致的	1.网络不通 		2用户名或者密码错误	3创建输入输出流错误	4expect创建或者执行异常
    */
    public String execute(List<String> cmdsToExecute) throws ShellException {
    	//没有特别的正则   则使用匹配命令提示的标准正则
    	if(linuxPromptRegex == null){
    		linuxPromptRegex = LINUX_PROMPT_REGEX_TEMPLATE;
    	}
        this.lstCmds = cmdsToExecute;
       
        Closure closure = new Closure() {
            public void run(ExpectState expectState) throws Exception {
            	 buffer = new StringBuilder();
                buffer.append(expectState.getBuffer());
            }
        };
        List<Match> lstPattern =  new ArrayList<Match>();
        for (String regexElement : linuxPromptRegex) {
            try {
                Match mat = new RegExpMatch(regexElement, closure);
                lstPattern.add(mat);
            } catch (MalformedPatternException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
 
        
        	/**
        	 * 每执行一条命令都重新连接服务器，异常的低效，为了兼容复杂的shell命令
        	 * 例如：prtconf |grep "Good Memory Size:"
        	 * 执行命令的错误提示：
        	 * expect4j.BlockingConsumer run
				信息: Found EOF to stop while loop
        	 */
        	
        try {
			expect = SSH();
			
            boolean isSuccess = true;
            for(String strCmd : lstCmds) {
                isSuccess = isSuccess(lstPattern,strCmd);
                if (!isSuccess) {
                    isSuccess = isSuccess(lstPattern,strCmd);
                }
            }
 
            checkResult(expect.expect(lstPattern));
        } catch (ShellException e) {
				// TODO Auto-generated catch block
				throw e;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ShellException(e.toString(),e);
		} finally {
        	//去掉特殊正则，默认回归标准的提示符匹配的正则
        	linuxPromptRegex = null;
        	this.disconnect();
        }
        return buffer.toString();
    }
    /**
     *
     * @param objPattern
     * @param strCommandPattern
     * @return
     */
    private boolean isSuccess(List<Match> objPattern,String strCommandPattern) {
        try {
            boolean isFailed = checkResult(expect.expect(objPattern));
 
            if (!isFailed) {
                expect.send(strCommandPattern);
                expect.send(ENTER_CHARACTER);
                return true;
            }
            return false;
        } catch (MalformedPatternException ex) {
            ex.printStackTrace();
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
    private final static long DEFAULT_TIMEOUT = 10*1000;
    
    /**
     *
     * @param hostname
     * @param username
     * @param password
     * @param port
     * @return
     * @throws ShellException 		shell创建和执行命令失败抛出异常   
     */
    private Expect4j SSH() throws ShellException {
        JSch jsch = new JSch();
        
        try {
			session = jsch.getSession(userName, host, SSH_PORT);
			
	        if (password != null) {
	            session.setPassword(password);
	        }
	        Hashtable<String,String> config = new Hashtable<String,String>();
	        config.put("StrictHostKeyChecking", "no");
	        //据stackoverflow.com上的解答，加入下面一条配置项
	        config.put("PreferredAuthentications", 
	                "publickey,keyboard-interactive,password");
	        session.setConfig(config);
	        session.connect(60000);
	        channel = (ChannelShell) session.openChannel("shell");
	        expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
	        expect.setDefaultTimeout(DEFAULT_TIMEOUT);
	        channel.connect();
        } catch (JSchException e) {
			// TODO Auto-generated catch block
			 
			throw new ShellException(e.toString(),e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			 
			throw new ShellException(e.toString(),e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			 
			throw new ShellException(e.toString(),e);
		}
        return expect;
    }
    public void disconnect(){
    	 if (expect!=null) {
             expect.close();
         }
     
        if(channel!=null){
            channel.disconnect();
        }
        if(session!=null){
            session.disconnect();
        }
    }
    /**
     *
     * @param intRetVal
     * @return
     */
    private boolean checkResult(int intRetVal) {
        if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
            return true;
        }
        return false;
    }
         
    /**
     *
     * @param args
     */
    public static void main(String[] args) {
    	String userDir = System.getProperty("user.dir");
    	
		List<Host> list = Host.getHostList(FileManager.readFile("/hostConfig.txt"));
		
		startCollect(list);
    }
    /**
     * 采集
     * @param list
     */
    public static void startCollect(List<Host> list){
    	//向用户传递采集进度
    	int maxNum = list.size();
    	StringBuilder allLoadBalancerFarmAndServerInfo = new StringBuilder();
    	if(maxNum == 0){
    		HintMsg msg = new HintMsg(0,0,"无");
    		DwrPageContext.run(JSONObject.fromObject(msg).toString());
    		logger.info(msg);
    	}else{
    	 	//获取负载均衡配置文件
    		///加载负载均衡配置
    		List<LoadBalancer> loadBalancerList = LoadBalancer.getLoadBalancerList(FileManager.readFile("/loadBalancerConfig.txt"));
    		System.out.println(loadBalancerList);
    		
    		///连接每个负载获取负载信息
    		
    		for(LoadBalancer lb: loadBalancerList){
    			Shell sshLoadBalancer;
    			try {
    				sshLoadBalancer = new Shell(lb.getIp(), SSH_PORT,lb.getUserName(), lb.getPassword());
    			} catch (ShellException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    				logger.error("无法登陆到		"+lb.getIp());
    				continue;
    			}
    			sshLoadBalancer.setLinuxPromptRegex(sshLoadBalancer.getPromptRegexArrayByTemplateAndSpecificRegex(LINUX_PROMPT_REGEX_TEMPLATE,new String[]{"--More--","peer#"}));
    			
    			List<String> cmdsToExecute = new ArrayList<String>();
    			
    			String cmdResult;
    			sshLoadBalancer.executeCommands(new String[]{"system config immediate"});
    			cmdResult = sshLoadBalancer.getResponse();
    			logger.info(cmdResult);
    	
    			sshLoadBalancer.disconnect();
    			
    			allLoadBalancerFarmAndServerInfo.append(cmdResult);
    		}
    		
    	}
    	int nowNum = 0;
    	for (Host h : list) {
			System.out.println(h);
			HintMsg msg = new HintMsg(nowNum++,maxNum,"当前IP:"+h.getIp());
			DwrPageContext.run(JSONObject.fromObject(msg).toString());
			logger.info(msg);
			// 设置参数
			String ip = h.getIp();
			String jkUser = h.getJkUser();
			String jkUserPassword = h.getJkUserPassword();
			
			//当需要特别权限的情况下使用root用户
			String rootUser = h.getRootUser();
			String rootUserPassword = h.getRootUserPassword();
			
			// 初始化服务器连接信息
			SSHClient ssh = new SSHClient(ip, jkUser, jkUserPassword);

			
			// 建立连接
			Shell shell;
			try {
				shell = new Shell(ip, SSH_PORT, jkUser, jkUserPassword);
			} catch (ShellException e) {
				// TODO Auto-generated catch block
				logger.error("无法采集主机"+ip+"，连接下一个主机");
				e.printStackTrace();
				continue;
			}
			logger.error("	连接到 	"+ip);
			
			
			//切换到root用户 ，提升权限
			shell.executeCommands(new String[] { "su -" });
			String cmdResult = shell.getResponse();
			
			logger.info(cmdResult);
			///模拟输入root密码
			shell.setLinuxPromptRegex(shell.getPromptRegexArrayByTemplateAndSpecificRegex(SSHClient.LINUX_PROMPT_REGEX_TEMPLATE,new String[]{"Password:"}));
			shell.executeCommands(new String[] { rootUserPassword });
			cmdResult = shell.getResponse();
				
			logger.info(cmdResult);
			
			shell.executeCommands(new String[] { "uname" });
		    cmdResult = shell.getResponse();
			//获取操作系统的类型
			logger.info("---操作系统的类型---");
			logger.info("操作系统的类型="+cmdResult);
			logger.info("正则表达式		\\s*uname\r\n(.*)\r\n");
			h.setOs(shell.parseInfoByRegex("\\s*uname\r\n(.*?)\r\n",cmdResult,1));
			if("AIX".equalsIgnoreCase(h.getOs())){
				Host.HostDetail hostDetail = new Host.HostDetail();
				h.setDetail(hostDetail);
				hostDetail.setOs(h.getOs());//主机详细信息页的操作系统类型
				//获取主机型号
				shell.executeCommands(new String[] { "uname -M" });
				cmdResult = shell.getResponse();
				
				logger.info("---主机型号---");
				logger.info(cmdResult);
				logger.info("正则表达式		\\s*uname -M\r\n(.*)\r\n");
				logger.info("主机型号="+shell.parseInfoByRegex("\\s*uname -M\r\n(.*)\r\n",cmdResult,1));
				hostDetail.setHostType(shell.parseInfoByRegex("\\s*uname -M\r\n(.*)\r\n",cmdResult,1));
				//获取主机名
				shell.executeCommands(new String[] { "uname -n" });
				cmdResult = shell.getResponse();
				
				logger.info("正则表达式		\\s*uname -n\r\n(.*)\r\n");
				logger.info("主机名="+shell.parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult,1));
				hostDetail.setHostName(shell.parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult,1));
				//获取系统版本号
				shell.executeCommands(new String[] { "uname -v" });
				cmdResult = shell.getResponse();
				
				logger.info("---系统版本号---");
				logger.info(cmdResult);
				logger.info("正则表达式		\\s*uname -v\r\n(.*)\r\n");
				 
				String version = shell.parseInfoByRegex("\\s*uname -v\r\n(.*)\r\n",cmdResult,1);
				
				shell.executeCommands(new String[] { "uname -r" });
				cmdResult = shell.getResponse();
				
				logger.info(cmdResult); 
				logger.info("正则表达式		\\s*uname -r\r\n(.*)\r\n");
				logger.info("系统版本号="+version+"."+shell.parseInfoByRegex("\\s*uname -r\r\n(.*)\r\n",cmdResult,1));
				hostDetail.setOsVersion(version+"."+shell.parseInfoByRegex("\\s*uname -r\r\n(.*)\r\n",cmdResult,1));
				
				
				//获取内存大小
				List<String> cmdsToExecute = new ArrayList<String>();
				
				ssh.setLinuxPromptRegex(ssh.getPromptRegexArrayByTemplateAndSpecificRegex(SSHClient.LINUX_PROMPT_REGEX_TEMPLATE,new String[]{"Full Core"}));
				
				cmdsToExecute.add("prtconf");
				
				try {
					cmdResult = ssh.execute(cmdsToExecute);
				} catch (ShellException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					cmdResult = "";///shell执行失败，结果默认为空串
				};
				
				logger.info("---内存大小---");
				logger.info(cmdResult);
				logger.info("正则表达式		[Gg]ood\\s+[Mm]emory\\s+[Ss]ize:\\s+(\\d+\\s+[MmBbKkGg]{0,2})");
				logger.info("内存大小="+shell.parseInfoByRegex("[Gg]ood\\s+[Mm]emory\\s+[Ss]ize:\\s+(\\d+\\s+[MmBbKkGg]{0,2})",cmdResult,1));
				hostDetail.setMemSize(shell.parseInfoByRegex("[Gg]ood\\s+[Mm]emory\\s+[Ss]ize:\\s+(\\d+\\s+[MmBbKkGg]{0,2})",cmdResult,1));
				
				//获取CPU个数
				logger.info("---CPU个数---");
			 
				logger.info("正则表达式		[Nn]umber\\s+[Oo]f\\s+[Pp]rocessors:\\s+(\\d+)");
				logger.info("CPU个数="+shell.parseInfoByRegex("[Nn]umber\\s+[Oo]f\\s+[Pp]rocessors:\\s+(\\d+)",cmdResult,1));
				hostDetail.setCPUNumber(shell.parseInfoByRegex("[Nn]umber\\s+[Oo]f\\s+[Pp]rocessors:\\s+(\\d+)",cmdResult,1));
				
				//获取CPU频率
				logger.info("---CPU频率---");
				logger.info("正则表达式		[Pp]rocessor\\s+[Cc]lock\\s+[Ss]peed:\\s+(\\d+\\s+[GgHhMmZz]{0,3})");
				logger.info("CPU频率="+shell.parseInfoByRegex("[Pp]rocessor\\s+[Cc]lock\\s+[Ss]peed:\\s+(\\d+\\s+[GgHhMmZz]{0,3})",cmdResult,1));
				hostDetail.setCPUClockSpeed(shell.parseInfoByRegex("[Pp]rocessor\\s+[Cc]lock\\s+[Ss]peed:\\s+(\\d+\\s+[GgHhMmZz]{0,3})",cmdResult,1));
				
				
				//获取CPU核数
				cmdsToExecute = new ArrayList<String>();
				cmdsToExecute.add("bindprocessor -q" );
				
				try {
					cmdResult =ssh.execute(cmdsToExecute);
				} catch (ShellException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					cmdResult = "";///shell执行失败，结果默认为空串
				}
				
				logger.info("---CPU核数---");
				logger.info("正则表达式		0\\s+(\\d+\\s*)+");
				logger.info("CPU核数="+shell.parseInfoByRegex("0\\s+(\\d+\\s*)+",cmdResult,1));
				hostDetail.setLogicalCPUNumber(Integer.parseInt(shell.parseInfoByRegex("0\\s+(\\d+\\s*)+",cmdResult,1).trim())+1+"");
				
				//是否有配置双机
				boolean isCluster = false;//默认没有配置双机
				shell.executeCommands(new String[] { "/usr/es/sbin/cluster/utilities/clshowsrv -v" });
				cmdResult = shell.getResponse();
				
				logger.info("---是否有配置双机---");
				logger.info(cmdResult);
				logger.info("正则表达式		0\\s+(\\d+\\s*)+");
				if(cmdResult.split("[\r\n]+").length>3?true:false){
					//配置有AIX自带的双机
					isCluster = true;
					hostDetail.setIsCluster("是");
					//获取双机虚地址
					shell.executeCommands(new String[] { "/usr/es/sbin/cluster/utilities/cllscf" });
					cmdResult = shell.getResponse();
					
					logger.info("---双机虚地址---");
					logger.info(cmdResult);
					logger.info("正则表达式		Service\\s+IP\\s+Label\\s+[\\d\\w]+:\\s+IP\\s+address:\\s+((\\d{1,3}\\.){3}\\d{1,3})");
					hostDetail.setClusterServiceIP(shell.parseInfoByRegex("Service\\s+IP\\s+Label\\s+[\\d\\w]+:\\s+IP\\s+address:\\s+((\\d{1,3}\\.){3}\\d{1,3})", cmdResult,1));
				}
				if(!isCluster){
					shell.executeCommands(new String[] { "hastatus -sum" });
					cmdResult = shell.getResponse();
					
					logger.info("---第三方双机---");
					logger.info(cmdResult);
					 
					//配置第三方双机,第三方双机采集不到虚地址
					if(cmdResult.split("[\r\n]+").length>3?true:false){
						isCluster = true;
						hostDetail.setIsCluster("是");
						hostDetail.setClusterServiceIP("非自带双机");
					}
				}
				if(!isCluster){
					//没有配置双机，也没有双机虚地址
					hostDetail.setIsCluster("否");
					hostDetail.setClusterServiceIP("NONE");
				}
				//是否负载均衡
				///正则匹配出Farm
				String  farm = shell.parseInfoByRegex("appdirector farm server table create (.*?) "+h.getIp(), allLoadBalancerFarmAndServerInfo.toString(),1);
				
				//负载均衡上的虚地址
				if(!"NONE".equals(farm)){
					hostDetail.setLoadBalancedVirtualIP(shell.parseInfoByRegex("appdirector l4-policy table create (.*?) (TCP|UDP) \\d{1,5} (\\d{1,3}\\.){3}\\d{1,3}\\\\\\s+ .*? -fn "+farm, allLoadBalancerFarmAndServerInfo.toString(),1));
					hostDetail.setIsLoadBalanced("是");
		    	    
				}else{
					hostDetail.setLoadBalancedVirtualIP("无");
					hostDetail.setIsLoadBalanced("否");
		    	    
				}
	    		
				//获取网卡信息
				shell.executeCommands(new String[] { "lsdev -Cc adapter | grep ent" });
				cmdResult = shell.getResponse();
				
				logger.info("---网卡信息---");
				logger.info(cmdResult);
				logger.info("正则表达式		^(ent\\d+)"); 
				String[] ents = cmdResult.split("[\r\n]+");
				List<Host.HostDetail.NetworkCard> cardList = new ArrayList<Host.HostDetail.NetworkCard>();
				///数组中第一个元素是输入的命令  最后一个元素是命令执行之后的提示符，过滤掉不予解析
				for(int i = 1,size = ents.length;i<size-1;i++){
					//提取网卡的名字
					Host.HostDetail.NetworkCard card = new Host.HostDetail.NetworkCard();
					card.setCardName(shell.parseInfoByRegex("^(ent\\d+)",ents[i],1));
					
					//提取网卡的类型（光口 or 电口）
					card.setIfType(ents[i].indexOf("-SX")== -1?"电口":"光口");//带有-SX为光口
					cardList.add(card);
				}
				  
				hostDetail.setCardList(cardList);
				
				
				//获取挂载点信息
				cmdsToExecute = new ArrayList<String>();
				cmdsToExecute.add("df -m" );
				try {
					cmdResult =ssh.execute(cmdsToExecute);
				} catch (ShellException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					cmdResult = "";///shell执行失败，结果默认为空串
				}
				
				logger.info("---挂载点信息---");
				logger.info(cmdResult);
				 
				String[] diskFSEntries = cmdResult.split("\n");
				///滤掉磁盘信息的表格头
				List<Host.HostDetail.FileSystem> fsList = new ArrayList();
				for(int i = 2,size = diskFSEntries.length-1;i<size;i++){
					String[] entry = diskFSEntries[i].split("\\s+");
					
					if(entry!=null && entry.length == 7){
						Host.HostDetail.FileSystem fs = new Host.HostDetail.FileSystem();
						
						fs.setMountOn(entry[6]);
						fs.setBlocks(entry[1]+" MB");
						fs.setUsed(entry[3]);
						
						fsList.add(fs);
						 
					}
					
				}
				hostDetail.setFsList(fsList);
				
				//检测是否安装了Oracle数据库

				List<Host.Database> dList = new ArrayList<Host.Database>();
				h.setdList(dList);
				shell.executeCommands(new String[] { "ps -ef|grep tnslsnr" });
				cmdResult = shell.getResponse();
				logger.info(cmdResult);
				 
				boolean isExistOracle = cmdResult.split("[\r\n]+").length >=4?true:false;
				 
				//安装有Oracle
				if(isExistOracle){
					Host.Database db = new Host.Database();
					dList.add(db);
					db.setType("Oracle");
					db.setIp(ip);
					//找到oracle用户的目录
					shell.executeCommands(new String[] { "cat /etc/passwd|grep oracle" });
					cmdResult = shell.getResponse();
					
					logger.info(cmdResult);
					logger.info("正则表达式		:/(.+):");
					String oracleUserDir = shell.parseInfoByRegex(":/(.+):",cmdResult,1);
					
					//找到oracle的安装目录
					shell.executeCommands(new String[] { "cat "+oracleUserDir+"/.profile" });
					cmdResult = shell.getResponse();
					
					logger.info(cmdResult);
					logger.info("正则表达式		ORACLE_HOME=([^\r\n]+)");
					String oracleHomeDir = shell.parseInfoByRegex("ORACLE_HOME=([^\r\n]+)",cmdResult,1);
					 
					oracleHomeDir = oracleHomeDir.indexOf("ORACLE_BASE")!=-1?oracleHomeDir.replaceAll("\\$ORACLE_BASE", shell.parseInfoByRegex("ORACLE_BASE=([^\r\n]+)",cmdResult,1)):oracleHomeDir;
					 
					db.setDeploymentDir(oracleHomeDir);
					//找到版本
					logger.info("---找到版本---");
					logger.info("正则表达式		/((\\d+\\.?)+\\d*)/");
					version = shell.parseInfoByRegex("/((\\d+\\.?)+\\d*)/|/(\\d+[g]?)/",oracleHomeDir,1);
					db.setVersion(version);
					//找到实例名
					 
					logger.info("正则表达式		ORACLE_SID=([^\r\n]+)");
					String oracleSid = shell.parseInfoByRegex("ORACLE_SID=([^\r\n]+)",cmdResult,1);
					 
					db.setDbName(oracleSid);
					
					//数据文件保存路径
					
					
					//数据文件列表
					shell.executeCommands(new String[] { "su - oracle","sqlplus / as sysdba"});
					cmdResult = shell.getResponse();
					
					logger.info(cmdResult);
					 
					
					shell.executeCommands(new String[] {"select file_name,bytes/1024/1024 ||'MB' as file_size from dba_data_files;"  });
					cmdResult = shell.getResponse();
					logger.info(cmdResult);
					logger.info("正则表达式		ORACLE_SID=([^\r\n]+)");
					 
					////数据文件大小 的正则\s+(\d+MB)\s+
					////数据文件位置的 正则\s+(/.*)\s+
					Pattern locationRegex = Pattern.compile("\\s+(/.*)\\s+");
					Pattern sizeRegex = Pattern.compile("\\s+(\\d+MB)\\s+");
					Matcher locationMatcher = locationRegex.matcher(cmdResult);
					Matcher sizeMatcher = sizeRegex.matcher(cmdResult);
					
					List<Host.Database.DataFile> dfList = new ArrayList<Host.Database.DataFile>();
					db.setDfList(dfList);
					while(locationMatcher.find()){
						Host.Database.DataFile dataFile = new Host.Database.DataFile();
						dataFile.setFileName(locationMatcher.group(1));
						if(sizeMatcher.find())
							dataFile.setFileSize(sizeMatcher.group(1));
						 
						dfList.add(dataFile);
					}
					 
					logger.info("正则表达式		\\s+(/.*)\\s+");
					logger.info("正则表达式		\\s+(\\d+MB)\\s+");
					//由于进入了sqlplus模式，在此断开连接，退出重新登录
					shell.disconnect();
					
					// 建立连接
					try {
						shell = new Shell(ip, SSH_PORT, jkUser, jkUserPassword);
					} catch (ShellException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				

				//weblogic中间件信息
				List<Host.Middleware> mList = new ArrayList<Host.Middleware>();
				h.setmList(mList);
				shell.executeCommands(new String[] { "" });
				shell.executeCommands(new String[] { "ps -ef|grep weblogic" });
				cmdResult = shell.getResponse();

				logger.info(cmdResult); 
				String[] lines = cmdResult.split("[\r\n]+");
				//存在weblogic
				if(lines.length>4){
					Host.Middleware mw = new Host.Middleware();
					mList.add(mw);
					mw.setType("WebLogic");
					mw.setIp(ip);
					//部署路径
					logger.info(cmdResult);
					logger.info("正则表达式		-Djava.security.policy=(/.+)/server/lib/weblogic.policy");
					String deploymentDir = shell.parseInfoByRegex("-Djava.security.policy=(/.+)/server/lib/weblogic.policy",cmdResult,1);
					String userProjectsDirSource = cmdResult;
					mw.setDeploymentDir(deploymentDir);
					//weblogic版本
					 
					logger.info("正则表达式		([\\d.]+)$");
					mw.setVersion(shell.parseInfoByRegex("([\\d.]+)$",deploymentDir,1));
					
					System.out.println(deploymentDir+"="+version);
					//JDK版本
					 
					logger.info("正则表达式		(/.+/bin/java)");
					shell.executeCommands(new String[] { shell.parseInfoByRegex("(/.+/bin/java)",cmdResult,1)+" -version" });
					cmdResult = shell.getResponse();
					
					logger.info(cmdResult);
					logger.info("正则表达式		java\\s+version\\s+\"([\\w.]+)\"");
					String jdkVersion = shell.parseInfoByRegex("java\\s+version\\s+\"([\\w.]+)\"",cmdResult,1);
					mw.setJdkVersion(jdkVersion);
					//采集 weblogic的应用列表
					mw.setAppList(collectWeblogicAppListForAIX(shell, userProjectsDirSource));
				}
			}else if("LINUX".equalsIgnoreCase(h.getOs())){
				
				
				Host.HostDetail hostDetail = new Host.HostDetail();
				h.setDetail(hostDetail);
				hostDetail.setOs(h.getOs());//主机详细信息页的操作系统类型
				
				
				
				//获取主机型号
				shell.executeCommands(new String[] { "dmidecode -s system-manufacturer" });//root用户登录
				cmdResult = shell.getResponse();
				
				logger.info(cmdResult);
				
				String manufacturer = cmdResult.split("[\r\n]+")[1].trim();
				
				shell.executeCommands(new String[] { "dmidecode -s system-product-name" });
				cmdResult = shell.getResponse();
				
				logger.info(cmdResult);
				 
				hostDetail.setHostType(manufacturer+" "+cmdResult.split("[\r\n]+")[1].trim());
				//获取主机名
				shell.executeCommands(new String[] { "uname -n" });
				cmdResult = shell.getResponse();
				
				logger.info(cmdResult);
				logger.info("正则表达式		\\s*uname -n\r\n(.*)\r\n");
				System.out.print(shell.parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult,1));
				hostDetail.setHostName(shell.parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult,1));
				//获取系统版本号
				shell.executeCommands(new String[] { "lsb_release -a |grep \"Description\"" });
				cmdResult = shell.getResponse();

				logger.info(cmdResult);
				logger.info("正则表达式		[Dd]escription:\\s+(.+)");
				hostDetail.setOsVersion(shell.parseInfoByRegex("[Dd]escription:\\s+(.+)",cmdResult,1));
				
				//CPU个数
				shell.executeCommands(new String[] { "cat /proc/cpuinfo |grep \"physical id\"|wc -l" });
				cmdResult = shell.getResponse();
				
				logger.info(cmdResult);
				logger.info("正则表达式		\\s+(\\d+)\\s+");
				hostDetail.setCPUNumber(shell.parseInfoByRegex("\\s+(\\d+)\\s+",cmdResult,1));
				
				//CPU核数
				shell.executeCommands(new String[] { "cat /proc/cpuinfo | grep \"cpu cores\"" });
				cmdResult = shell.getResponse();

				logger.info(cmdResult);
				logger.info("正则表达式		cpu\\s+cores\\s+:\\s+(\\d+)\\s*");
				hostDetail.setLogicalCPUNumber(parseLogicalCPUNumber("cpu\\s+cores\\s+:\\s+(\\d+)\\s*",cmdResult));
				
				//CPU主频
				shell.executeCommands(new String[] { "dmidecode -s processor-frequency" });
				cmdResult = shell.getResponse();

				logger.info(cmdResult);
				 
				hostDetail.setCPUClockSpeed(cmdResult.split("[\r\n]+").length>=1?cmdResult.split("[\r\n]+")[1]:"NONE");
				
				//内存大小
				shell.executeCommands(new String[] { "free -m" });
				cmdResult = shell.getResponse();
				
				logger.info(cmdResult);
				 
				hostDetail.setMemSize(cmdResult.split("[\r\n]+").length>=2?cmdResult.split("[\r\n]+")[2].trim().split("\\s+")[1].trim()+" MB":"NONE");
				
				//获取网卡信息
				
				shell.executeCommands(new String[] { "ifconfig -a | grep \"^eth\"" });
				cmdResult = shell.getResponse();
				
				logger.info(cmdResult);
				 
				String[] eths = cmdResult.split("[\r\n]+");
				List<Host.HostDetail.NetworkCard> cardList = new ArrayList<Host.HostDetail.NetworkCard>();
				if(eths.length>=3){
					for(int i = 1,size=eths.length-1;i<size;i++){
						
						String eth = shell.parseInfoByRegex("^(eth\\d+)",eths[i],1);
						shell.executeCommands(new String[]{"ethtool "+eth+"| grep \"Supported ports\""});
						cmdResult = shell.getResponse();
						
						logger.info(cmdResult);
						logger.info("正则表达式		Supported\\s+ports:\\s*\\[\\s*(\\w*)\\s*\\]");
						 
						String typeStr = shell.parseInfoByRegex("Supported\\s+ports:\\s*\\[\\s*(\\w*)\\s*\\]",cmdResult,1);
						String ifType = typeStr.indexOf("TP")!=-1?"电口":(typeStr.indexOf("FIBRE")!=-1?"光口":"未知");
						Host.HostDetail.NetworkCard card = new Host.HostDetail.NetworkCard();
						card.setCardName(eth);
						card.setIfType(ifType);
						cardList.add(card);
					}
					hostDetail.setCardList(cardList);
				}
				
				
				//获取挂载点信息
				shell.executeCommands(new String[] { "df -m" });
				cmdResult = shell.getResponse();
				
				logger.info(cmdResult);
				 
				String[] diskFSEntries = cmdResult.split("\n");
							//滤掉磁盘信息的表格头
				List<Host.HostDetail.FileSystem> fsList = new ArrayList();
				for(int i = 2,size = diskFSEntries.length-1;i<size;i++){
					String[] entry = diskFSEntries[i].split("\\s+");
					
					if(entry!=null && entry.length == 6){
						Host.HostDetail.FileSystem fs = new Host.HostDetail.FileSystem();
						
						fs.setMountOn(entry[5]);
						fs.setBlocks(entry[1]+" MB");
						fs.setUsed(entry[4]);
						
						fsList.add(fs);
						
					}
					
				}
				hostDetail.setFsList(fsList);
				
				
				//检测是否安装了Oracle数据库

				List<Host.Database> dList = new ArrayList<Host.Database>();
				h.setdList(dList);
				shell.executeCommands(new String[] { "ps -ef|grep tnslsnr" });
				cmdResult = shell.getResponse();
				logger.info(cmdResult);
				 
				boolean isExist = cmdResult.split("[\r\n]+").length >=4?true:false;
				
				//安装有Oracle
				if(isExist){
					Host.Database db = new Host.Database();
					dList.add(db);
					//
					db.setType("Oracle");
					db.setIp(ip);
				
					//找到oracle的安装目录
					 
					logger.info("正则表达式		(/.+)/bin/tnslsnr");
					String oracleHomeDir = shell.parseInfoByRegex("(/.+)/bin/tnslsnr",cmdResult,1);
					
					db.setDeploymentDir(oracleHomeDir);
					//找到版本
					 
					logger.info("正则表达式		/((\\d+\\.?)+\\d*)/");
					String version = shell.parseInfoByRegex("/((\\d+\\.?)+\\d*)/",oracleHomeDir,1);
					db.setVersion(version);
					//找到实例名
					shell.executeCommands(new String[] { "echo $ORACLE_SID" });
					cmdResult = shell.getResponse();
					
					logger.info(cmdResult);
					 
					String oracleSid = cmdResult.split("[\r\n]+").length>=1?cmdResult.split("[\r\n]+")[1]:"NONE";
					 
					db.setDbName(oracleSid);
					
					//找到数据库文件文件的目录
					
					shell.executeCommands(new String[] { "su - oracle","sqlplus / as sysdba"});
					cmdResult = shell.getResponse();

					logger.info(cmdResult);
					 
					shell.executeCommands(new String[] {"select file_name,bytes/1024/1024 ||'MB' as file_size from dba_data_files;"  });
					cmdResult = shell.getResponse();

					logger.info(cmdResult);
					 
					////数据文件大小 的正则\s+(\d+MB)\s+
					////数据文件位置的 正则\s+(/.*)\s+
					Pattern locationRegex = Pattern.compile("\\s+(/.*)\\s+");
					Pattern sizeRegex = Pattern.compile("\\s+(\\d+MB)\\s+");
					 
					logger.info("正则表达式		\\s+(/.*)\\s+"); 
					logger.info("正则表达式		\\s+(\\d+MB)\\s+");
					Matcher locationMatcher = locationRegex.matcher(cmdResult);
					Matcher sizeMatcher = sizeRegex.matcher(cmdResult);
					
					List<Host.Database.DataFile> dfList = new ArrayList<Host.Database.DataFile>();
					db.setDfList(dfList);
					while(locationMatcher.find()){
						Host.Database.DataFile dataFile = new Host.Database.DataFile();
						dataFile.setFileName(locationMatcher.group(1));
						if(sizeMatcher.find())
							dataFile.setFileSize(sizeMatcher.group(1));
						 
						dfList.add(dataFile);
					}
				}
				
				
				//weblogic中间件信息
				List<Host.Middleware> mList = new ArrayList<Host.Middleware>();
				h.setmList(mList);
				shell.executeCommands(new String[] { "ps -ef|grep weblogic" });
				cmdResult = shell.getResponse();

				logger.info(cmdResult);
				 
				String[] lines = cmdResult.split("[\r\n]+");
				//存在weblogic
				if(lines.length>4){
					Host.Middleware mw = new Host.Middleware();
					mList.add(mw);
					mw.setType("WebLogic");
					mw.setIp(ip);
					//部署路径

					logger.info(cmdResult);
					logger.info("正则表达式		-Djava.security.policy=(/.+)/server/lib/weblogic.policy");
					String deploymentDir = shell.parseInfoByRegex("-Djava.security.policy=(/.+)/server/lib/weblogic.policy",cmdResult,1);
					String userProjectsDirSource = cmdResult;
					mw.setDeploymentDir(deploymentDir);
					//weblogic版本
					logger.info("正则表达式		([\\d.]+)$");
					mw.setVersion(shell.parseInfoByRegex("([\\d.]+)$",deploymentDir,1));
				 
					//JDK版本
					
					shell.executeCommands(new String[] { shell.parseInfoByRegex("(/.+/bin/java)",cmdResult,1)+" -version" });
					cmdResult = shell.getResponse();
					
					logger.info(cmdResult);
					logger.info("正则表达式		java\\s+version\\s+\"([\\w.]+)\"");
					 
					String jdkVersion = shell.parseInfoByRegex("java\\s+version\\s+\"([\\w.]+)\"",cmdResult,1);
					mw.setJdkVersion(jdkVersion);
					
					mw.setAppList(collectWeblogicAppListForLinux(shell, userProjectsDirSource));
				}
			}else if("HP-UNIX".equalsIgnoreCase(h.getOs())){
				
			}
			System.out.println(h);
			shell.disconnect();

		}
    	HintMsg msg = new HintMsg(nowNum,maxNum,"采集完毕");
		DwrPageContext.run(JSONObject.fromObject(msg).toString());
    }
    
    /**
     * 获取CPU核数
     * @author HP
     * @param pattern
     * @param cmdResult
     * @return
     */
    public static String parseLogicalCPUNumber(final String pattern,final String cmdResult){
    	//获取
    	Matcher m = Pattern.compile(pattern).matcher(cmdResult);
    			int number = 0;
    			while(m.find()){
    				number += Integer.parseInt(m.group(1));
    				
    			}
    			return number+"";
    	    	
    }
    /**
     * 采用标准提示符匹配的正则和特殊正则来构造
     * @param template
     * @param specific
     * @return
     */
    public String[] getPromptRegexArrayByTemplateAndSpecificRegex(final String[] template,final String[] specific){
    	String[] regexArray = new String[template.length+specific.length];
    	System.arraycopy(template, 0, regexArray, 0, template.length);
    	System.arraycopy(specific, 0, regexArray, template.length, specific.length);
    	return regexArray;
    }
    /**
     * 采集weblogic的应用名字和部署路径
     * @param shell
     * @param userProjectsDirSource   user_projects的上一层目录
     * @return
     */
    public static List<Host.Middleware.App> collectWeblogicAppListForAIX(final Shell shell,final String userProjectsDirSource){

		
		//应用名称及其部署路径
		///找到weblogic中的应用domain 文件夹路径 层次 user_projects->domains->appName_domains
    	logger.info("---weblogic中的应用domain 文件夹路径 层次 user_projects->domains->appName_domains---");
		 logger.info("正则表达式		-Djava.security.policy=(/.+)/[\\w.]+/server/lib/weblogic.policy");
		
		 Set<String> appRootDirSet = shell.parseUserProjectSetByRegex("-Djava.security.policy=(/.+)/[\\w.]+/server/lib/weblogic.policy",userProjectsDirSource);
		logger.info("　weblogic中的应用domain 文件夹路径 层次＝"+appRootDirSet);
		
		Map<String,Set<String>> appDomainMap = new HashMap();//key是 appRootDir应用根目录
		for(String appRootDir:appRootDirSet){
			shell.executeCommands(new String[] {"ls " + appRootDir+"/user_projects/domains" });
			String cmdResult = shell.getResponse();
			
			logger.info(cmdResult);
			 
			String[] lines = cmdResult.split("[\r\n]+");
			 if(lines.length>2){//domains下面有多个应用domain
				Set<String> appDomainSet = new HashSet();
				 for(int i = 1,index = lines.length-1 ;i<index;i++ ){
					 String[] domains = lines[i].split("\\s+");
					 for(String domain:domains){
						 appDomainSet.add(domain);
					 }
						 
				 }
				 appDomainMap.put(appRootDir, appDomainSet);
			 }
			
			
		}
		///从每个应用配置文件config.xml中检索  应用名称（从<name></name>配置节中） 和部署路径
		List<Host.Middleware.App> appList = new ArrayList();
		appRootDirSet = appDomainMap.keySet();
		for(String appRootDir:appRootDirSet){
			 Set<String> appDomainSet = appDomainMap.get(appRootDir);
			 ///appName_domain与应用映射   版本10中config.xml文件位于appName_domain->config文件夹
			 for(String domain:appDomainSet){
				 //System.out.println(domain);
				 boolean isExistConfig = false;
				 	if(!isExistConfig){
					 	shell.executeCommands(new String[] {"cat " + appRootDir+"/user_projects/domains/"+domain+"/config/config.xml" });
						String cmdResult = shell.getResponse();
						
						logger.info(cmdResult);
						String[] lines = cmdResult.split("[\r\n]+");
						
						///weblogic10
						if(lines.length>4){    ///执行返回的结果大于4行的话，说明存在config.xml配置文件
							isExistConfig = true;
							Host.Middleware.App app = new Host.Middleware.App();
						 
							logger.info("正则表达式		<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>");
							logger.info("正则表达式		<app-deployment>[\\s\\S]*?<source-path>(.*)</source-path>[\\s\\S]*?</app-deployment>");
							///匹配应用的名字<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<source-path>(.*)</source-path>[\\s\\S]*?</app-deployment>",cmdResult,1));
							appList.add(app);
							logger.info(app);
						}
				 	}
					if(!isExistConfig){
						///weblogic8
						shell.executeCommands(new String[] {"cat " + appRootDir+"/user_projects/domains/"+domain+"/config.xml" });
						String cmdResult = shell.getResponse();
						String[] lines = cmdResult.split("[\r\n]+");
						if(lines.length>4){		///执行返回的结果大于4行的话，说明存在config.xml配置文件
							isExistConfig = true;
							Host.Middleware.App app = new Host.Middleware.App();
							
							logger.info(cmdResult);
							logger.info("正则表达式		<[Aa]pplication[\\s\\S]+?[Nn]ame=\"([\\S]+)\"");
							logger.info("正则表达式		<[Aa]pplication[\\s\\S]+?[Pp]ath=\"([\\S]+)\"");
							
							///匹配应用的名字<[Aa]pplication[\s\S]+?[Nn]ame="([\S]+)"  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Nn]ame=\"([\\S]+)\"",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Pp]ath=\"([\\S]+)\"",cmdResult,1));
							appList.add(app);
							logger.info(app);
						}
					}
					//System.out.println(cmdResult);
					
			 }
			
		 }
			
		 return appList;
		
    }
    
    /**
     * 
     * @param shell
     * @param userProjectsDirSource
     * @return
     */
    public static List<Host.Middleware.App> collectWeblogicAppListForLinux(final Shell shell,final String userProjectsDirSource){

	
		//应用名称及其部署路径
		///找到weblogic中的应用domain 文件夹路径 层次 user_projects->domains->appName_domains
		Set<String> appRootDirSet = shell.parseUserProjectSetByRegex("-Djava.security.policy=(/.+)/[\\w.]+/server/lib/weblogic.policy",userProjectsDirSource);
		
		logger.info(appRootDirSet);
		logger.info("正则表达式		-Djava.security.policy=(/.+)/[\\w.]+/server/lib/weblogic.policy");
		
		
		Map<String,Set<String>> appDomainMap = new HashMap();//key是 appRootDir应用根目录
		for(String appRootDir:appRootDirSet){
			shell.executeCommands(new String[] {"ls --color=never " + appRootDir+"/user_projects/domains" });
			String cmdResult = shell.getResponse();

			logger.info(cmdResult);
			 
			 String[] lines = cmdResult.split("[\r\n]+");
			 if(lines.length>2){//domains下面有多个应用domain
				Set<String> appDomainSet = new HashSet();
				 for(int i = 1,index = lines.length-1 ;i<index;i++ ){
					 String[] domains = lines[i].split("\\s+");
					 for(String domain:domains){
						 appDomainSet.add(domain);
					 }
						 
				 }
				 appDomainMap.put(appRootDir, appDomainSet);
			 }
			
			
		}
		///从每个应用配置文件config.xml中检索  应用名称（从<name></name>配置节中） 和部署路径
		List<Host.Middleware.App> appList = new ArrayList();
		appRootDirSet = appDomainMap.keySet();
		 for(String appRootDir:appRootDirSet){
			 Set<String> appDomainSet = appDomainMap.get(appRootDir);
			 ///appName_domain与应用映射   版本10中config.xml文件位于appName_domain->config文件夹
			 for(String domain:appDomainSet){
				 //System.out.println(domain);
				 boolean isExistConfig = false;
					 if(!isExistConfig){
						shell.executeCommands(new String[] {"cat " + appRootDir+"/user_projects/domains/"+domain+"/config/config.xml" });
						String cmdResult = shell.getResponse();
						
						logger.info(cmdResult);
						
						String[] lines = cmdResult.split("[\r\n]+");
					
						///weblogic10
						if(lines.length>4){    ///执行返回的结果大于4行的话，说明存在config.xml配置文件
							isExistConfig = true;
							Host.Middleware.App app = new Host.Middleware.App();
							
							
							logger.info("正则表达式		<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>");
							logger.info("正则表达式		<app-deployment>[\\s\\S]*?<source-path>(.*)</source-path>[\\s\\S]*?</app-deployment>");
							
							///匹配应用的名字<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<source-path>(.*)</source-path>[\\s\\S]*?</app-deployment>",cmdResult,1));
							appList.add(app);
							logger.info(app);
						}
					 }
					if(!isExistConfig){
						///weblogic8
						shell.executeCommands(new String[] {"cat " + appRootDir+"/user_projects/domains/"+domain+"/config.xml" });
						String cmdResult = shell.getResponse();
						
						logger.info(cmdResult);
						
						String[] lines = cmdResult.split("[\r\n]+");
						if(lines.length>4){		///执行返回的结果大于4行的话，说明存在config.xml配置文件
							isExistConfig = true;
							Host.Middleware.App app = new Host.Middleware.App();
							
							logger.info("正则表达式		<[Aa]pplication[\\s\\S]+?[Nn]ame=\"([\\S]+)\"");
							logger.info("正则表达式		<[Aa]pplication[\\s\\S]+?[Pp]ath=\"([\\S]+)\"");
							
							
							///匹配应用的名字<[Aa]pplication[\s\S]+?[Nn]ame="([\S]+)"  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Nn]ame=\"([\\S]+)\"",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Pp]ath=\"([\\S]+)\"",cmdResult,1));
							appList.add(app);
							logger.info(app);
						}
					}
					//System.out.println(cmdResult);
					
			 }
			
		 }
			
		 return appList;
    }
}