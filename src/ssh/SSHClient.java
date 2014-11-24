package ssh;

import host.FileManager;
import host.Host;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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
     */
   
       
    /**
     *
     * @param args
     */
    public static void main(String[] args) {
    	String userDir = System.getProperty("user.dir");
		List<Host> list = FileManager.getHostList(userDir+"/WebRoot/WEB-INF/classes/config.txt");
		
		startPoll(list);
    }
    /**
     * 
     * @param list
     */
    public static void startPoll(List<Host> list){
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
			h.setOs(parseInfoByRegex("\\s*uname\r\n(.*)\r\n",cmdResult));
			if("AIX".equalsIgnoreCase(h.getOs())){
				Host.HostDetail hostDetail = new Host.HostDetail();
				h.setDetail(hostDetail);
				hostDetail.setOs(h.getOs());//主机详细信息页的操作系统类型
				//获取主机型号
				shell.executeCommands(new String[] { "uname -M" });
				cmdResult = shell.getResponse();
				
				System.out.print(parseInfoByRegex("\\s*uname -M\r\n(.*)\r\n",cmdResult));
				hostDetail.setHostType(parseInfoByRegex("\\s*uname -M\r\n(.*)\r\n",cmdResult));
				//获取主机名
				shell.executeCommands(new String[] { "uname -n" });
				cmdResult = shell.getResponse();
				
				System.out.print(parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult));
				hostDetail.setHostName(parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult));
				//获取系统版本号
				
				shell.executeCommands(new String[] { "uname -v" });
				cmdResult = shell.getResponse();
				
				String version = parseInfoByRegex("\\s*uname -v\r\n(.*)\r\n",cmdResult);
				
				shell.executeCommands(new String[] { "uname -r" });
				cmdResult = shell.getResponse();
				
				System.out.print(version+"."+parseInfoByRegex("\\s*uname -r\r\n(.*)\r\n",cmdResult));
				hostDetail.setOsVersion(version+"."+parseInfoByRegex("\\s*uname -r\r\n(.*)\r\n",cmdResult));
				
				
				//获取内存大小
				List<String> cmdsToExecute = new ArrayList<String>();
				
				ssh.setLinuxPromptRegex(ssh.getPromptRegexArrayByTemplateAndSpecificRegex(SSHClient.LINUX_PROMPT_REGEX_TEMPLATE,new String[]{"Full Core"}));
				
				cmdsToExecute.add("prtconf");
				
				cmdResult = ssh.execute(cmdsToExecute);;
				System.out.println(parseInfoByRegex("[Gg]ood\\s+[Mm]emory\\s+[Ss]ize:\\s+(\\d+\\s+[MmBbKkGg]{0,2})",cmdResult));
				hostDetail.setMemSize(parseInfoByRegex("[Gg]ood\\s+[Mm]emory\\s+[Ss]ize:\\s+(\\d+\\s+[MmBbKkGg]{0,2})",cmdResult));
				
				//获取CPU个数
				
				System.out.print("CPU个数"+parseInfoByRegex("[Nn]umber\\s+[Oo]f\\s+[Pp]rocessors:\\s+(\\d+)",cmdResult));
				hostDetail.setCPUNumber(parseInfoByRegex("[Nn]umber\\s+[Oo]f\\s+[Pp]rocessors:\\s+(\\d+)",cmdResult));
				
				//获取CPU频率
				
				System.out.print("CPU频率"+parseInfoByRegex("[Pp]rocessor\\s+[Cc]lock\\s+[Ss]peed:\\s+(\\d+\\s+[GgHhMmZz]{0,3})",cmdResult));
				hostDetail.setCPUClockSpeed(parseInfoByRegex("[Pp]rocessor\\s+[Cc]lock\\s+[Ss]peed:\\s+(\\d+\\s+[GgHhMmZz]{0,3})",cmdResult));
				
				
				//获取CPU核数
				cmdsToExecute = new ArrayList<String>();
				cmdsToExecute.add("bindprocessor -q" );
				
				cmdResult =ssh.execute(cmdsToExecute);
				
				
				System.out.print("CPU核数"+parseInfoByRegex("0\\s+(\\d+\\s*)+",cmdResult));
				hostDetail.setLogicalCPUNumber(Integer.parseInt(parseInfoByRegex("0\\s+(\\d+\\s*)+",cmdResult).trim())+1+"");
				
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
					hostDetail.setClusterServiceIP(parseInfoByRegex("Service\\s+IP\\s+Label\\s+[\\d\\w]+:\\s+IP\\s+address:\\s+((\\d{1,3}\\.){3}\\d{1,3})", cmdResult));
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
					card.setCardName(parseInfoByRegex("^(ent\\d+)",ents[i]));
					
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
				//滤掉磁盘信息的表格头
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
				boolean isExist = cmdResult.split("[\r\n]+").length >=4?true:false;
				
				//安装有Oracle
				if(isExist){
					Host.Database db = new Host.Database();
					dList.add(db);
					db.setType("Oracle");
					db.setIp(ip);
					//找到oracle用户的目录
					shell.executeCommands(new String[] { "cat /etc/passwd|grep oracle" });
					cmdResult = shell.getResponse();
					String oracleUserDir = parseInfoByRegex(":/(.+):",cmdResult);
					
					//找到oracle的安装目录
					shell.executeCommands(new String[] { "cat "+oracleUserDir+"/.profile" });
					cmdResult = shell.getResponse();
					
					String oracleHomeDir = parseInfoByRegex("ORACLE_HOME=([^\r\n]+)",cmdResult);
					System.out.println(oracleHomeDir);
					oracleHomeDir = oracleHomeDir.indexOf("ORACLE_BASE")!=-1?oracleHomeDir.replaceAll("\\$ORACLE_BASE", parseInfoByRegex("ORACLE_BASE=([^\r\n]+)",cmdResult)):oracleHomeDir;
					System.out.println(oracleHomeDir);
					db.setDeploymentDir(oracleHomeDir);
					//找到版本
					version = parseInfoByRegex("/((\\d+\\.?)+\\d*)/",oracleHomeDir);
					db.setVersion(version);
					//找到实例名
					String oracleSid = parseInfoByRegex("ORACLE_SID=([^\r\n]+)",cmdResult);
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
					//weblogic中间件信息
					
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
				
				System.out.print(parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult));
				hostDetail.setHostName(parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult));
				//获取系统版本号
				shell.executeCommands(new String[] { "lsb_release -a |grep \"Description\"" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				hostDetail.setOsVersion(parseInfoByRegex("[Dd]escription:\\s+(.+)",cmdResult));
				
				//CPU个数
				shell.executeCommands(new String[] { "cat /proc/cpuinfo |grep \"physical id\"|wc -l" });
				cmdResult = shell.getResponse();
				
				hostDetail.setCPUNumber(parseInfoByRegex("\\s+(\\d+)\\s+",cmdResult));
				
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
						
						String eth = parseInfoByRegex("^(eth\\d+)",eths[i]);
						shell.executeCommands(new String[]{"ethtool "+eth+"| grep \"Supported ports\""});
						cmdResult = shell.getResponse();
						System.out.println(cmdResult);
						String typeStr = parseInfoByRegex("Supported\\s+ports:\\s*\\[\\s*(\\w*)\\s*\\]",cmdResult);
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
					String oracleHomeDir = parseInfoByRegex("(/.+)/bin/tnslsnr",cmdResult);
					System.out.println(oracleHomeDir);
					db.setDeploymentDir(oracleHomeDir);
					//找到版本
					String version = parseInfoByRegex("/((\\d+\\.?)+\\d*)/",oracleHomeDir);
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
    private String[] getPromptRegexArrayByTemplateAndSpecificRegex(final String[] template,final String[] specific){
    	String[] regexArray = new String[template.length+specific.length];
    	System.arraycopy(template, 0, regexArray, 0, template.length);
    	System.arraycopy(specific, 0, regexArray, template.length, specific.length);
    	return regexArray;
    }
    /**
     * 使用pattern从cmdResult获取必要的信息，若提取不到返回NONE
     * @param pattern     提取必要信息的模式
     * @param cmdResult   shell命名执行后返回的结果
     * @return
     */
    public static String parseInfoByRegex(final String pattern,final String cmdResult){
    	//获取操作系统的类型
		Matcher m = Pattern.compile(pattern).matcher(cmdResult);
		if(m.find()){
			return m.group(1);
	
		}else{
			return "NONE";
		}
    	
    }
}