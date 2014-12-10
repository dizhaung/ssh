package ssh;

import host.FileManager;
import host.Host;
import host.Host.HostDetail;
import host.LoadBalancer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.MalformedPatternException;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;


import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.EofMatch;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

public class Shell {

    private static Logger logger = Logger.getLogger(Shell.class);
    
    private Session session;
    private ChannelShell channel;
    private  Expect4j expect = null;
    private static final long DEFAULT_TIME_OUT = 2*1000;
    public  StringBuffer buffer= null;
    
    public static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
    public static final String BACKSLASH_R = "\r";
    public static final String BACKSLASH_N = "\n";
    public static final String COLON_CHAR = ":";
    public static String ENTER_CHARACTER = BACKSLASH_R;
    public static final int SSH_PORT = 22;
    
   
    public static String[] errorMsg=new String[]{"could not acquire the config lock "};
    
    private String ip;
    private int port;
    private String user;
    private String password;
    /**
     * 模拟终端
     * @param ip
     * @param port
     * @param user
     * @param password
     * @throws ShellException  创建Shell失败，可能的原因1.网络失败  2.用户名或者密码错误	3输入输出流获取失败	4Expect创建失败
     */
    public Shell(String ip,int port,String user,String password) throws ShellException{
        this.ip=ip;
        this.port=port;
        this.user=user;
        this.password=password;
        expect = getExpect();
    }
    
    public void disconnect(){
        if(channel!=null){
            channel.disconnect();
        }
        if(session!=null){
            session.disconnect();
        }
    }
    

    public String getResponse(){
        return buffer.toString();
    }
    /**
     * 
     * @return
     * @throws Exception   连接主机失败
     */
    private Expect4j getExpect() throws ShellException{
        try {
            logger.debug(String.format("Start logging to %s@%s:%s",user,ip,port));
            JSch jsch = new JSch();
            session = jsch.getSession(user, ip, port);
            session.setPassword(password);
            Hashtable<String, String> config = new Hashtable<String, String>();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", 
                    "publickey,keyboard-interactive,password");
            session.setConfig(config);
            localUserInfo ui = new localUserInfo();
            session.setUserInfo(ui);
            session.connect();
            channel = (ChannelShell) session.openChannel("shell");
            Expect4j expect = new Expect4j(channel.getInputStream(), channel
                    .getOutputStream());
            expect.setDefaultTimeout(DEFAULT_TIME_OUT);
            channel.connect();
            logger.debug(String.format("Logging to %s@%s:%s successfully!",user,ip,port));
            return expect;
        } catch (JSchException ex) {
        	logger.error("连接到 "+ip+":"+port+"失败,请检查网络、用户名和密码!");
        	
            //ex.printStackTrace();
            throw new ShellException(ex.toString()+"	连接到 "+ip+":"+port+"失败,请检查网络、用户名和密码!",ex);
        } catch (IOException ex) {
			// TODO Auto-generated catch block
        	logger.error(ex.getMessage()+"	成功连接到 "+ip+":"+port+",但是获取JSCH channel输入/输出流失败!");
			 
			throw new ShellException(ex.toString()+"	成功连接到 "+ip+":"+port+",但是获取JSCH channel输入/输出流失败!",ex);
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			throw new ShellException(ex.toString()+"	Expect失败，错误未知",ex);
		}
    }

    public boolean executeCommands(String[] commands) {
    	buffer = new StringBuffer();
    	//主机连接应为用户名  密码不正确  或者  网络不通  而连接主机失败的情况下 
        if(expect==null){
            return false;
        }
       
        logger.debug("----------Running commands are listed as follows:----------");
        for(String command:commands){
        	logger.debug(command);
        }
        logger.debug("----------End----------");
        
        Closure closure = new Closure() {
            public void run(ExpectState expectState) throws Exception {
            	//匹配到模式串的情况下，例如：#等等待输入命令的提示符时，输出执行的命令和返回的信息
            	//System.out.println(expectState.getBuffer());
                buffer.append(expectState.getBuffer());// buffer is string
                                                        // buffer for appending
                                                        // output of executed
                                                        // command
              
               expectState.exp_continue();
                
            }
        };
        List<Match> lstPattern = new ArrayList<Match>();
        String[] regEx = linuxPromptRegEx;
        if (regEx != null && regEx.length > 0) {
            synchronized (regEx) {
                for (String regexElement : regEx) {// list of regx like, :>, />
                                                    // etc. it is possible
                                                    // command prompts of your
                                                    // remote machine
                    try {
                        RegExpMatch mat = new RegExpMatch(regexElement, closure);
                        lstPattern.add(mat);
                    } catch (MalformedPatternException e) {
                        return false;
                    } catch (Exception e) {
                        return false;
                    }
                }
               
                lstPattern.add(new TimeoutMatch(DEFAULT_TIME_OUT, new Closure() {
                    public void run(ExpectState state) {
                    }
                }));
            }
        }
        try {
            boolean isSuccess = true;
            for (String strCmd : commands){
                isSuccess = isSuccess(lstPattern, strCmd);
            }
            isSuccess = !checkResult(expect.expect(lstPattern));
            
            String response=buffer.toString().toLowerCase();
            for(String msg:errorMsg){
                if(response.indexOf(msg)>-1){
                    return false;
                }
            }
            
            return isSuccess;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean isSuccess(List<Match> objPattern, String strCommandPattern) {
        try {
            boolean isFailed = checkResult(expect.expect(objPattern));
            if (!isFailed) {
                expect.send(strCommandPattern);
                expect.send("\n");
                return true;
            }
            return false;
        } catch (MalformedPatternException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean checkResult(int intRetVal) {
        if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
            return true;
        }
        return false;
    }
    
    public static class localUserInfo implements UserInfo {
        String passwd;

        public String getPassword() {
            return passwd;
        }

        public boolean promptYesNo(String str) {
            return true;
        }

        public String getPassphrase() {
            return null;
        }

        public boolean promptPassphrase(String message) {
            return true;
        }

        public boolean promptPassword(String message) {
            return true;
        }

        public void showMessage(String message) {
            
        }
    }
   
    
    public static String[] linuxPromptRegEx = new String[] { "~]#", "~#", "#",
        ":~#", "/$", ">","SQL>","\\$"};

    public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException{
    
    	String userDir = System.getProperty("user.dir");
    	List<Host> list = Host.getHostList(FileManager.readFile("/hostConfig.txt"));
		
		for (Host h : list) {
			System.out.println(h);

			// 设置参数
			String ip = h.getIp();
			String jkUser = h.getJkUser();
			String jkUserPassword = h.getJkUserPassword();
			
			//当需要特别权限的情况下使用root用户
			String rootUser = h.getRootUser();
			String rootUserPassword = h.getRootUserPassword();
			
			// 建立连接
			Shell shell = null;
			
			try {
				shell = new Shell(ip, SSH_PORT, jkUser, jkUserPassword);
			} catch (ShellException e) {
				// TODO Auto-generated catch block
				logger.error("无法采集主机"+ip+"，连接下一个主机");
				e.printStackTrace();
				continue;
			}
		
		
				
			
			// 初始化服务器连接信息(特殊情况使用，每执行一个命令连接一次)
			SSHClient ssh = new SSHClient(ip, jkUser, jkUserPassword);
			
			shell.executeCommands(new String[] { "uname" });
		
			String cmdResult = shell.getResponse();
			//获取操作系统的类型
			
			System.out.println(cmdResult);
			h.setOs(parseInfoByRegex("\\s*uname\r\n(.*)\r\n",cmdResult,1));
			if("AIX".equalsIgnoreCase(h.getOs())){
				/*
				 //获取主机型号
				shell.executeCommands(new String[] { "uname -M" });
				cmdResult = shell.getResponse();
				
				System.out.print(parseInfoByRegex("\\s*uname -M\r\n(.*)\r\n",cmdResult));
				
				
				//获取主机名
				shell.executeCommands(new String[] { "uname -n" });
				cmdResult = shell.getResponse();
				
				System.out.print(parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult));
				
				//获取系统版本号
				
				shell.executeCommands(new String[] { "uname -v" });
				cmdResult = shell.getResponse();
				
				String version = parseInfoByRegex("\\s*uname -v\r\n(.*)\r\n",cmdResult);
				
				shell.executeCommands(new String[] { "uname -r" });
				cmdResult = shell.getResponse();
				
				System.out.print(version+"."+parseInfoByRegex("\\s*uname -r\r\n(.*)\r\n",cmdResult));
				*/
				//获取内存大小
				/*shell.executeCommands(new String[] { "prtconf |grep \"Good Memory Size:\"" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				System.out.print("内存大小"+parseInfoByRegex("\\s*prtconf \\|grep \"Good Memory Size:\"\r\n(.*)\r\n",cmdResult));
				*/
				/*//获取CPU个数
				shell.executeCommands(new String[] { "prtconf |grep \"Number Of Processors:\"" });
				cmdResult = shell.getResponse();
				
				System.out.print("CPU个数"+parseInfoByRegex("\\s*prtconf |grep \"Number Of Processors:\"\r\n(.*)\r\n",cmdResult));
				//获取CPU频率
				shell.executeCommands(new String[] { "prtconf |grep \"Processor Clock Speed:\"" });
				cmdResult = shell.getResponse();
				
				System.out.print("CPU频率"+parseInfoByRegex("\\s*prtconf |grep \"Processor Clock Speed:\"\r\n(.*)\r\n",cmdResult));
				
				//获取CPU核数
				shell.executeCommands(new String[] { "bindprocessor -q" });
				cmdResult = shell.getResponse();
				
				System.out.print("CPU核数"+parseInfoByRegex("\\s*bindprocessor -q\r\n(.*)\r\n",cmdResult));
				
				//获取CPU核数
				shell.executeCommands(new String[] { "bindprocessor -q" });
				cmdResult = shell.getResponse();
				
				System.out.print(parseInfoByRegex("\\s*bindprocessor -q\r\n(.*)\r\n",cmdResult));
				
				
				//获取网卡信息
				shell.executeCommands(new String[] { "lsdev -Cc adapter | grep ent" });
				cmdResult = shell.getResponse();
				String[] ents = cmdResult.split("[\r\n]+");
				///数组中第一个元素是输入的命令  最后一个元素是命令执行之后的提示符，过滤掉不予解析
				for(int i = 1,size = ents.length;i<size-1;i++){
					//提取网卡的名字
					System.out.print(parseInfoByRegex("^(ent\\d+)",ents[i]));
					System.out.println(ents[i].indexOf("-SX"));//带有-SX为光口
					//提取网卡的类型（光口 or 电口）
					
				}
				System.out.println(cmdResult);
				*/
				/*System.out.print(parseInfoByRegex("\\s*bindprocessor -q\r\n(.*)\r\n",cmdResult));*/
				/*//检测是否安装了Oracle数据库
				shell.executeCommands(new String[] { "ps -ef|grep tnslsnr" });
				cmdResult = shell.getResponse();
				boolean isExist = cmdResult.split("[\r\n]+").length >=4?true:false;
				
				//安装有Oracle
				if(isExist){
					//找到oracle用户的目录
					shell.executeCommands(new String[] { "cat /etc/passwd|grep oracle" });
					cmdResult = shell.getResponse();
					String oracleUserDir = parseInfoByRegex(":(/.+):",cmdResult);
					
					//找到oracle的安装目录
					shell.executeCommands(new String[] { "cat "+oracleUserDir+"/.profile" });
					cmdResult = shell.getResponse();
					
					String oracleHomeDir = parseInfoByRegex("ORACLE_HOME=([^\r\n]+)",cmdResult);
					System.out.println(oracleHomeDir);
					oracleHomeDir = oracleHomeDir.indexOf("ORACLE_BASE")!=-1?oracleHomeDir.replaceAll("\\$ORACLE_BASE", parseInfoByRegex("ORACLE_BASE=([^\r\n]+)",cmdResult)):oracleHomeDir;
					System.out.println(oracleHomeDir);
					
					//找到实例名
					String oracleSid = parseInfoByRegex("ORACLE_SID=([^\r\n]+)",cmdResult);
					System.out.println(oracleSid);
					
					System.out.println("tnslsnr="+cmdResult);
					//找到数据库文件文件的目录
					List<String> cmdsToExecute  = new ArrayList<String>();
					cmdsToExecute.add("su - oracle");
					cmdsToExecute.add("select * from dual;");
					cmdsToExecute.add("select file_name,bytes/1024/1024 ||'MB' as file_size from dba_data_files;" );
					cmdResult = ssh.execute(cmdsToExecute);
					shell.executeCommands(new String[] { "su - oracle","sqlplus / as sysdba"});
					cmdResult = shell.getResponse();
					System.out.println(cmdResult);
					
					shell.executeCommands(new String[] {"select file_name,bytes/1024/1024 ||'MB' as file_size from dba_data_files;"  });
					cmdResult = shell.getResponse();
					System.out.println(cmdResult);
					////数据文件大小 的正则\s+(\d+MB)\s+
					////数据文件位置的 正则\s+(/.*)\s+
					Pattern locationRegex = Pattern.compile("\\s+(/.*)\\s+");
					Pattern sizeRegex = Pattern.compile("\\s+(\\d+MB)\\s+");
					Matcher locationMatcher = locationRegex.matcher(cmdResult);
					Matcher sizeMatcher = sizeRegex.matcher(cmdResult);
					
					List<Host.Database.DataFile> dfList = new ArrayList<Host.Database.DataFile>();
					
					while(locationMatcher.find()){
						Host.Database.DataFile dataFile = new Host.Database.DataFile();
						dataFile.setFileName(locationMatcher.group(1));
						sizeMatcher.find();
						dataFile.setFileSize(sizeMatcher.group(1));
						System.out.println(dataFile);
						dfList.add(dataFile);
					}
				
				}*/
				/*//主机负载均衡
				///加载负载均衡配置
				List<LoadBalancer> loadBalancerList = LoadBalancer.getLoadBalancerList(FileManager.readFile("/loadBalancerConfig.txt"));
				System.out.println(loadBalancerList);
				
				///连接每个负载获取负载信息
				StringBuilder allLoadBalancerFarmAndServerInfo = new StringBuilder();
				for(LoadBalancer lb: loadBalancerList){
					SSHClient sshLoadBalancer = new SSHClient(lb.getIp(), lb.getUserName(), lb.getPassword());
					sshLoadBalancer.setLinuxPromptRegex(sshLoadBalancer.getPromptRegexArrayByTemplateAndSpecificRegex(SSHClient.LINUX_PROMPT_REGEX_TEMPLATE,new String[]{"User:","Password:"}));
					List<String> cmdsToExecute = new ArrayList<String>();
					cmdsToExecute.add("appdirector farm server table");
					
					cmdResult = ssh.execute(cmdsToExecute);
					allLoadBalancerFarmAndServerInfo.append(cmdResult);
				}*/
				
				shell.executeCommands(new String[] { "ps -ef|grep weblogic" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				String[] lines = cmdResult.split("[\r\n]+");
				//存在weblogic
				if(lines.length>4){
					//部署路径
					String deploymentDir = parseInfoByRegex("-Djava.security.policy=(/.+)/server/lib/weblogic.policy",cmdResult,1);
					String userProjectsDirSource = cmdResult;
					/*//weblogic版本
					String version = parseInfoByRegex("([\\d.]+)$",deploymentDir);
					
					System.out.println(deploymentDir+"="+version);
					//JDK版本
					shell.executeCommands(new String[] { parseInfoByRegex("(/.+/bin/java)",cmdResult)+" -version" });
					cmdResult = shell.getResponse();
					System.out.println(cmdResult);
					String jdkVersion = parseInfoByRegex("java\\s+version\\s+\"([\\w.]+)\"",cmdResult);*/
					
					//应用名称及其部署路径
					SSHClient.collectWeblogicAppListForAIX(shell, userProjectsDirSource); 
				}
				
			}else if("LINUX".equalsIgnoreCase(h.getOs())){
				/*//CPU个数
				shell.executeCommands(new String[] { "cat /proc/cpuinfo |grep \"physical id\"|wc -l" });
				cmdResult = shell.getResponse();
				
				System.out.print(parseInfoByRegex("\\s+(\\d+)\\s+",cmdResult));
				
				//CPU核数
				shell.executeCommands(new String[] { "cat /proc/cpuinfo | grep \"cpu cores\"" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				System.out.print(parseLogicalCPUNumber("cpu\\s+cores\\s+:\\s+(\\d+)\\s*",cmdResult));
				
				//CPU主频
				shell.executeCommands(new String[] { "dmidecode -s processor-frequency" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				System.out.print(cmdResult.split("[\r\n]+")[1]);
				
				//操作系统版本
				shell.executeCommands(new String[] { "lsb_release -a |grep \"Description\"" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				System.out.print(parseInfoByRegex("[Dd]escription:\\s+(.+)",cmdResult));
				
				//主机型号
				shell.executeCommands(new String[] { "dmidecode -s system-manufacturer" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				String manufacturer = cmdResult.split("[\r\n]+")[1].trim();
				
				shell.executeCommands(new String[] { "dmidecode -s system-product-name" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
			
				System.out.print(manufacturer+" "+cmdResult.split("[\r\n]+")[1].trim());
				
				//内存大小
				shell.executeCommands(new String[] { "free -m" });
				cmdResult = shell.getResponse();
				
				System.out.println(cmdResult.split("[\r\n]+")[2].trim().split("\\s+")[1].trim());
				
				
				//获取挂载点信息
				shell.executeCommands(new String[] { "df -m" });
				cmdResult = shell.getResponse();
				
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
						System.out.println(fs);
					}
					
				}
				*/
				/*//获取网卡信息
				shell.executeCommands(new String[] { "ifconfig -a | grep \"^eth\"" });
				cmdResult = shell.getResponse();
				String[] eths = cmdResult.split("[\r\n]+");
				for(int i = 1,size=eths.length-1;i<size;i++){
					
							String eth = parseInfoByRegex("^(eth\\d+)",eths[i],1);
					shell.executeCommands(new String[]{"ethtool "+eth+"| grep \"Supported ports\""});
					cmdResult = shell.getResponse();
					System.out.println(cmdResult);
					String typeStr = parseInfoByRegex("Supported\\s+ports:\\s*\\[\\s*(\\w*)\\s*\\]",cmdResult,1);
					String type = typeStr.indexOf("TP")!=-1?"电口":(typeStr.indexOf("FIBRE")!=-1?"光口":"未知");
					System.out.println(type);
				}*/
				
				shell.executeCommands(new String[] { "ps -ef|grep weblogic" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				String[] lines = cmdResult.split("[\r\n]+");
				//存在weblogic
				if(lines.length>4){
					//部署路径
					String deploymentDir = parseInfoByRegex("-Djava.security.policy=(/.+)/server/lib/weblogic.policy",cmdResult,1);
					String userProjectsDirSource = cmdResult;
					/*//weblogic版本
					String version = parseInfoByRegex("([\\d.]+)$",deploymentDir);
					
					System.out.println(deploymentDir+"="+version);
					//JDK版本
					shell.executeCommands(new String[] { parseInfoByRegex("(/.+/bin/java)",cmdResult)+" -version" });
					cmdResult = shell.getResponse();
					System.out.println(cmdResult);
					String jdkVersion = parseInfoByRegex("java\\s+version\\s+\"([\\w.]+)\"",cmdResult);*/
					
					//应用名称及其部署路径
					SSHClient.collectWeblogicAppListForLinux(shell, userProjectsDirSource);
					}
			}else if("HP-UNIX".equalsIgnoreCase(h.getOs())){
				
			}
			shell.disconnect();

		}
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
     * 使用pattern从cmdResult获取必要的信息，若提取不到返回NONE
     * @param pattern     提取必要信息的模式
     * @param cmdResult   shell命名执行后返回的结果
     * @return
     */
    public static String parseInfoByRegex(final String pattern,final String cmdResult,final int groupNum){
    	//获取操作系统的类型
		Matcher m = Pattern.compile(pattern ).matcher(cmdResult);
		if(m.find()){
			return m.group(groupNum);
	
		}else{
			return "NONE";
		}
    	
    }
    /**
     * 使用pattern从cmdResult获取应用根目录
     * @param pattern     提取必要信息的模式
     * @param cmdResult   shell命名执行后返回的结果
     * @return
     */
    public static Set<String> parseUserProjectSetByRegex(final String pattern,final String cmdResult){
    	Set<String> userProjects = new HashSet();
    	//获取操作系统的类型
		Matcher m = Pattern.compile(pattern).matcher(cmdResult);
		while(m.find()){
			userProjects.add( m.group(1));
	
		}
    	return userProjects;
    }
}
