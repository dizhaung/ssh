package ssh;

import host.FileManager;
import host.Host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.oro.text.regex.MalformedPatternException;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
 
import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
 
public class SSHClient {
 
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
     */
    public String execute(List<String> cmdsToExecute) {
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
 
        try {
        	/**
        	 * 每执行一条命令都重新连接服务器，异常的低效，为了兼容复杂的shell命令
        	 * 例如：prtconf |grep "Good Memory Size:"
        	 * 执行命令的错误提示：
        	 * expect4j.BlockingConsumer run
				信息: Found EOF to stop while loop
        	 */
        	
            expect = SSH();
            boolean isSuccess = true;
            for(String strCmd : lstCmds) {
                isSuccess = isSuccess(lstPattern,strCmd);
                if (!isSuccess) {
                    isSuccess = isSuccess(lstPattern,strCmd);
                }
            }
 
            checkResult(expect.expect(lstPattern));
        } catch (Exception ex) {
            ex.printStackTrace();
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
    /**
     *
     * @param hostname
     * @param username
     * @param password
     * @param port
     * @return
     * @throws Exception
     */
    private Expect4j SSH() throws Exception {
        JSch jsch = new JSch();
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
        Expect4j expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
        channel.connect();
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
     * 
     * @param list
     */
    public static void startCollect(List<Host> list){
    	for (Host h : list) {
			System.out.println(h);

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
			Shell shell = new Shell(ip, SSH_PORT, jkUser, jkUserPassword);
			shell.executeCommands(new String[] { "uname" });
			String cmdResult = shell.getResponse();
			//获取操作系统的类型
			System.out.println(cmdResult);
			h.setOs(shell.parseInfoByRegex("\\s*uname\r\n(.*)\r\n",cmdResult,1));
			if("AIX".equalsIgnoreCase(h.getOs())){
				Host.HostDetail hostDetail = new Host.HostDetail();
				h.setDetail(hostDetail);
				hostDetail.setOs(h.getOs());//主机详细信息页的操作系统类型
				//获取主机型号
				shell.executeCommands(new String[] { "uname -M" });
				cmdResult = shell.getResponse();
				
				System.out.print(shell.parseInfoByRegex("\\s*uname -M\r\n(.*)\r\n",cmdResult,1));
				hostDetail.setHostType(shell.parseInfoByRegex("\\s*uname -M\r\n(.*)\r\n",cmdResult,1));
				//获取主机名
				shell.executeCommands(new String[] { "uname -n" });
				cmdResult = shell.getResponse();
				
				System.out.print(shell.parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult,1));
				hostDetail.setHostName(shell.parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult,1));
				//获取系统版本号
				
				shell.executeCommands(new String[] { "uname -v" });
				cmdResult = shell.getResponse();
				
				String version = shell.parseInfoByRegex("\\s*uname -v\r\n(.*)\r\n",cmdResult,1);
				
				shell.executeCommands(new String[] { "uname -r" });
				cmdResult = shell.getResponse();
				
				System.out.print(version+"."+shell.parseInfoByRegex("\\s*uname -r\r\n(.*)\r\n",cmdResult,1));
				hostDetail.setOsVersion(version+"."+shell.parseInfoByRegex("\\s*uname -r\r\n(.*)\r\n",cmdResult,1));
				
				
				//获取内存大小
				List<String> cmdsToExecute = new ArrayList<String>();
				
				ssh.setLinuxPromptRegex(ssh.getPromptRegexArrayByTemplateAndSpecificRegex(SSHClient.LINUX_PROMPT_REGEX_TEMPLATE,new String[]{"Full Core"}));
				
				cmdsToExecute.add("prtconf");
				
				cmdResult = ssh.execute(cmdsToExecute);;
				System.out.println(shell.parseInfoByRegex("[Gg]ood\\s+[Mm]emory\\s+[Ss]ize:\\s+(\\d+\\s+[MmBbKkGg]{0,2})",cmdResult,1));
				hostDetail.setMemSize(shell.parseInfoByRegex("[Gg]ood\\s+[Mm]emory\\s+[Ss]ize:\\s+(\\d+\\s+[MmBbKkGg]{0,2})",cmdResult,1));
				
				//获取CPU个数
				
				System.out.print("CPU个数"+shell.parseInfoByRegex("[Nn]umber\\s+[Oo]f\\s+[Pp]rocessors:\\s+(\\d+)",cmdResult,1));
				hostDetail.setCPUNumber(shell.parseInfoByRegex("[Nn]umber\\s+[Oo]f\\s+[Pp]rocessors:\\s+(\\d+)",cmdResult,1));
				
				//获取CPU频率
				
				System.out.print("CPU频率"+shell.parseInfoByRegex("[Pp]rocessor\\s+[Cc]lock\\s+[Ss]peed:\\s+(\\d+\\s+[GgHhMmZz]{0,3})",cmdResult,1));
				hostDetail.setCPUClockSpeed(shell.parseInfoByRegex("[Pp]rocessor\\s+[Cc]lock\\s+[Ss]peed:\\s+(\\d+\\s+[GgHhMmZz]{0,3})",cmdResult,1));
				
				
				//获取CPU核数
				cmdsToExecute = new ArrayList<String>();
				cmdsToExecute.add("bindprocessor -q" );
				
				cmdResult =ssh.execute(cmdsToExecute);
				
				
				System.out.print("CPU核数"+shell.parseInfoByRegex("0\\s+(\\d+\\s*)+",cmdResult,1));
				hostDetail.setLogicalCPUNumber(Integer.parseInt(shell.parseInfoByRegex("0\\s+(\\d+\\s*)+",cmdResult,1).trim())+1+"");
				
				//是否有配置双机
				boolean isCluster = false;//默认没有配置双机
				shell.executeCommands(new String[] { "/usr/es/sbin/cluster/utilities/clshowsrv -v" });
				cmdResult = shell.getResponse();
				
				if(cmdResult.split("[\r\n]+").length>3?true:false){
					//配置有AIX自带的双机
					isCluster = true;
					hostDetail.setIsCluster("是");
					//获取双机虚地址
					shell.executeCommands(new String[] { "/usr/es/sbin/cluster/utilities/cllscf" });
					cmdResult = shell.getResponse();
					hostDetail.setClusterServiceIP(shell.parseInfoByRegex("Service\\s+IP\\s+Label\\s+[\\d\\w]+:\\s+IP\\s+address:\\s+((\\d{1,3}\\.){3}\\d{1,3})", cmdResult,1));
				}
				if(!isCluster){
					shell.executeCommands(new String[] { "hastatus -sum" });
					cmdResult = shell.getResponse();
					//配置第三方双机
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
				
				//获取网卡信息
				shell.executeCommands(new String[] { "lsdev -Cc adapter | grep ent" });
				cmdResult = shell.getResponse();
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
				cmdResult =ssh.execute(cmdsToExecute);
				
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
						System.out.println(fs);
					}
					
				}
				hostDetail.setFsList(fsList);
				
				//检测是否安装了Oracle数据库

				List<Host.Database> dList = new ArrayList<Host.Database>();
				h.setdList(dList);
				shell.executeCommands(new String[] { "ps -ef|grep tnslsnr" });
				cmdResult = shell.getResponse();
				boolean isExistOracle = cmdResult.split("[\r\n]+").length >=4?true:false;
				System.out.println("isExistOracle="+isExistOracle);
				//安装有Oracle
				if(isExistOracle){
					Host.Database db = new Host.Database();
					dList.add(db);
					db.setType("Oracle");
					db.setIp(ip);
					//找到oracle用户的目录
					shell.executeCommands(new String[] { "cat /etc/passwd|grep oracle" });
					cmdResult = shell.getResponse();
					String oracleUserDir = shell.parseInfoByRegex(":/(.+):",cmdResult,1);
					
					//找到oracle的安装目录
					shell.executeCommands(new String[] { "cat "+oracleUserDir+"/.profile" });
					cmdResult = shell.getResponse();
					
					String oracleHomeDir = shell.parseInfoByRegex("ORACLE_HOME=([^\r\n]+)",cmdResult,1);
					System.out.println(oracleHomeDir);
					oracleHomeDir = oracleHomeDir.indexOf("ORACLE_BASE")!=-1?oracleHomeDir.replaceAll("\\$ORACLE_BASE", shell.parseInfoByRegex("ORACLE_BASE=([^\r\n]+)",cmdResult,1)):oracleHomeDir;
					System.out.println(oracleHomeDir);
					db.setDeploymentDir(oracleHomeDir);
					//找到版本
					version = shell.parseInfoByRegex("/((\\d+\\.?)+\\d*)/",oracleHomeDir,1);
					db.setVersion(version);
					//找到实例名
					String oracleSid = shell.parseInfoByRegex("ORACLE_SID=([^\r\n]+)",cmdResult,1);
					System.out.println(oracleSid);
					db.setDbName(oracleSid);
					
					//数据文件保存路径
					
					
					//数据文件列表
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
					db.setDfList(dfList);
					while(locationMatcher.find()){
						Host.Database.DataFile dataFile = new Host.Database.DataFile();
						dataFile.setFileName(locationMatcher.group(1));
						sizeMatcher.find();
						dataFile.setFileSize(sizeMatcher.group(1));
						System.out.println(dataFile);
						dfList.add(dataFile);
					}
					//由于进入了sqlplus模式，在此断开连接，退出重新登录
					shell.disconnect();
					
					// 建立连接
					shell = new Shell(ip, SSH_PORT, jkUser, jkUserPassword);
					
				}
				
				

				//weblogic中间件信息
				List<Host.Middleware> mList = new ArrayList<Host.Middleware>();
				h.setmList(mList);
				shell.executeCommands(new String[] { "ps -ef|grep weblogic" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				String[] lines = cmdResult.split("[\r\n]+");
				//存在weblogic
				if(lines.length>4){
					Host.Middleware mw = new Host.Middleware();
					mList.add(mw);
					mw.setType("WebLogic");
					mw.setIp(ip);
					//部署路径
					String deploymentDir = shell.parseInfoByRegex("-Djava.security.policy=(/.+)/server/lib/weblogic.policy",cmdResult,1);
					String userProjectsDirSource = cmdResult;
					mw.setDeploymentDir(deploymentDir);
					//weblogic版本
					mw.setVersion(shell.parseInfoByRegex("([\\d.]+)$",deploymentDir,1));
					
					System.out.println(deploymentDir+"="+version);
					//JDK版本
					shell.executeCommands(new String[] { shell.parseInfoByRegex("(/.+/bin/java)",cmdResult,1)+" -version" });
					cmdResult = shell.getResponse();
					System.out.println(cmdResult);
					String jdkVersion = shell.parseInfoByRegex("java\\s+version\\s+\"([\\w.]+)\"",cmdResult,1);
					mw.setJdkVersion(jdkVersion);
					
					mw.setAppList(collectWeblogicAppListForAIX(shell, userProjectsDirSource));
				}
			}else if("LINUX".equalsIgnoreCase(h.getOs())){
				
				
				Host.HostDetail hostDetail = new Host.HostDetail();
				h.setDetail(hostDetail);
				hostDetail.setOs(h.getOs());//主机详细信息页的操作系统类型
				
				
				
				//获取主机型号
				shell.executeCommands(new String[] { "dmidecode -s system-manufacturer" });//root用户登录
				cmdResult = shell.getResponse();
				String manufacturer = cmdResult.split("[\r\n]+")[1].trim();
				
				shell.executeCommands(new String[] { "dmidecode -s system-product-name" });
				cmdResult = shell.getResponse();
				hostDetail.setHostType(manufacturer+" "+cmdResult.split("[\r\n]+")[1].trim());
				//获取主机名
				shell.executeCommands(new String[] { "uname -n" });
				cmdResult = shell.getResponse();
				
				System.out.print(shell.parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult,1));
				hostDetail.setHostName(shell.parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult,1));
				//获取系统版本号
				shell.executeCommands(new String[] { "lsb_release -a |grep \"Description\"" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				hostDetail.setOsVersion(shell.parseInfoByRegex("[Dd]escription:\\s+(.+)",cmdResult,1));
				
				//CPU个数
				shell.executeCommands(new String[] { "cat /proc/cpuinfo |grep \"physical id\"|wc -l" });
				cmdResult = shell.getResponse();
				
				hostDetail.setCPUNumber(shell.parseInfoByRegex("\\s+(\\d+)\\s+",cmdResult,1));
				
				//CPU核数
				shell.executeCommands(new String[] { "cat /proc/cpuinfo | grep \"cpu cores\"" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				hostDetail.setLogicalCPUNumber(parseLogicalCPUNumber("cpu\\s+cores\\s+:\\s+(\\d+)\\s*",cmdResult));
				
				//CPU主频
				shell.executeCommands(new String[] { "dmidecode -s processor-frequency" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				hostDetail.setCPUClockSpeed(cmdResult.split("[\r\n]+")[1]);
				
				//内存大小
				shell.executeCommands(new String[] { "free -m" });
				cmdResult = shell.getResponse();
				hostDetail.setMemSize(cmdResult.split("[\r\n]+")[2].trim().split("\\s+")[1].trim()+" MB");
				
				//获取网卡信息
				
				shell.executeCommands(new String[] { "ifconfig -a | grep \"^eth\"" });
				cmdResult = shell.getResponse();
				String[] eths = cmdResult.split("[\r\n]+");
				List<Host.HostDetail.NetworkCard> cardList = new ArrayList<Host.HostDetail.NetworkCard>();
				if(eths.length>=3){
					for(int i = 1,size=eths.length-1;i<size;i++){
						
						String eth = shell.parseInfoByRegex("^(eth\\d+)",eths[i],1);
						shell.executeCommands(new String[]{"ethtool "+eth+"| grep \"Supported ports\""});
						cmdResult = shell.getResponse();
						System.out.println(cmdResult);
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
				hostDetail.setFsList(fsList);
				
				
				//检测是否安装了Oracle数据库

				List<Host.Database> dList = new ArrayList<Host.Database>();
				h.setdList(dList);
				shell.executeCommands(new String[] { "ps -ef|grep tnslsnr" });
				cmdResult = shell.getResponse();
				boolean isExist = cmdResult.split("[\r\n]+").length >=4?true:false;
				
				//安装有Oracle
				if(isExist){
					Host.Database db = new Host.Database();
					dList.add(db);
					//
					db.setType("Oracle");
					db.setIp(ip);
				
					//找到oracle的安装目录
					String oracleHomeDir = shell.parseInfoByRegex("(/.+)/bin/tnslsnr",cmdResult,1);
					System.out.println(oracleHomeDir);
					db.setDeploymentDir(oracleHomeDir);
					//找到版本
					String version = shell.parseInfoByRegex("/((\\d+\\.?)+\\d*)/",oracleHomeDir,1);
					db.setVersion(version);
					//找到实例名
					shell.executeCommands(new String[] { "echo $ORACLE_SID" });
					cmdResult = shell.getResponse();
					String oracleSid = cmdResult.split("[\r\n]+")[1];
					System.out.println(oracleSid);
					db.setDbName(oracleSid);
					
					//找到数据库文件文件的目录
					
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
					db.setDfList(dfList);
					while(locationMatcher.find()){
						Host.Database.DataFile dataFile = new Host.Database.DataFile();
						dataFile.setFileName(locationMatcher.group(1));
						sizeMatcher.find();
						dataFile.setFileSize(sizeMatcher.group(1));
						System.out.println(dataFile);
						dfList.add(dataFile);
					}
				}
				
				
				//weblogic中间件信息
				List<Host.Middleware> mList = new ArrayList<Host.Middleware>();
				h.setmList(mList);
				shell.executeCommands(new String[] { "ps -ef|grep weblogic" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				String[] lines = cmdResult.split("[\r\n]+");
				//存在weblogic
				if(lines.length>4){
					Host.Middleware mw = new Host.Middleware();
					mList.add(mw);
					mw.setType("WebLogic");
					mw.setIp(ip);
					//部署路径
					String deploymentDir = shell.parseInfoByRegex("-Djava.security.policy=(/.+)/server/lib/weblogic.policy",cmdResult,1);
					String userProjectsDirSource = cmdResult;
					mw.setDeploymentDir(deploymentDir);
					//weblogic版本
					mw.setVersion(shell.parseInfoByRegex("([\\d.]+)$",deploymentDir,1));
				 
					//JDK版本
					shell.executeCommands(new String[] { shell.parseInfoByRegex("(/.+/bin/java)",cmdResult,1)+" -version" });
					cmdResult = shell.getResponse();
					System.out.println(cmdResult);
					String jdkVersion = shell.parseInfoByRegex("java\\s+version\\s+\"([\\w.]+)\"",cmdResult,1);
					mw.setJdkVersion(jdkVersion);
					
					mw.setAppList(collectWeblogicAppListForLinux(shell, userProjectsDirSource));
				}
			}else if("HP-UNIX".equalsIgnoreCase(h.getOs())){
				
			}
			System.out.println(h);
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
		Set<String> appRootDirSet = shell.parseUserProjectSetByRegex("-Djava.security.policy=(/.+)/[\\w.]+/server/lib/weblogic.policy",userProjectsDirSource);
		System.out.println(appRootDirSet);
		Map<String,Set<String>> appDomainMap = new HashMap();//key是 appRootDir应用根目录
		for(String appRootDir:appRootDirSet){
			shell.executeCommands(new String[] {"ls " + appRootDir+"/user_projects/domains" });
			String cmdResult = shell.getResponse();
			System.out.println(cmdResult);
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
						String[] lines = cmdResult.split("[\r\n]+");
						System.out.println(cmdResult);
						///weblogic10
						if(lines.length>4){    ///执行返回的结果大于4行的话，说明存在config.xml配置文件
							isExistConfig = true;
							Host.Middleware.App app = new Host.Middleware.App();
							
							///匹配应用的名字<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<source-path>(.*)</source-path>[\\s\\S]*?</app-deployment>",cmdResult,1));
							appList.add(app);
							System.out.println(app);
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
							///匹配应用的名字<[Aa]pplication[\s\S]+?[Nn]ame="([\S]+)"  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Nn]ame=\"([\\S]+)\"",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Pp]ath=\"([\\S]+)\"",cmdResult,1));
							appList.add(app);
							System.out.println(app);
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
		System.out.println(appRootDirSet);
		Map<String,Set<String>> appDomainMap = new HashMap();//key是 appRootDir应用根目录
		for(String appRootDir:appRootDirSet){
			shell.executeCommands(new String[] {"ls --color=never " + appRootDir+"/user_projects/domains" });
			String cmdResult = shell.getResponse();
			System.out.println(cmdResult);
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
						
						String[] lines = cmdResult.split("[\r\n]+");
					
						///weblogic10
						if(lines.length>4){    ///执行返回的结果大于4行的话，说明存在config.xml配置文件
							isExistConfig = true;
							Host.Middleware.App app = new Host.Middleware.App();
							
							///匹配应用的名字<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<source-path>(.*)</source-path>[\\s\\S]*?</app-deployment>",cmdResult,1));
							appList.add(app);
							System.out.println(app);
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
							///匹配应用的名字<[Aa]pplication[\s\S]+?[Nn]ame="([\S]+)"  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Nn]ame=\"([\\S]+)\"",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Pp]ath=\"([\\S]+)\"",cmdResult,1));
							appList.add(app);
							System.out.println(app);
						}
					}
					//System.out.println(cmdResult);
					
			 }
			
		 }
			
		 return appList;
    }
}