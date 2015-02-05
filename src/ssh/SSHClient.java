package ssh;

import host.FileManager;
import host.Host;
import host.Host.Database;
import host.Host.Database.DataFile;
import host.Host.Middleware.App;
import host.HostBase;
import host.LoadBalancer;
import host.TinyHost;
import host.command.CollectCommand;
import host.database.db2.Tablespace;
import host.middleware.tomcat.Service;
import host.middleware.tomcat.Service.Connector;
import host.middleware.tomcat.Service.Engine;
import host.middleware.tomcat.Service.HttpConnector;
import host.middleware.tomcat.Service.Engine.Host.Context;



import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oro.text.regex.MalformedPatternException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import ssh.collect.AixCollector;

import collect.CollectedResource;
import collect.dwr.DwrPageContext;
import collect.model.HintMsg;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import constants.Format;
import constants.Unit;
import constants.regex.Regex;
import constants.regex.RegexEntity;
import constants.regex.Regex.CommonRegex;
 
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
    private static final String ENTER_CHARACTER = "\r";
    private static final int SSH_PORT = 22;
    private List<String> lstCmds = new ArrayList<String>();
    public static final String[] COMMAND_LINE_PROMPT_REGEX_TEMPLATE = new String[] { "~]#", "~#", "#",
        ":~#", "/$", ">","\\$","SQL>"};//加入\\$匹配登录后命令提示符为\$的情况
    private String[] commandLinePromptRegex = null;
    private Expect4j expect = null;
    private StringBuilder buffer = null;
    private String userName;
    private String password;
    private String host;
 
    public void setCommandLinePromptRegex(String[] commandLinePromptRegex){
    	this.commandLinePromptRegex = commandLinePromptRegex;
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
    	if(commandLinePromptRegex == null){
    		commandLinePromptRegex = COMMAND_LINE_PROMPT_REGEX_TEMPLATE;
    	}
        this.lstCmds = cmdsToExecute;
       
        Closure closure = new Closure() {
            public void run(ExpectState expectState) throws Exception {
            	 buffer = new StringBuilder();
                buffer.append(expectState.getBuffer());
            }
        };
        List<Match> lstPattern =  new ArrayList<Match>();
        for (String regexElement : commandLinePromptRegex) {
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
			commandLinePromptRegex = null;
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
     * 应用端口对应的farm和虚地址
     * @author HP
     *
     */
    private static class PortLoadConfig{
    	private String port;
    	private String farm;
    	private String serviceIp;
    	private String servicePort;
    	
    	
		@Override
		public String toString() {
			return "PortLoadConfig [port=" + port + ", farm=" + farm
					+ ", serviceIp=" + serviceIp + ", servicePort="
					+ servicePort + "]";
		}
		public String getServicePort() {
			return servicePort;
		}
		public void setServicePort(String servicePort) {
			this.servicePort = servicePort;
		}
		public String getPort() {
			return port;
		}
		public void setPort(String port) {
			this.port = port;
		}
		public String getFarm() {
			return farm;
		}
		public void setFarm(String farm) {
			this.farm = farm;
		}
		public String getServiceIp() {
			return serviceIp;
		}
		public void setServiceIp(String serviceIp) {
			this.serviceIp = serviceIp;
		}
    	
    	
    }
    
    
    private static void collectHostDetailForLinux(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){

		
		Host.HostDetail hostDetail = new Host.HostDetail();
		h.setDetail(hostDetail);
		hostDetail.setOs(h.getOs());//主机详细信息页的操作系统类型
		
		
		
		//获取主机型号
		shell.executeCommands(new String[] { "dmidecode -s system-manufacturer" });//root用户登录
		String cmdResult = shell.getResponse();
		
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
		
    }
    
    static void collectOracleForLinux(Shell shell,final Host h){
    	//检测是否安装了Oracle数据库
    	shell.executeCommands(new String[] { "ps -ef|grep tnslsnr" });
		String cmdResult = shell.getResponse();
		logger.info(cmdResult);
		 
		boolean isExist = cmdResult.split("[\r\n]+").length >=4?true:false;
		
		//安装有Oracle
		if(isExist){
			Host.Database db = new Host.Database();
			h.addDatabase(db);
			//
			db.setType("Oracle");
			db.setIp(h.getIp());
		
			//找到oracle的安装目录
			 
			logger.info("正则表达式		(/.+)/bin/tnslsnr");
			String oracleHomeDir = shell.parseInfoByRegex("(/.+)/bin/tnslsnr",cmdResult,1);
			
			db.setDeploymentDir(oracleHomeDir);
			
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
			Pattern sizeRegex = Pattern.compile("\\s+(\\d+)MB\\s+");
			 
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
			
			//找到版本
			logger.info("---找到版本---");
			shell.executeCommands(new String[] {"select version from v$instance;"  });
			cmdResult = shell.getResponse();
			logger.info(cmdResult);
			logger.info("正则表达式		((\\d+\\.?)+\\d*)");
			String version = shell.parseInfoByRegex("((\\d+\\.?)+\\d*)",cmdResult,1);
			db.setVersion(version);
			//由于进入了sqlplus模式，在此断开连接，退出重新登录
			shell.executeCommands(new String[] {"exit;"  });
			cmdResult = shell.getResponse();
			logger.info(h.getIp()+"	退出SQLPlus	"+cmdResult);
		}
		
		
    }
    
    
    static  void collectWeblogicForLinux(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){
    	//weblogic中间件信息
		 
    			shell.executeCommands(new String[] { "ps -ef|grep weblogic" });
    			String cmdResult = shell.getResponse();

    			logger.info(cmdResult);
    			 
    			String[] lines = cmdResult.split("[\r\n]+");
    			//存在weblogic
    			if(lines.length>4){
    				Host.Middleware mw = new Host.Middleware();
    				h.addMiddleware(mw);
    				mw.setType("WebLogic");
    				mw.setIp(h.getIp());
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
    				List<App> appList = searchServiceIpAndPortForEachOf(collectWeblogicAppListForLinux(shell, userProjectsDirSource,h), portListFromLoad);
    				mw.setAppList(appList);
    			}
    		
    }
    
    static void collectTomcatForLinux(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){

		shell.executeCommands(new String[] { "ps -ef|grep  org.apache.catalina.startup.Bootstrap" });
		String cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+cmdResult);
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		
		if(lines.length > 3){//安装有Tomcat.
			
			Matcher catalinaHomeMatcher = Pattern.compile(Regex.LinuxRegex.TOMCAT_CATALINA_HOME.toString()).matcher(cmdResult);
			Set<String> multipulInstanceCatalinaHomeSet = new HashSet();
			while(catalinaHomeMatcher.find()){
				String catalinaHome = catalinaHomeMatcher.group(1);
				multipulInstanceCatalinaHomeSet.add(catalinaHome);
			}
			
			logger.info(h.getIp()+"	可能有多个Tomcat实例同时运行	"+multipulInstanceCatalinaHomeSet);
			for(String catalinaHome:multipulInstanceCatalinaHomeSet){
				Host.Middleware mw = new Host.Middleware();
				h.addMiddleware(mw);
				h.setIp(h.getIp());
				//tomcat部署路径
				mw.setType("Tomcat");
				String deploymentDir = catalinaHome;
				mw.setDeploymentDir(deploymentDir);
				
				//tomcat版本号
				shell.executeCommands(new String[] { "cd "+deploymentDir+"/bin","./version.sh" });
				cmdResult = shell.getResponse();
				logger.info(h.getIp()+cmdResult);
				mw.setVersion(shell.parseInfoByRegex(Regex.LinuxRegex.TOMCAT_VERSION, cmdResult, 1)); 
				
				//JDK版本
				String jre_home = shell.parseInfoByRegex(Regex.LinuxRegex.TOMCAT_JRE_HOME, cmdResult, 1);
				shell.executeCommands(new String[] { jre_home+"/bin/java -version" });
				cmdResult = shell.getResponse();
				
				logger.info(h.getIp()+cmdResult);
				mw.setJdkVersion(shell.parseInfoByRegex(Regex.LinuxRegex.JDK_VERSION, cmdResult, 1));
				
				logger.info(mw);
				//tomcat应用列表
				;
				List<App> appList = searchServiceIpAndPortForEachOf(collectAppListForTomcat(shell,deploymentDir,h),portListFromLoad);
				
				
				mw.setAppList(appList);
				
			}
			
		}
    }
    static String reverseTomcatAppPathToAppName(final String path){
    	if("/".equals(path)){
    		return "ROOT";
    	}
    	return path.replaceAll("^/", "");
    }
    static List<App> collectAppListForTomcat(final Shell shell,final String catalinaHome, final Host h){
    	shell.executeCommands(new String[] { "cat "+catalinaHome+"/conf/server.xml" });
		String cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+"	解析前	"+cmdResult);
		String serverDotXmlText = parseValidXmlTextFrom(cmdResult);
		logger.info(h.getIp()+"	解析后	"+serverDotXmlText);
    	Document serverDotXmlDoc = null;
		try {
			serverDotXmlDoc = DocumentHelper.parseText(serverDotXmlText);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
			logger.info("server.xml文件中内容格式不符合xml规范");
			return Collections.emptyList();
		}
    	Element serverNode = serverDotXmlDoc.getRootElement();
	
		List<Element> serviceNodeList = serverNode.elements("Service");
		Set<Service> serviceSet = new HashSet();
		for(Element serviceNode:serviceNodeList){
			Service service = new Service();
			serviceSet.add(service);
			service.setName(serviceNode.attributeValue("name"));
			
			List<Element> connectorNodeList = serviceNode.elements("Connector");
			for(Element connectorNode:connectorNodeList){
				logger.info(connectorNode);
				String connectorProtocolAttr = connectorNode.attributeValue("protocol");
				if(Pattern.compile("HTTP",Pattern.CASE_INSENSITIVE).matcher(connectorProtocolAttr).find()){
					String connectorSslEnabledAttr = connectorNode.attributeValue("SSLEnabled");
					if(connectorSslEnabledAttr == null){
						Service.Connector httpConnector = service.new HttpConnector();
						service.addConnector(httpConnector);
						httpConnector.setPort(connectorNode.attributeValue("port"));
						httpConnector.setProtocol(connectorProtocolAttr);
					}
				}
			}
			
			List<Element> engineNodeList = serviceNode.elements("Engine");
			for(Element engineNode:engineNodeList){
				Service.Engine engine = service.new Engine(); 
				engine.setDefaultHost(engineNode.attributeValue("defaultHost"));
				engine.setName(engineNode.attributeValue("name"));
				service.addEngine(engine);
				
				List<Element> hostNodeList = engineNode.elements("Host");
				for(Element hostNode:hostNodeList){
					Service.Engine.Host host = engine.new Host();
					engine.addHost(host);
					host.setName(hostNode.attributeValue("name"));
					host.setAppBase(hostNode.attributeValue("appBase"));
					
					List<Element> contextNodeList = hostNode.elements("Context");
					for(Element contextNode:contextNodeList){
						Service.Engine.Host.Context context = host.new Context();
						host.addContext(context);
						context.setPath(reverseTomcatAppPathToAppName(hostNode.attributeValue("path")));
						context.setDocBase(hostNode.attributeValue("docBase"));
					}
					
				}
				
			}
			
			
			
			
		}
		
		logger.info(serviceSet);
		/**
		 * 1Context会在appBase下
		 * 2在$CATALINA_BASE/conf/[enginename]/[hostname]/appName.xml中
		 * 3在appName/META-INF/context.xml  中
		 * 4在server.xml Host元素中
		 */
		
		/**
		 * 1server.xml Host元素中的Context已经被解析出来,为了便于处理，context path去掉最前面的  / 然后作为appname
		 * 2查找$CATALINA_BASE/APPBase下的文件夹，作为appName和$CATALINA_BASE/APPBase/appName部署路径
		 * 3APPBASE路径下，appName/META-INF/context.xml文件会被复制到$CATALINA_BASE/conf/[enginename]/[hostname]/下
		 * 并被命名为appName.xml文件
		 * 这样，通过$CATALINA_BASE/conf/[enginename]/[hostname]/下appName.xml解析，矫正appName/META-INF/context.xml的docBase
		 */
		
		for(Iterator<Service> serviceIt = serviceSet.iterator();serviceIt.hasNext(); ){
			Service service = serviceIt.next();
			
			Set<Engine> engineSet = service.getEngineSet();
			
			for(Iterator<Engine> engineIt = engineSet.iterator();engineIt.hasNext(); ){
				Engine engine = engineIt.next();
				String engineName = engine.getName();
				Set<host.middleware.tomcat.Service.Engine.Host> hostSet = engine.getHostSet();
				for(Iterator<host.middleware.tomcat.Service.Engine.Host> hostIt = hostSet.iterator();hostIt.hasNext();){
					host.middleware.tomcat.Service.Engine.Host  host = hostIt.next();
					String hostDir = catalinaHome+"/"+host.getAppBase();
					if(existDirectory(hostDir,shell)){
						shell.executeCommands(new String[] { "ls --color=never -l|grep ^d" });
						cmdResult = shell.getResponse();
						logger.info(cmdResult);
						String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
						if(lines.length > 2){
							lines = Arrays.copyOfRange(lines, 1, lines.length-1);
							
							for(int i = 0,size = lines.length;i<size;i++){
								String[] lineFragments = lines[i].split(Regex.CommonRegex.BLANK_DELIMITER.toString());
								lines[i] = lineFragments[lineFragments.length-1];
							}
							String[] appNameArray = lines;

							
							for(String appName:appNameArray){
								Context context = host.new Context();
								host.addContext(context);
								context.setPath(appName);
								context.setDocBase(hostDir+"/"+appName);
							}
							
						
						}
					}
					logger.info(host);
					//校正
					String hostName = host.getName();
					String appNameDotXmlDir = catalinaHome+"/conf/"+engineName+"/"+hostName;
					if(existDirectory(appNameDotXmlDir,shell)){
						shell.executeCommands(new String[] { "ls --color=never -1" });
						cmdResult = shell.getResponse();
						logger.info(h.getIp()+"	目录"+appNameDotXmlDir+"下的xml部署文件	"+cmdResult);
						String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
						if(lines.length > 2){
							String[] appNameDotXmlArray = Arrays.copyOfRange(lines, 1, lines.length-1);
							logger.info(h.getIp()+"	目录"+appNameDotXmlDir+"下的xml部署文件	"+appNameDotXmlArray);
							
							for(String appNameDotXml:appNameDotXmlArray){
								if(!appNameDotXml.endsWith(".xml")) continue;
								
								shell.executeCommands(new String[] { "cat "+appNameDotXml });
								cmdResult = shell.getResponse();
								logger.info(h.getIp()+"	文件"+appNameDotXml+cmdResult);
								String contextDotXmlText = parseValidXmlTextFrom(cmdResult);
								logger.info(h.getIp()+"	格式化后"+contextDotXmlText);
								Document contextDotXmlDoc = null;
								try {
									contextDotXmlDoc = DocumentHelper.parseText(contextDotXmlText);
								} catch (DocumentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									logger.info(appNameDotXml+"文件中内容格式不符合xml规范");
									continue;
								}
								Element contextNode = contextDotXmlDoc.getRootElement();
								String appName = appNameDotXml.replaceAll("\\.xml$", "");
								String docBase = contextNode.attributeValue("docBase");
								Context context = host.new Context();
								context.setPath(appName);
								context.setDocBase(docBase);
								logger.info(h.getIp()+"	从"+appNameDotXml+"解析出来的	"+context);
								/**
								 * 1tomcat默认的manager应用，webapps下的manager目录就是应用的context，这样的情况context.xml文件中可以没有path和docBase
								 * 2而用户在webapps中部署的应用，假如不是上面的情况，context.xml  存在docBase 且会被当做context
								 * 3Engine->Host->下appName.xml部署方式，appName.xml中存在docBase
								 */
								if(context.getDocBase() != null){
									host.addContext(context);
								}
							}
							
						
						}
					}
				}
			}
		}
		
		List<App> appList = new ArrayList();
		for(Service service:serviceSet){
			
			/*Connector[] connectorArray = service.getConnectorSet().toArray(new Connector[0]);
			
			StringBuffer connectorPort = new StringBuffer();
			
			for(int i = 0,size = connectorArray.length;i < size ; i++){
				connectorPort.append(connectorArray[i].getPort());
				if((i+1) != size){
					connectorPort.append(",");
				}
			}*/
			for(Engine engine:service.getEngineSet()){
				for(host.middleware.tomcat.Service.Engine.Host host:engine.getHostSet()){
					for(Context context:host.getContextSet()){
						for(Connector connector:service.getConnectorSet()){
							App app = new App();
							String hostName = host.getName();
							String port = connector.getPort();
							String appName = context.getPath();
							app.setAppName(hostName+":"+port+"/"+appName);
							app.setDir(context.getDocBase());
							app.setPort(connector.getPort());
							app.setServiceIp("无");
							app.setServicePort("无");
							logger.info(h.getIp()+app);
						}
						
					}
				}
			}
		}
		
		return appList;
    }
   
    static boolean existDirectory(final String directory,final Shell shell){
    	shell.executeCommands(new String[] { "cd "+directory });
		String cmdResult = shell.getResponse();
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		if(lines.length > 2){
			return false;
		}
		return true;
    }
    static String parseValidXmlTextFrom(final String cmdResult){
    	String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		String serverDotXml = cmdResult.replaceAll("^"+Pattern.quote(lines[0]), "").replaceAll(Pattern.quote(lines[lines.length-1])+"$", "").trim();
		return serverDotXml;
    }
    /**
     * 采集linux服务器上的性能数据
     */
    private static void collectLinux(final Shell shell,final SSHClient ssh,final Host h,final List<PortLoadConfig> portListFromLoad){
    	
    	collectHostDetailForLinux(shell,h,portListFromLoad);
    	collectOracleForLinux(shell,h);
    	
    	collectWeblogicForLinux(shell,h,portListFromLoad);
    	collectTomcatForLinux(shell,h,portListFromLoad);
    }
    
    static void collectMysqlForLinux(final Shell shell,final Host h){
    	grantRoot(shell,h);
    	shell.executeCommands(new String[]{ "ps -ef|grep mysqld" });
    	String cmdResult = shell.getResponse();
    	
    	logger.info(h.getIp()+cmdResult);
    	
    	String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
    	
    	if(lines.length > 3){
    		List<String> lineWithMysqldList = new ArrayList();
    		Pattern mysqldProcessPattern = Pattern.compile("mysqld ");
    		//可能启动多个mysql数据库实例，一个实例是一个mysqld进程
    		for(String line:lines){
    			if(mysqldProcessPattern.matcher(line).find())
    				lineWithMysqldList.add(line);
    			
    		}
    		
    		for(String lineWithMysqld : lineWithMysqldList){
    			Database db = new Database();
    			h.addDatabase(db);
    			db.setType("MySQL");
    			db.setIp(h.getIp());
    			
    			//数据库部署路径
    			db.setDeploymentDir(shell.parseInfoByRegex(Regex.LinuxRegex.MYSQL_DEPLOYMENT_DIR, cmdResult, 1));
    			
    			//数据文件保存路径
    			String dataDir = shell.parseInfoByRegex(Regex.LinuxRegex.MYSQL_DATA_DIR, cmdResult, 1);
    			db.setDataFileDir(dataDir);
    			//版本号
    			shell.executeCommands(new String[]{"mysql --help|grep Distrib"});
    			cmdResult = shell.getResponse();
    			logger.info(h.getIp()+"	版本信息	"+cmdResult);
    			
    			db.setVersion(shell.parseInfoByRegex(Regex.LinuxRegex.MYSQL_VERSION, cmdResult, 1));
    			
    			//数据文件及其大小
    			
    			if(existDirectory(dataDir,shell)){
    				shell.executeCommands(new String[]{"ls --color=never -l|grep ^d"});
        			cmdResult = shell.getResponse();
        			logger.info(h.getIp()+"	数据库信息	"+cmdResult);
        			
        			lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
        			if(lines.length > 2){
        				//列出各数据库目录的名字和ibdata\w*(innodb引擎共享数据库表空间)，待下一步从获取此文件夹内所有目录和文件的列表中提取这些符合规定的文件大小
        				Set<String> dataFileNameSet = new HashSet();
        				for(int i = 1,size = lines.length-1;i < size;i++){
        					dataFileNameSet.add(shell.parseInfoByRegex(Regex.CommonRegex.DIR_NAME, lines[i], 1));
        				}
        				dataFileNameSet.add("ibdata\\w*");
            			shell.executeCommands(new String[]{"du -sm *"});
            			cmdResult = shell.getResponse();
            			
            			logger.info(h.getIp()+"	数据库大小	"+cmdResult);
            			
            			//把数据文件夹内所有目录和文件的大小都列出来，然后再从中找出各数据库目录和ibdata\w*的大小
            			lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
            			
            			List<DataFile> dataFileList = new ArrayList();
            			for(int i = 1,size = lines.length-1;i < size;i++){
            				String[] sizeAndName = lines[i].split(Regex.CommonRegex.BLANK_DELIMITER.toString());
            				String fileOrDirSize = sizeAndName[0];
            				String fileOrDirName = sizeAndName[1];
            				for(String dataFileName : dataFileNameSet){
            					if(fileOrDirName.matches(dataFileName)){
            						DataFile dataFile = new DataFile();
            						dataFile.setFileName(fileOrDirName);
            						dataFile.setFileSize(fileOrDirSize);
            			
            						dataFileList.add(dataFile);
            						
            					}
            				}
            			}
            			
            			logger.info(h.getIp()+"	数据文件列表	"+dataFileList);
            			db.setDfList(dataFileList);
        			}
        			
    			}
    			logger.info(h.getIp()+"	mysql数据库	"+db);
    		}
    	}
    }
    /**
     * @date 2015-1-9下午4:46:27
     * @author HP
     */
   public static void collectHostDetailForAIX(final Shell shell,final SSHClient ssh,final Host h,final List<PortLoadConfig> portListFromLoad){
    
	   Host.HostDetail hostDetail = new Host.HostDetail();
		h.setDetail(hostDetail);
		hostDetail.setOs(h.getOs());//主机详细信息页的操作系统类型
		//获取主机型号
		
		shell.executeCommands(new String[] { CollectCommand.AixCommand.HOST_TYPE.toString() });
		String cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+"---主机型号---");
		logger.info(cmdResult);
		
		logger.info(h.getIp()+"主机型号正则表达式		"+Regex.AixRegex.HOST_TYPE);
		logger.info(h.getIp()+"主机型号="+shell.parseInfoByRegex(Regex.AixRegex.HOST_TYPE,cmdResult,1));
		hostDetail.setHostType(shell.parseInfoByRegex(Regex.AixRegex.HOST_TYPE,cmdResult,1));
		//获取主机名
		shell.executeCommands(new String[] { "uname -n" });
		cmdResult = shell.getResponse();
		logger.info(h.getIp()+cmdResult);
		logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.HOST_NAME);
		logger.info(h.getIp()+"主机名="+shell.parseInfoByRegex(Regex.AixRegex.HOST_NAME,cmdResult,1));
		hostDetail.setHostName(shell.parseInfoByRegex(Regex.AixRegex.HOST_NAME,cmdResult,1));
		//获取系统版本号
		shell.executeCommands(new String[] { "uname -v" });
		cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+"---系统      主版本号---");
		logger.info(h.getIp()+cmdResult);
		logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.OS_MAIN_VERSION);
		 
		String version = shell.parseInfoByRegex(Regex.AixRegex.OS_MAIN_VERSION,cmdResult,1);
		
		shell.executeCommands(new String[] { "uname -r" });
		cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+"---系统      次要版本号---");
		logger.info(h.getIp()+cmdResult); 
		logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.OS_SECOND_VERSION);
		logger.info(h.getIp()+"系统版本号="+version+"."+shell.parseInfoByRegex(Regex.AixRegex.OS_SECOND_VERSION,cmdResult,1));
		hostDetail.setOsVersion(version+"."+shell.parseInfoByRegex(Regex.AixRegex.OS_SECOND_VERSION,cmdResult,1));
		
		
		//获取内存大小
		List<String> cmdsToExecute = new ArrayList<String>();
		
		ssh.setCommandLinePromptRegex(ssh.getPromptRegexArrayByTemplateAndSpecificRegex(COMMAND_LINE_PROMPT_REGEX_TEMPLATE,new String[]{"Full Core"}));
		
		cmdsToExecute.add("prtconf");
		
		try {
			cmdResult = ssh.execute(cmdsToExecute);
		} catch (ShellException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			cmdResult = "";///shell执行失败，结果默认为空串
		};
		
		logger.info(h.getIp()+"---内存大小---");
		logger.info(h.getIp()+cmdResult);
		logger.info(h.getIp()+"正则表达式		 "+Regex.AixRegex.MEMORY_SIZE);
		logger.info(h.getIp()+"内存大小="+shell.parseInfoByRegex(Regex.AixRegex.MEMORY_SIZE,cmdResult,1));
		hostDetail.setMemSize(shell.parseInfoByRegex(Regex.AixRegex.MEMORY_SIZE,cmdResult,1));
		
		//获取CPU个数
		logger.info(h.getIp()+"---CPU个数---");
	 
		logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.CPU_NUMBER);
		logger.info(h.getIp()+"CPU个数="+shell.parseInfoByRegex(Regex.AixRegex.CPU_NUMBER,cmdResult,1));
		hostDetail.setCPUNumber(shell.parseInfoByRegex(Regex.AixRegex.CPU_NUMBER,cmdResult,1));
		
		//获取CPU频率
		logger.info(h.getIp()+"---CPU频率---");
		logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.CPU_CLOCK_SPEED);
		logger.info(h.getIp()+"CPU频率="+shell.parseInfoByRegex(Regex.AixRegex.CPU_CLOCK_SPEED,cmdResult,1));
		hostDetail.setCPUClockSpeed(shell.parseInfoByRegex(Regex.AixRegex.CPU_CLOCK_SPEED,cmdResult,1));
		
		
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
		
		logger.info(h.getIp()+"---CPU核数---");
		logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.LOGICAL_CPU_NUMBER);
		logger.info(h.getIp()+"CPU核数="+shell.parseInfoByRegex(Regex.AixRegex.LOGICAL_CPU_NUMBER,cmdResult,1));
		String logicalCpuNumber;
		try{
			logicalCpuNumber = Integer.parseInt(shell.parseInfoByRegex(Regex.AixRegex.LOGICAL_CPU_NUMBER,cmdResult,1).trim())+1+"";
		}catch(NumberFormatException e){
			logicalCpuNumber = "NONE";
			logger.error(e);
			e.printStackTrace();
		}
		hostDetail.setLogicalCPUNumber(logicalCpuNumber);
		
		/****************
		 * 是否有配置双机
		 ****************/
		boolean isCluster = false;//默认没有配置双机
		shell.executeCommands(new String[] { "/usr/es/sbin/cluster/utilities/clshowsrv -v" });
		cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+"---是否有配置双机---");
		logger.info(h.getIp()+cmdResult);
		 
		if(cmdResult.split(Regex.CommonRegex.LINE_REAR.toString()).length>3?true:false){
			//配置有AIX自带的双机
			isCluster = true;
			hostDetail.setIsCluster("是");
			//获取双机虚地址
			shell.executeCommands(new String[] { "/usr/es/sbin/cluster/utilities/cllscf" });
			cmdResult = shell.getResponse();
			
			logger.info(h.getIp()+"---双机虚地址---");
			logger.info(h.getIp()+cmdResult);
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.CLUSTER_SERVICE_IP);
			hostDetail.setClusterServiceIP(shell.parseInfoByRegex(Regex.AixRegex.CLUSTER_SERVICE_IP, cmdResult,1));
		}
		if(!isCluster){
			shell.executeCommands(new String[] { "hastatus -sum" });
			cmdResult = shell.getResponse();
			
			logger.info(h.getIp()+"---第三方双机---");
			logger.info(h.getIp()+cmdResult);
			 
			//配置第三方双机,第三方双机采集不到虚地址
			if(cmdResult.split(Regex.CommonRegex.LINE_REAR.toString()).length>3?true:false){
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
		

		
		//主机被负载均衡
		if(portListFromLoad.size() > 0){
			hostDetail.setIsLoadBalanced("是");
			hostDetail.setLoadBalancedVirtualIP("见应用列表");
		}else{
			hostDetail.setIsLoadBalanced("否");
			hostDetail.setLoadBalancedVirtualIP("无");
		}
		logger.info(h.getIp()+portListFromLoad);
		
		/*******************
		 * 获取网卡信息
		 *******************/
		shell.executeCommands(new String[] { "lsdev -Cc adapter | grep ent" });
		cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+"---网卡信息---");
		logger.info(h.getIp()+cmdResult);
		logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.NETCARD_NAME); 
		String[] ents = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		List<Host.HostDetail.NetworkCard> cardList = new ArrayList<Host.HostDetail.NetworkCard>();
		///数组中第一个元素是输入的命令  最后一个元素是命令执行之后的提示符，过滤掉不予解析
		for(int i = 1,size = ents.length;i<size-1;i++){
			//提取网卡的名字
			Host.HostDetail.NetworkCard card = new Host.HostDetail.NetworkCard();
			card.setCardName(shell.parseInfoByRegex(Regex.AixRegex.NETCARD_NAME,ents[i],1));
			
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
		
		logger.info(h.getIp()+"---挂载点信息---");
		logger.info(h.getIp()+cmdResult);
		 
		String[] diskFSEntries = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		///滤掉磁盘信息的表格头
		List<Host.HostDetail.FileSystem> fsList = new ArrayList();
		for(int i = 2,size = diskFSEntries.length-1;i<size;i++){
			String[] entry = diskFSEntries[i].split(Regex.CommonRegex.BLANK_DELIMITER.toString());
			
			if(entry!=null && entry.length == 7){
				Host.HostDetail.FileSystem fs = new Host.HostDetail.FileSystem();
				
				fs.setMountOn(entry[6]);
				fs.setBlocks(entry[1]+" MB");
				fs.setUsed(entry[3]);
				
				fsList.add(fs);
				 
			}
			
		}
		hostDetail.setFsList(fsList);
    }
   /**
    * 
    */
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
   
   /**
    * 
    * @param shell
    * @param h
    * @date 2015-1-9下午3:42:34
    * @author HP
    */
   	public static void collectOracleForAIX(Shell shell,final Host h){

		//检测是否安装了Oracle数据库
 
		shell.executeCommands(new String[] { "ps -ef|grep tnslsnr" });
		String cmdResult = shell.getResponse();
		logger.info(h.getIp()+cmdResult);
		 
		boolean isExistOracle = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString()).length >=4?true:false;
		 
		//安装有Oracle
		if(isExistOracle){
			Host.Database db = new Host.Database();
			h.addDatabase(db);
			db.setType("Oracle");
			db.setIp(h.getIp());
			//找到oracle用户的目录
			shell.executeCommands(new String[] { "cat /etc/passwd|grep oracle" });
			cmdResult = shell.getResponse();
			
			logger.info(h.getIp()+cmdResult);
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.ORACLE_USER_DIR);
			String oracleUserDir = shell.parseInfoByRegex(Regex.AixRegex.ORACLE_USER_DIR,cmdResult,1);
			
			//找到oracle的安装目录
			shell.executeCommands(new String[] { "cat "+oracleUserDir+"/.profile" });
			cmdResult = shell.getResponse();
			
			logger.info(h.getIp()+cmdResult);
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.ORACLE_HOME_DIR);
			String oracleHomeDir = shell.parseInfoByRegex(Regex.AixRegex.ORACLE_HOME_DIR,cmdResult,1);
			 
			oracleHomeDir = oracleHomeDir.indexOf("ORACLE_BASE")!=-1?oracleHomeDir.replaceAll("\\$ORACLE_BASE", shell.parseInfoByRegex(Regex.AixRegex.ORACLE_BASE_DIR,cmdResult,1)):oracleHomeDir;
			 
			db.setDeploymentDir(oracleHomeDir);
			
			//找到实例名
			 
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.ORACLE_SID);
			String oracleSid = shell.parseInfoByRegex(Regex.AixRegex.ORACLE_SID,cmdResult,1);
			 
			db.setDbName(oracleSid);
			
			//数据文件保存路径
			
			
			//数据文件列表
			shell.executeCommands(new String[] { "su - oracle","sqlplus / as sysdba"});
			cmdResult = shell.getResponse();
			
			logger.info(h.getIp()+cmdResult);
			 
			
			shell.executeCommands(new String[] {"select file_name,bytes/1024/1024 ||'MB' as file_size from dba_data_files;"  });
			cmdResult = shell.getResponse();
			
			
			
			logger.info(h.getIp()+cmdResult); 
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.ORACLE_DATAFILE_LOCATION);
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.ORACLE_DATAFILE_SIZE);
	 		////数据文件大小 的正则\s+(\d+MB)\s+
			////数据文件位置的 正则\s+(/.*)\s+
			Pattern locationRegex = Pattern.compile(Regex.AixRegex.ORACLE_DATAFILE_LOCATION.toString());
			Pattern sizeRegex = Pattern.compile(Regex.AixRegex.ORACLE_DATAFILE_SIZE.toString());
			Matcher locationMatcher = locationRegex.matcher(cmdResult);
			Matcher sizeMatcher = sizeRegex.matcher(cmdResult);
			
			List<Host.Database.DataFile> dfList = new ArrayList<Host.Database.DataFile>();
			db.setDfList(dfList);
			while(locationMatcher.find()){
				Host.Database.DataFile dataFile = new Host.Database.DataFile();
				dataFile.setFileName(locationMatcher.group(1));
				logger.info(h.getIp()+"数据文件路径="+locationMatcher.group(1));
				if(sizeMatcher.find()){
					dataFile.setFileSize(sizeMatcher.group(1));
					logger.info(h.getIp()+"数据文件大小="+sizeMatcher.group(1));
				}
				
				dfList.add(dataFile);
			}
			
			
			//找到版本
			logger.info(h.getIp()+"---找到版本---");
			shell.executeCommands(new String[] {"select version from v$instance;"  });
			cmdResult = shell.getResponse();
			logger.info(h.getIp()+cmdResult);
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.ORACLE_VERSION);
			String version = shell.parseInfoByRegex(Regex.AixRegex.ORACLE_VERSION,cmdResult,1);
			db.setVersion(version);
			//由于进入了sqlplus模式，在此断开连接，退出重新登录
			shell.executeCommands(new String[] {"exit;"  });
			cmdResult = shell.getResponse();
			logger.info(h.getIp()+"	退出SQLPlus	"+cmdResult);
		}
		
		

   	}
   	/**
   	 * 采集weblogic的信息
   	 * @date 2015-1-9下午4:45:15
   	 * @author HP
   	 */
   	public static void collectWeblogicForAIX(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){

		/*********************
		 * weblogic中间件信息
		 *********************/
		 
		shell.executeCommands(new String[] { "" });
		shell.executeCommands(new String[] { "ps -ef|grep weblogic" });
		String cmdResult = shell.getResponse();

		logger.info(h.getIp()+cmdResult); 
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		//存在weblogic
		if(lines.length>4){
			Host.Middleware mw = new Host.Middleware();
			h.addMiddleware(mw);
			mw.setType("WebLogic");
			mw.setIp(h.getIp());
			//部署路径
			logger.info(h.getIp()+cmdResult);
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.WEBLOGIC_DEPLOY_DIR);
			String deploymentDir = shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_DEPLOY_DIR,cmdResult,1);
			String userProjectsDirSource = cmdResult;
			mw.setDeploymentDir(deploymentDir);
			//weblogic版本
			 
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.WEBLOGIC_VERSION);
			mw.setVersion(shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_VERSION,deploymentDir,1));
			
			//JDK版本
			 
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.WEBLOGIC_JDK_JAVA_COMMAND);
			shell.executeCommands(new String[] { shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_JDK_JAVA_COMMAND,cmdResult,1)+" -version" });
			cmdResult = shell.getResponse();
			
			logger.info(h.getIp()+cmdResult);
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.WEBLOGIC_JDK_VERSION);
			String jdkVersion = shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_JDK_VERSION,cmdResult,1);
			mw.setJdkVersion(jdkVersion);
			//采集 weblogic的应用列表
			List<App> appList = searchServiceIpAndPortForEachOf(collectWeblogicAppListForAIX(shell, userProjectsDirSource,h),portListFromLoad);
			
			
			mw.setAppList(appList);
			
		}
   	}
   	static List<App>  searchServiceIpAndPortForEachOf(final List<App> appList,final List<PortLoadConfig> portListFromLoad){
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
    /**
     * 采集aix主机信息
     * @param shell     建立ssh通道，连续发起采集命令
     * @param ssh	建立ssh通道，只能发起单个采集命令
     * @param h		
     * @param allLoadBalancerFarmAndServerInfo  所有的负载均衡的配置文件信息
     */
    private static void collectAIX(Shell shell,final SSHClient ssh,final Host h,final List<PortLoadConfig> portListFromLoad){
     	collectHostDetailForAIX(shell, ssh, h, portListFromLoad); 
		collectOracleForAIX(shell, h);
		collectWeblogicForAIX(shell, h, portListFromLoad);
		collectDB2ForAix(shell,h);
		
		collectWebSphereForAIX(shell, h, portListFromLoad);
		
    }
    
    /**
     * 采集DB2数据库
     * 
     * @date 2015-1-8下午4:49:58
     * @author HP
     */
    public static void collectDB2ForAix(final Shell shell,final Host h){
    	//检测安装有db2数据库，但是没有启动实例的情况
    	shell.executeCommands(new String[] { "ps -ef|grep db2fmcd" });
    	String cmdResult = shell.getResponse();
    	String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
    	
    	if(lines.length > 3){//安装了db2，没有启动实例
    		Host.Database db = new Host.Database();
			h.addDatabase(db);
			db.setIp(h.getIp());
			db.setType("DB2");
			List<String> db2InstanceUserListOfAllInstance = collectAllInstanceUserListForDB2( shell,  h);
    		//取每个实例用户所对应实例的数据库
    		boolean isNotCollectVersion = true;
    		
    		List<String> dbNameListOfAllInstance = new ArrayList();
    		final Set<String> dataFileDirSet = new HashSet();
			List<Host.Database.DataFile> dfList = new ArrayList();//所有实例下所有数据库的容器集合
			db.setDfList(dfList);
			
    		for(String instanceUser:db2InstanceUserListOfAllInstance){

    			grantRoot(shell,h);
    			
    			///切换到运行实例的DB2用户
    			shell.executeCommands(new String[] { "su - "+instanceUser });
    			cmdResult = shell.getResponse();
    			
    			logger.info(h.getIp()+"	切换到"+instanceUser+"用户	"+cmdResult);
    			
    			//多个用户的话db2安装路径和版本只在第一个用户登录的情况下采集一次
    			if(isNotCollectVersion){
    				///db2安装路径
        			shell.executeCommands(new String[] { "db2level" });
        			cmdResult = shell.getResponse();
        			
        			logger.info(h.getIp()+cmdResult);
        			logger.info(h.getIp()+"	db2安装路径="+shell.parseInfoByRegex("Product is installed at \"(\\S+?)\"", cmdResult, 1));
        			db.setDeploymentDir(shell.parseInfoByRegex("Product is installed at \"(\\S+?)\"", cmdResult, 1));
        			
        			//db2版本
        			logger.info(h.getIp()+"	db2版本	"+shell.parseInfoByRegex("Informational tokens are \"DB2 (\\S+?)\"", cmdResult, 1));
        			db.setVersion(shell.parseInfoByRegex("Informational tokens are \"DB2 (\\S+?)\"", cmdResult, 1));
        			
        			isNotCollectVersion = !isNotCollectVersion;
    			}
    			//db2数据库名称
    			shell.executeCommands(new String[] { "db2 list database directory" });
    			cmdResult = shell.getResponse();
    			
    			logger.info("数据库名称和数据文件路径="+cmdResult);
    			///找出实例下的所有数据库，每个实例下的数据库可以重名，因为可能是不同的数据库
    			Matcher  dbNameMatcher = Pattern.compile("Database alias\\s*?=\\s*([\\s\\S]*?)\\s").matcher(cmdResult);
    			List<String> dbNameListForCurrentInstance = new ArrayList();
    			while(dbNameMatcher.find()){
    				dbNameListForCurrentInstance.add(dbNameMatcher.group(1));
    			}
    			dbNameListOfAllInstance.addAll(dbNameListForCurrentInstance);
    			//数据文件路径 (多个数据库可能对应一个数据文件路径) 
    			Matcher  dataFileDirMatcher = Pattern.compile("Local database directory\\s*?=\\s*([\\s\\S]*?)\\s").matcher(cmdResult);
    			final Set<String> dataFileDirSetForCurrentInstance = new HashSet();
    			while(dataFileDirMatcher.find()){
    				dataFileDirSetForCurrentInstance.add(dataFileDirMatcher.group(1));
    			}
    			dataFileDirSet.addAll(dataFileDirSetForCurrentInstance);
    		
    			
    			//分别连接到当前实例的数据库
    			for(String dbName:dbNameListForCurrentInstance){
    				shell.executeCommands(new String[] { "db2 connect to "+dbName });
        			cmdResult = shell.getResponse();
        			
        			logger.info("连接到db2数据库="+cmdResult);
        			if(!connectDatabaseSuccess(cmdResult)){continue;}
        			
        			//取所有表空间id  和  页面大小
    				shell.executeCommands(new String[] { "db2 list tablespaces show detail" });
        			cmdResult = shell.getResponse();
        			logger.info("db2表空间详情="+cmdResult);
        			if(!showTablespacesDetailSuccess(cmdResult)) {continue;}
        			logger.info("TablespacesDetailSuccess");
        			///表空间ID
        			Matcher    tablespaceIdMatcher    =  Pattern.compile("Tablespace\\s+ID\\s+=\\s+(\\d+)",Pattern.CASE_INSENSITIVE).matcher(cmdResult);
        			///表空间页面大小
        			Matcher    pageSizeMatcher    =  Pattern.compile("Page\\s+size\\s+\\(bytes\\)\\s+=\\s+(\\d+)",Pattern.CASE_INSENSITIVE).matcher(cmdResult);
        			
        			List<Tablespace> tablespaceList = new ArrayList();
        			while(tablespaceIdMatcher.find()){
        				Tablespace tablespace = new Tablespace();
        				tablespaceList.add(tablespace);
        				tablespace.setId(tablespaceIdMatcher.group(1));
        				if(pageSizeMatcher.find()){
        					tablespace.setPageSize(Long.parseLong(pageSizeMatcher.group(1)));
        				}
        			}
        			logger.info("表空间列表="+tablespaceList);
        			//去每个表空间中的容器（数据文件）  容器大小
        			for(Iterator<Tablespace> it = tablespaceList.iterator();it.hasNext();){
        				Tablespace tablespace = it.next();
        				shell.executeCommands(new String[] { "db2 list tablespace containers for "+tablespace.getId()+" show detail" });
        				cmdResult = shell.getResponse();
        				
        				logger.info("db2表空间中的容器="+cmdResult);
        				if(!showContainersDetailSuccess(cmdResult)){continue;}
        				
        				Matcher   containerIdMatcher    =  Pattern.compile("Container\\s+ID\\s+=\\s+(\\d+)",Pattern.CASE_INSENSITIVE).matcher(cmdResult);
        				Matcher   containerTotalPagesMatcher    =  Pattern.compile("Total\\s+pages\\s+=\\s+(\\d+)",Pattern.CASE_INSENSITIVE).matcher(cmdResult);
        				Matcher   containerNameMatcher    =  Pattern.compile("Name\\s+=\\s+(\\S+)",Pattern.CASE_INSENSITIVE).matcher(cmdResult);
        				Matcher   containerTypeMatcher    =  Pattern.compile("Type\\s+=\\s+(\\w+)",Pattern.CASE_INSENSITIVE).matcher(cmdResult);
            			
            			List<Tablespace.Container> containerList  = new ArrayList();
            			tablespace.setContainerList(containerList);
        				DecimalFormat formator = new DecimalFormat(Format.TWO_DIGIT_DECIMAL.toString());
            			while(containerIdMatcher.find()){
        					Tablespace.Container container =  tablespace.new Container();
        					containerList.add(container);
        					container.setId(containerIdMatcher.group(1));
        					if(containerTotalPagesMatcher.find())	container.setTotalPages(Long.parseLong(containerTotalPagesMatcher.group(1)));
        					if(containerNameMatcher.find())		container.setName(containerNameMatcher.group(1));
        					if(containerTypeMatcher.find())		container.setType(containerTypeMatcher.group(1));
        					
        					float totalSize = 1.0f*container.getTotalPages()*tablespace.getPageSize()/Unit.MB.unitValue();
        					logger.info(totalSize+"="+container.getTotalPages()+"*"+tablespace.getPageSize()+"/"+Unit.MB.unitValue());
        					container.setTotalSize(Float.parseFloat(formator.format(totalSize)));
        					
        					Host.Database.DataFile dataFile = new Host.Database.DataFile();
        					//db2容易当做数据文件来看待
        					dataFile.setFileName(container.getName());
        					dataFile.setFileSize(formator.format(totalSize));
        					dfList.add(dataFile);
        				}
        			}
        			logger.info("表空间列表="+tablespaceList);
    			
        			
    			}
    			
    			
    			
        	}
    		db.setDbName(dbNameListOfAllInstance.toString());
    		db.setDataFileDir(dataFileDirSet.toString());
    		//db2容器（数据文件）及其大小
			
			
			logger.info(db);
    	}
    	
    	
    }
    
    private static List<String> collectAllInstanceUserListForDB2(final Shell shell,final Host h){
    	shell.executeCommands(new String[] { "strings /var/db2/global.reg" });
		String cmdResult = shell.getResponse();
		
		logger.info(cmdResult);
		Matcher db2InstanceUserMatcher  = Pattern.compile("(/\\S+)*/(\\S+?)/sqllib").matcher(cmdResult);
		
		List<String> db2AllInstanceUserList = new ArrayList();
		while(db2InstanceUserMatcher.find()){
			String userDirectory = db2InstanceUserMatcher.group();
			logger.info("用户根目录="+userDirectory);
			//定位到用户根目录，如果可以定位到用户根目录说明  这个实例用户是有效的，可以切换
			shell.executeCommands(new String[] { "cd "+userDirectory });
			cmdResult = shell.getResponse();
			logger.info("定位到用户根目录="+cmdResult);
	    	String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
			if(lines.length > 2){//说明 无法定位到用户的根目录，即这个用户的根目录是不存在的
				continue;
			}
			db2AllInstanceUserList.add(db2InstanceUserMatcher.group(2));
		}
		logger.info("db2实例用户="+db2AllInstanceUserList);
		return db2AllInstanceUserList;
    }
    public static boolean showContainersDetailSuccess(final String cmdResult){
    	return match("Tablespace\\s+Containers\\s+for\\s+Tablespace",cmdResult);
    }
    public static boolean showTablespacesDetailSuccess(final String cmdResult){
    	return match("Tablespaces\\s+for\\s+Current\\s+Database",cmdResult);
    }
    public static boolean connectDatabaseSuccess(final String cmdResult){
    	return match("Database\\s+Connection\\s+Information",cmdResult);
    }
    
    public static  boolean	match(final String regex,final String string){
    	return Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(string).find();
    }
    /**
     * 
     * @param shell
     * @param h
     * @return
     * @date 2015-1-12下午6:02:51
     * @author HP
     */
    public static Set<String>	collectRunningInstanceUserSetForDB2(final Shell shell,final Host h){
    	//检测安装有db2数据库，同时也启动了实例的情况，通过下面的命令，可以知道启动了哪些实例
		shell.executeCommands(new String[] { "ps -ef|grep db2sysc" });
		String cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+cmdResult); 
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		
		final Set<String>  db2RunningUserSet = new HashSet();
		if(lines.length > 3){//启动了db2实例
		
			//运行db2实例的用户名,至少有一个用户在运行db2实例
			for(String line:lines){
				if(!Pattern.compile(Regex.AixRegex.WEBSPHERE_INSTANCE_PROCESS.toString()).matcher(line).find()){
					Matcher db2UserMatcher = Pattern.compile(Regex.AixRegex.WEBSPHREE_INSTANCE_USER.toString()).matcher(line);
					if(db2UserMatcher.find()){
						db2RunningUserSet.add(db2UserMatcher.group(1));
					}
						
				}
			}
			logger.info("运行实例的db2用户列表="+db2RunningUserSet);
		}
		return db2RunningUserSet;
    }
	 /**
	  * 采集websphere中间件
	  * @param shell
	  * @param h
	  * @date 2015-1-7下午4:30:09
	  * @author HP
	  */
    public static void collectWebSphereForAIX(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){
    	shell.executeCommands(new String[] { "" });
		shell.executeCommands(new String[] { "ps -ef|grep websphere" });
		String cmdResult = shell.getResponse();
		logger.info(h.getIp()+cmdResult); 
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		
		//检查有没有安装websphere
		if(lines.length > 4){
			Host.Middleware  mw = new Host.Middleware();
			h.addMiddleware(mw);
			
			mw.setType("WebSphere");
			mw.setIp(h.getIp());
			//webSphere安装路径
			String  deploymentDir = shell.parseInfoByRegex(Pattern.quote("-Dwas.install.root=")+"([\\S]+)",cmdResult,1);
			mw.setDeploymentDir(deploymentDir);
			
			//webSphere应用部署路径,考虑到集群的情况，DM的user.intall.root与node不一致
			Set<String> userInstallRootSet = new HashSet();
			for(int i = 1,size = lines.length -1 ;i < size; i++){
				if(isNotDeploymentManager(lines[i],"dmgr$")){
					String userIntallRoot = shell.parseInfoByRegex(Pattern.quote("-Duser.install.root=")+"([\\S]+)",cmdResult,1);
					userInstallRootSet.add(userIntallRoot);
				}
			}
			logger.info(h.getIp()+"	用户根目录	"+userInstallRootSet);
			//webSphere  JDK版本
			shell.executeCommands(new String[] { deploymentDir+"/java/bin/java -version" });
			cmdResult = shell.getResponse();
			
			logger.info(h.getIp()+cmdResult);
			mw.setJdkVersion(shell.parseInfoByRegex("java version \"([\\s\\S]*?)\"",cmdResult,1));
			
			//webSphere版本
			shell.executeCommands(new String[] { "cd "+deploymentDir+"/bin"});
			shell.executeCommands(new String[] { "./versionInfo.sh"});
			cmdResult = shell.getResponse(); 
			mw.setVersion(shell.parseInfoByRegex("Version\\s+([\\d.]+)",cmdResult,1));
			logger.info(h.getIp()+cmdResult);
			
			//websphere应用列表
			List<App> appList = new ArrayList();
			for(String userIntallRoot : userInstallRootSet){
				appList.addAll(searchServiceIpAndPortForEachOf(collectAppListForWebsphere(shell,userIntallRoot,h),portListFromLoad));
				
			}
			mw.setAppList(appList);
			logger.info(mw);
		}
    }
    private static boolean isNotDeploymentManager(final String line,final String regex){
    	return !match(line,regex);
    }
    static List<App> collectAppListForWebsphere(final Shell shell,String userInstallRoot,final Host h){
    	grantRoot(shell,h);
    	String   installedAppsDir = userInstallRoot+"/installedApps/";
    	List<App> appListOfAllCell = new ArrayList();
    	if(existDirectory(installedAppsDir,shell)){
    		shell.executeCommands(new String[] { "ls -l|grep ^d"});
    		String  cmdResult = shell.getResponse(); 
    		
    		
    		logger.info(h.getIp()+"	installedApps下的cell文件夹		"+cmdResult);
			Set<String> cellDirSetOnInstalledAppsDir = parseDirectoryNameSetFromDirDetail(cmdResult,shell);
			
			if(!cellDirSetOnInstalledAppsDir.isEmpty()){
				/**
				 * 本机节点node可以属于多个cell(管理域)
				 * 每个cell中安装的程序可能是多个node集群的应用
				 */
				Map<String,Set<String>> cellPathAndAppNameDirSetMap = new HashMap();
				for(String cellDirOnInstalledAppsDir : cellDirSetOnInstalledAppsDir){
					String cellPathOnInstalledAppsDir = installedAppsDir+cellDirOnInstalledAppsDir;
					shell.executeCommands(new String[] { "ls -l "+cellPathOnInstalledAppsDir});
		    		cmdResult = shell.getResponse();
		    		
		    		Set<String> appNameDirSet = parseDirectoryNameSetFromDirDetail(cmdResult,shell);
		    		
		    		cellPathAndAppNameDirSetMap.put(cellPathOnInstalledAppsDir, appNameDirSet);
				}
				logger.info(h.getIp()+"	各个cell下的app	"+cellPathAndAppNameDirSetMap);
				//
				String cellsPathOnConfigDir = userInstallRoot+"/config/cells/";
				 
				shell.executeCommands(new String[] { "ls -l "+cellsPathOnConfigDir + " |grep ^d"});
	    		cmdResult = shell.getResponse();
	    		
	    		logger.info(h.getIp()+"	config目录下的cell	"+cmdResult);
	    		Set<String> cellDirSetOnCellsDir = parseDirectoryNameSetFromDirDetail(cmdResult,shell);
	    		logger.info(h.getIp()+"	config目录下的cell	"+cellDirSetOnCellsDir);
	    		/**
	    		 * serverindex.xml中应用和端口
	    		 * 在某个cell下本机node所安装的应用
	    		 */
	    		Map<String,List<App>> cellDirAndAppListMap = new HashMap();
	    		for(String cellDirOnCellsDir : cellDirSetOnCellsDir){
	    			
	    			String nodesPathOnCellDir = cellsPathOnConfigDir+cellDirOnCellsDir+"/nodes/";
	    			shell.executeCommands(new String[] { "ls -l "+nodesPathOnCellDir+" |grep ^d"});
		    		cmdResult = shell.getResponse();
		    		
		    		logger.info(h.getIp()+"	nodes目录下的node	"+cmdResult);
	    			Set<String> nodeDirSetOnNodesDir = parseDirectoryNameSetFromDirDetail(cmdResult,shell);
	    			logger.info(h.getIp()+"	nodes目录下的node	"+nodeDirSetOnNodesDir);
	    			List<App> appListOfCellDir = new ArrayList();
	    			
	    			cellDirAndAppListMap.put(cellDirOnCellsDir, appListOfCellDir);
	    			for(String nodeDirOnNodesDir : nodeDirSetOnNodesDir){
	    				String nodePathOnNodesDir = nodesPathOnCellDir+nodeDirOnNodesDir+"/";
	    				
	    	    		logger.info(h.getIp()+" serverindex.xml路径	"+nodePathOnNodesDir);
	    	    		if(existFileOnPath(nodePathOnNodesDir,"serverindex.xml",shell)){
	    	    			/***
	    	    			 * 默认情况下，node名称为hostnameNodexx(xx为01,02的规则)，但是node名称可自定义
	    	    			 * 故采用serverindex.xml取得hostName
	    	    			 * 与主机名想匹配的方式
	    	    			 */
		    				shell.executeCommands(new String[] { "cat "+nodePathOnNodesDir+"serverindex.xml"});
		    	    		cmdResult = shell.getResponse();
		    	    		
		    	    		logger.info(h.getIp()+"	各个node目录下的serverindex.xml文件内容	"+cmdResult);
		    	    		String serverIndexDotXmlText = parseValidXmlTextFrom(cmdResult);
		    	    		logger.info(h.getIp()+"	格式化之后的serverindex.xml内容	"+serverIndexDotXmlText);
		    	    		Document serverIndexDoc = null;
		    	    		try {
		    	    			serverIndexDoc = DocumentHelper.parseText(serverIndexDotXmlText);
							} catch (DocumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								logger.info("xml文本串不合法");
								continue;
							}
		    	    		
		    	    		/**
		    	    		 * 1找出serverType为APPLICATION_SERVER的serverEntries，内部子元素
		    	    		 * <deployedApplications>IDSWebApp_war.ear/deployments/IDSWebApp_war</deployedApplications>
		    	    		 * IDSWebApp_war.ear就是部署的应用
		    	    		 * 
		    	    		 * 2serverEntries元素内部，它的endPointName属性为WC_defaulthost的子元素specialEndpoints是访问端口元素
		    	    		 * 然后 取specialEndpoints它的endPoint子元素
		    	    		 * 			其port属性就是端口，元素结构如下
		    	    		 * <specialEndpoints xmi:id="NamedEndPoint_1227522882663" endPointName="WC_defaulthost"> 
										<endPoint xmi:id="EndPoint_1227522882663" host="*" port="9080"/> 
							   </specialEndpoints> 
		    	    		 */
		    	    		Element serverIndexNode = serverIndexDoc.getRootElement();
		    	    		String hostNameAttValue = serverIndexNode.attributeValue("hostName");
		    	    		
		    	    		logger.info(h.getIp()+"	serverindex.xml中主机名	"+hostNameAttValue);
		    	    		
		    	    		
		    	    		String hostName = AixCollector.collectHostNameForAIX(shell, h);//Junit单元测试和正式部署都可以使用这种方式，可选的方法可以通过h.getDetail().getHostName()得到主机名
		    	    		if(hostNameAttValue.equalsIgnoreCase(hostName)){
		    	    			
		    	    			List<Element> serverEntriesNodeList = serverIndexNode.elements("serverEntries");
			    	    		logger.info(h.getIp()+serverEntriesNodeList);
			    	    		for(Element serverEntriesNode : serverEntriesNodeList){
			    	    			//取application
			    	    			String serverTypeAttValue = serverEntriesNode.attributeValue("serverType");
			    	    			if("APPLICATION_SERVER".equalsIgnoreCase(serverTypeAttValue)){
			    	    				List<Element>  deployedApplicationsNodeList = serverEntriesNode.elements("deployedApplications");
			    	    				if(!deployedApplicationsNodeList.isEmpty()){

				    	    				//取访问port
					    	    			List<Element> specialEndpointsNodeList = serverEntriesNode.elements("specialEndpoints");
					    	    			String port = "";
					    	    			for(Element specialEndpointsNode  : specialEndpointsNodeList){
					    	    				String endPointNameAttValue =  specialEndpointsNode.attributeValue("endPointName");
					    	    				if("WC_defaulthost".equalsIgnoreCase(endPointNameAttValue)){
					    	    					Element endPointNode = specialEndpointsNode.element("endPoint");
					    	    					String portAttValue = endPointNode.attributeValue("port");
					    	    					port = portAttValue;
					    	    					logger.info(h.getIp()+"	 serverindex.xml中解析到的port	"+port);
					    	    					break;
					    	    				}
					    	    			}
			    	    					for(Element deployedApplicationsNode : deployedApplicationsNodeList){
				    	    					String deployedApplicationsText = deployedApplicationsNode.getTextTrim();
				    	    					String applicationName = shell.parseInfoByRegex("[^/]+", deployedApplicationsText, 0);
				    	    					logger.info(h.getIp()+"	 serverindex.xml中解析到的appName	"+applicationName);
				    	    					App app = new App();
				    	    					appListOfCellDir.add(app);
				    	    					app.setAppName(applicationName);
				    	    					app.setPort(port);
				    	    					app.setDir(deployedApplicationsText);
				    	    					app.setServiceIp("无");
				    	    					app.setServicePort("无");
				    	    					logger.info(h.getIp()+"	 serverindex.xml中解析到的app	"+app);
				    	    				}
				    	    				
			    	    				}
			    	    				
			    	    			}
			    	    			
			    	    			
			    	    		}
		    	    		}
		    	    		
	    	    		}
	    	    		
	    			}
	    		}
	    		
	    		/**
	    		 * 拼凑出部署应用的完整的路径名，一部分来自serverindex.xml文件,另一部分来自installedApps目录
	    		 */
	    		Set<String> cellPathSet = cellPathAndAppNameDirSetMap.keySet();
	    		Set<String> cellDirSet = cellDirAndAppListMap.keySet();
	    		for(String cellDir : cellDirSet){
    				Pattern cellDirPattern = Pattern.compile(Pattern.quote(cellDir)+"$");
    				for(String cellPath  : cellPathSet){
    	    			
    	    			Matcher cellDirMatcher = cellDirPattern.matcher(cellPath);
    	    			if(cellDirMatcher.find()){
    	    				Set<String> appNameDirSet  = cellPathAndAppNameDirSetMap.get(cellPath);//installedApps下
    	    				List<App> appListOfCellDir = cellDirAndAppListMap.get(cellDir);//config下
    	    				for(Iterator<App> it = appListOfCellDir.iterator();it.hasNext();){
    	    					App app = it.next();
    	    					for(String appNameDir : appNameDirSet){
    	    						if(appNameDir.equalsIgnoreCase(app.getAppName())){
    	    							String appFullPath = cellPath + "/" +app.getDir();
    	    							app.setDir(appFullPath);
    	    							logger.info(h.getIp()+"	 app的完整安装路径名	"+appFullPath);
    	    						}
    	    					}
    	    				}
    	    			}
    	    		}
    			}
	    		
	    		//所有cell下的app
	    		Collection<List<App>> appListCollectionOfAllCell = cellDirAndAppListMap.values();
	    		for(List<App> appListOfAnyCell : appListCollectionOfAllCell){
	    			appListOfAllCell.addAll(appListOfAnyCell);
	    		}
			}
			
    	
    	}
    	logger.info(h.getIp()+"	 应用	"+appListOfAllCell);
    	return appListOfAllCell;
    }
    static boolean existFileOnPath(final String path,final String fileName,final Shell shell){
    	
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
    static   Set<String> parseDirectoryNameSetFromDirDetail(final String cmdResult,final Shell shell){
    	
    	Set<String> dirNameSet = new HashSet<String>();
    	String[] dirDetailInfoArray = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
    	if(dirDetailInfoArray.length > 2){
    		for(int i = 1,size = dirDetailInfoArray.length -1;i < size;i++){
        		dirNameSet.add(shell.parseInfoByRegex("(\\S+)$", dirDetailInfoArray[i], 1));
    		}
    	}
    	
    	
    	return dirNameSet;
    }
    /**
     * 提升为root权限
     * @param shell
     * @param h
     */
    private static void grantRoot(final Shell shell,final HostBase h){
    	logger.info(h.getIp()+"	root用户登录");
    	//当需要特别权限的情况下使用root用户
		String rootUser = h.getRootUser();
		String rootUserPassword = h.getRootUserPassword();
    	//切换到root用户 ，提升权限
		shell.setCommandLinePromptRegex(shell.getPromptRegexArrayByTemplateAndSpecificRegex(COMMAND_LINE_PROMPT_REGEX_TEMPLATE,new String[]{"Password:"}));
		
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
    /**
     * 采集操作系统的类型   AIX  Linux
     * @param shell
     * @param h
     */
    private static void collectOs(final Shell shell,final HostBase h){
    	 
		shell.executeCommands(new String[] { CollectCommand.CommonCommand.HOST_OS.toString() });
		String cmdResult = shell.getResponse();
		//获取操作系统的类型
		logger.info(h.getIp()+"---操作系统的类型---");
		logger.info(h.getIp()+"执行命令的结果="+cmdResult);
		logger.info(h.getIp()+"正则表达式		\\s*uname\r\n(.*)\r\n");
		h.setOs(shell.parseInfoByRegex("\\s*uname\\s+(\\w+?)\\s+",cmdResult,1));
		logger.info(h.getIp()+"操作系统类型="+shell.parseInfoByRegex("\\s*uname\\s+(\\w+?)\\s+",cmdResult,1));
    }
    
   
    /***
     *  采集负载均衡配置
     * @return
     */
    private static StringBuilder collectLoadBalancer(){
    	
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
							
							shell = new Shell(lb.getIp(), SSH_PORT,lb.getUserName(), lb.getPassword());
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
    	StringBuilder allLoadBalancerFarmAndServerInfo  = collectLoadBalancer();
    	logger.info("------开始采集主机-----");
    	collectHosts(list,allLoadBalancerFarmAndServerInfo);
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
    	batHosts(list,command,isNotCollected);
    	
    }
    /**
     * 
     * @param list	批处理的主机
     * @param command	要在所有主机上执行的命令
     */
    public static void batHosts(final List<TinyHost> list,final String command,final boolean isNotCollected){
    	
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
		    			SSHClient ssh = new SSHClient(h.getIp(), h.getJkUser(), h.getJkUserPassword());
 
		    			// 建立连接
		    			Shell shell;
		    			try {
		    				shell = new Shell(h.getIp(), SSH_PORT,h.getJkUser(), h.getJkUserPassword());
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
    /**
     * 采集主机
     * @param list	主机列表
     * @param allLoadBalancerFarmAndServerInfo   负载均衡上采集到的配置信息
     */
    public static void collectHosts(final List<Host> list,final StringBuilder allLoadBalancerFarmAndServerInfo){
    	
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
		    			
		    			/*HintMsg msg = new HintMsg(nowNum++,maxNum,"当前IP:"+h.getIp(),"当前主机采集进度");
		    			DwrPageContext.run(JSONObject.fromObject(msg).toString());
		    			logger.info(msg);*/
		    			// 登录
		    			SSHClient ssh = new SSHClient(h.getIp(), h.getJkUser(), h.getJkUserPassword());
 
		    			// 建立连接
		    			Shell shell;
		    			try {
		    				shell = new Shell(h.getIp(), SSH_PORT,h.getJkUser(), h.getJkUserPassword());
		    				shell.setTimeout(2*1000);
		    				
		    				logger.error("	连接到 	"+h.getIp());
			    			
			    			grantRoot(shell,h);
			    			
			    			collectOs(shell,h);
			    			
			    			List<PortLoadConfig> portListFromLoad  =   parsePortList(h, allLoadBalancerFarmAndServerInfo.toString());
			    			if("AIX".equalsIgnoreCase(h.getOs())){
			    				collectAIX(shell,ssh,h,portListFromLoad);
			    			}else if("LINUX".equalsIgnoreCase(h.getOs())){
			    				collectLinux(shell,ssh,h,portListFromLoad);
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
    public static List<Host.Middleware.App> collectWeblogicAppListForAIX(final Shell shell,final String userProjectsDirSource,final Host h){

		
		//应用名称及其部署路径
		///找到weblogic中的应用domain 文件夹路径 层次 user_projects->domains->appName_domains
    	logger.info(h.getIp()+"---weblogic中的应用domain 文件夹路径 层次 user_projects->domains->appName_domains---");
		 logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.WEBLOGIC_ROOT_DIR);
		
		 Set<String> appRootDirSet = shell.parseUserProjectSetByRegex(Regex.AixRegex.WEBLOGIC_ROOT_DIR,userProjectsDirSource);
		logger.info(h.getIp()+"　weblogic中的应用domain 文件夹路径 层次＝"+appRootDirSet);
		
		Map<String,Set<String>> appDomainMap = new HashMap();//key是 appRootDir应用根目录
		for(String appRootDir:appRootDirSet){
			shell.executeCommands(new String[] {"ls " + appRootDir+"/user_projects/domains" });
			String cmdResult = shell.getResponse();
			
			logger.info(h.getIp()+cmdResult);
			 
			String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
			 if(lines.length>2){//domains下面有多个应用domain
				Set<String> appDomainSet = new HashSet();
				 for(int i = 1,index = lines.length-1 ;i<index;i++ ){
					 String[] domains = lines[i].split(Regex.CommonRegex.BLANK_DELIMITER.toString());
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
						
						logger.info(h.getIp()+cmdResult);
						String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
						
						///weblogic10
						if(lines.length>4){    ///执行返回的结果大于4行的话，说明存在config.xml配置文件
							isExistConfig = true;
							Host.Middleware.App app = new Host.Middleware.App();
						 
							logger.info(h.getIp()+"应用名称   正则表达式		"+Regex.AixRegex.WEBLOGIC_10_APP_NAME);
							logger.info(h.getIp()+"应用路径   正则表达式		"+Regex.AixRegex.WEBLOGIC_10_APP_DIR);
							///匹配应用的名字<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_10_APP_NAME,cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_10_APP_DIR,cmdResult,1));
							app.setPort(shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_10_APP_PORT,cmdResult,1));
							app.setServiceIp("无");
							app.setServicePort("无");
							
							appList.add(app);
							logger.info(h.getIp()+app);
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
							
							logger.info(h.getIp()+cmdResult);
							logger.info(h.getIp()+"应用名称     正则表达式		"+Regex.AixRegex.WEBLOGIC_8_APP_NAME);
							logger.info(h.getIp()+"应用路径     正则表达式		"+Regex.AixRegex.WEBLOGIC_8_APP_DIR);
							
							///匹配应用的名字<[Aa]pplication[\s\S]+?[Nn]ame="([\S]+)"  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_8_APP_NAME,cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_8_APP_DIR,cmdResult,1));
							app.setPort(shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_8_APP_PORT,cmdResult,1));
							app.setServiceIp("无");
							app.setServicePort("无");
							
							appList.add(app);
							logger.info(h.getIp()+app);
						}
					}
					//System.out.println(cmdResult);
					
			 }
			
		 }
		//过滤掉没有部署应用的域    即appname为NONE的应用
		for(Iterator<App> it = appList.iterator();it.hasNext();){
			App app = it.next();
			if("NONE".equals(app.getAppName())){
				it.remove();
			}
		}
		return appList;
		
    }
    
    /**
     * 采集linux服务器上运行的应用列表
     * @param shell
     * @param userProjectsDirSource
     * @return
     */
    public static List<Host.Middleware.App> collectWeblogicAppListForLinux(final Shell shell,final String userProjectsDirSource,final Host h){
    	//应用名称及其部署路径
		///找到weblogic中的应用domain 文件夹路径 层次 user_projects->domains->appName_domains
		Set<String> appRootDirSet = shell.parseUserProjectSetByRegex("-Djava.security.policy=(/.+)/[\\w.]+/server/lib/weblogic.policy",userProjectsDirSource);
		
		logger.info(h.getIp()+appRootDirSet);
		logger.info(h.getIp()+"正则表达式		-Djava.security.policy=(/.+)/[\\w.]+/server/lib/weblogic.policy");
		
		
		Map<String,Set<String>> appDomainMap = new HashMap();//key是 appRootDir应用根目录
		for(String appRootDir:appRootDirSet){
			shell.executeCommands(new String[] {"ls --color=never " + appRootDir+"/user_projects/domains" });
			String cmdResult = shell.getResponse();

			logger.info(h.getIp()+cmdResult);
			 
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
						
						logger.info(h.getIp()+cmdResult);
						
						String[] lines = cmdResult.split("[\r\n]+");
					
						///weblogic10
						if(lines.length>4){    ///执行返回的结果大于4行的话，说明存在config.xml配置文件
							isExistConfig = true;
							Host.Middleware.App app = new Host.Middleware.App();
							
							
							logger.info(h.getIp()+"正则表达式		<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>");
							logger.info(h.getIp()+"正则表达式		<app-deployment>[\\s\\S]*?<source-path>(.*)</source-path>[\\s\\S]*?</app-deployment>");
							
							///匹配应用的名字<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<app-deployment>[\\s\\S]*?<source-path>(.*)</source-path>[\\s\\S]*?</app-deployment>",cmdResult,1));
							app.setPort(shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_10_APP_PORT,cmdResult,1));
							app.setServiceIp("无");
							app.setServicePort("无");
							appList.add(app);
							logger.info(h.getIp()+app);
						}
					 }
					if(!isExistConfig){
						///weblogic8
						shell.executeCommands(new String[] {"cat " + appRootDir+"/user_projects/domains/"+domain+"/config.xml" });
						String cmdResult = shell.getResponse();
						
						logger.info(h.getIp()+cmdResult);
						
						String[] lines = cmdResult.split("[\r\n]+");
						if(lines.length>4){		///执行返回的结果大于4行的话，说明存在config.xml配置文件
							isExistConfig = true;
							Host.Middleware.App app = new Host.Middleware.App();
							
							logger.info(h.getIp()+"正则表达式		<[Aa]pplication[\\s\\S]+?[Nn]ame=\"([\\S]+)\"");
							logger.info(h.getIp()+"正则表达式		<[Aa]pplication[\\s\\S]+?[Pp]ath=\"([\\S]+)\"");
							
							
							///匹配应用的名字<[Aa]pplication[\s\S]+?[Nn]ame="([\S]+)"  有优化空间，或者可以使用dom4j建立xml文件的DOM结构
							app.setAppName(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Nn]ame=\"([\\S]+)\"",cmdResult,1)); 
							app.setDir(shell.parseInfoByRegex("<[Aa]pplication[\\s\\S]+?[Pp]ath=\"([\\S]+)\"",cmdResult,1));
							app.setPort(shell.parseInfoByRegex(Regex.AixRegex.WEBLOGIC_8_APP_PORT,cmdResult,1));
							app.setServiceIp("无");
							app.setServicePort("无");
							appList.add(app);
							logger.info(h.getIp()+app);
						}
					}	
			 }
			
		 }
			
		 return appList;
    }
}