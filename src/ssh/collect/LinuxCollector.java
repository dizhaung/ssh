package ssh.collect;

import host.Host;
import host.Host.Database;
import host.Host.Database.DataFile;
import host.Host.HostDetail;
import host.Host.HostDetail.FileSystem;
import host.Host.HostDetail.NetworkCard;
import host.Host.Middleware;
import host.Host.Middleware.App;
import host.PortLoadConfig;
import host.middleware.tomcat.Service;
import host.middleware.tomcat.Service.Connector;
import host.middleware.tomcat.Service.Engine;
import host.middleware.tomcat.Service.Engine.Host.Context;
import host.middleware.tomcat.Service.HttpConnector;
import host.middleware.tongweb.HttpListener;
import host.middleware.tongweb.VirtualServer;
import host.middleware.tongweb.WebApp;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import constants.regex.Regex;
import constants.regex.Regex.AixRegex;
import constants.regex.Regex.CommonRegex;
import constants.regex.Regex.LinuxRegex;

import ssh.SSHClient;
import ssh.Shell;

public class LinuxCollector extends HostCollector {

	private static Log logger = LogFactory.getLog(LinuxCollector.class);

	protected void collectHostDetail(final Shell shell,SSHClient ssh,final Host h, final List<PortLoadConfig> portListFromLoad){
	
		
		Host.HostDetail hostDetail = new Host.HostDetail();
		h.setDetail(hostDetail);
		hostDetail.setOs(h.getOs());//主机详细信息页的操作系统类型
		
		
		
		//获取主机型号
		shell.executeCommands(new String[] { "dmidecode -s system-manufacturer" });//root用户登录
		String cmdResult = shell.getResponse();
		
		logger .info(cmdResult);
		
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

	protected void collectOracle(Shell shell,final Host h){
		//检测是否安装了Oracle数据库
		shell.executeCommands(new String[] { "ps -ef|grep tnslsnr" });
		String cmdResult = shell.getResponse();
		logger.info(cmdResult);
		 
		boolean isExistOracle = cmdResult.split("[\r\n]+").length >=4?true:false;
		
		//安装有Oracle
		if(isExistOracle){
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

	protected  void collectWeblogic(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){
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
					List<App> appList = searchServiceIpAndPortForEachOf(LinuxCollector.collectWeblogicAppListForLinux(shell, userProjectsDirSource,h), portListFromLoad);
					mw.setAppList(appList);
				}
			
	}

	protected void collectTomcat(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){
	
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

	public static String reverseTomcatAppPathToAppName(final String path){
		if("/".equals(path)){
			return "ROOT";
		}
		return path.replaceAll("^/", "");
	}

	public  List<App> collectAppListForTomcat(final Shell shell,final String catalinaHome, final Host h){
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

	protected void collectWebSphere(Shell shell, host.Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	 protected void collectDB2(Shell shell, host.Host h) {
		// TODO Auto-generated method stub
		
	}

	protected void collectTongweb(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){
		grantRoot(shell,h);
		shell.executeCommands(new String[]{ "ps -ef|grep tongweb" });
		String cmdResult = shell.getResponse();
		
		logger.info(h.getIp()+cmdResult);
		String[] lines = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		
		if(lines.length > 3){
			//假如安装有多个tongweb中间件的话，每个运行的tongweb中间件都有自己的tongweb.root安装路径
			Set<String> tongwebProcessInfoLineSet = new HashSet();
			
			Pattern deploymentDirPattern  = Pattern.compile("Dtongweb\\.root\\s*=\\s*(\\S+)");
			
			for(String line : lines){
				Matcher deploymentDirMatcher = deploymentDirPattern.matcher(line);
				if(deploymentDirMatcher.find()){
					tongwebProcessInfoLineSet.add(line);
				}
			}
			
			logger.info(h.getIp()+tongwebProcessInfoLineSet);
			for(String tongwebProcessInfoLine:tongwebProcessInfoLineSet){
				Host.Middleware mw = new Host.Middleware();
				h.addMiddleware(mw);
				mw.setType("TongWeb");
				mw.setIp(h.getIp());
				
				//tongweb安装路径
				String deploymentDir = shell.parseInfoByRegex("Dtongweb\\.root\\s*=\\s*(\\S+)", tongwebProcessInfoLine, 1);
				mw.setDeploymentDir(deploymentDir);
				logger.info(h.getIp()+deploymentDir);
				//tongweb版本
				
				//JDK版本
				String javaCommand = shell.parseInfoByRegex("(/\\S+?/java)\\s", tongwebProcessInfoLine, 1);
				logger.info(h.getIp()+"	java命令路径	"+shell.parseInfoByRegex("(/\\S+?/java)\\s", tongwebProcessInfoLine, 1));
				
				shell.executeCommands(new String[]{javaCommand+" -version"});
				cmdResult = shell.getResponse();
				
				logger.info(h.getIp()+cmdResult);
				mw.setJdkVersion(shell.parseInfoByRegex("java\\s+version\\s+\"([^\"]+)\"", cmdResult, 1));
				logger.info(h.getIp()+"	JDK版本	"+shell.parseInfoByRegex("java\\s+version\\s+\"([^\"]+)\"", cmdResult, 1));
				//应用列表
				//采集 weblogic的应用列表
				List<App> appList = searchServiceIpAndPortForEachOf(collectAppListForTongweb(shell, deploymentDir,h),portListFromLoad);
				
				mw.setAppList(appList);
			}
			
		}
	}

	public  List<App> collectAppListForTongweb(final Shell shell,final String tongwebRoot, final Host h){
		String configPath = tongwebRoot+"/config/";
		List<App> appList = new ArrayList();
		if(existFileOnPath(configPath,"twns.xml",shell)){
			shell.executeCommands(new String[]{ "cat "+configPath+"twns.xml" });
	    	String cmdResult = shell.getResponse();
	    	
	    	logger.info(h.getIp()+cmdResult);
			
	    	String twnsDotXmlText = parseValidXmlTextFrom(cmdResult);
	    	logger.info(h.getIp()+"格式化后twns.xml内容	"+twnsDotXmlText);
	    	Document twnsDotXmlDoc = null;
			try {
				SAXReader reader = new SAXReader();
				reader.setValidation(false);
				reader.setEntityResolver(new EntityResolver(){
	
					@Override
					public InputSource resolveEntity(String publicId,
							String systemId) throws SAXException, IOException {
						// TODO Auto-generated method stub
						if(systemId.equals("http://www.tongtech.com/dtds/tongweb-config.dtd")){
							return new InputSource(new StringReader(""));
						}
						return null;
					}
					
				});
				
				twnsDotXmlDoc = reader.read(new StringReader(twnsDotXmlText));
			
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.info("xml文本串不合法");
			}
			/**
			 * 
			 */
			Element twnsNode = twnsDotXmlDoc.getRootElement();
			Element serverNode = twnsNode.element("server");
			Element webContainerNode = serverNode.element("web-container");
			List<Element> virtualServerNodeList = webContainerNode.elements("virtual-server");
			Element deploymentsNode = 	twnsNode.element("deployments");
			List<Element> webAppNodeList = deploymentsNode.elements("web-app");
			/**
			 * twns.xml中 web-app元素中的vs-names属性和 virtual-server元素的id属性相对应
			 * 1.多个virtual-server中，每个virtual-server所对应的http-listener
			 * 2.http-listener找到它的port,将port和virtual-server相关联
			 */
			
			Set<VirtualServer> virtualServerSet = new HashSet();
			for(Element virtualServerNode : virtualServerNodeList){
				String idAttrValue = virtualServerNode.attributeValue("id");
				String httpListenersAttrValue = virtualServerNode.attributeValue("http-listeners");
				
				VirtualServer virtualServer = new VirtualServer();
				virtualServer.setId(idAttrValue);
				String[] httpListenersArray  = httpListenersAttrValue.split(",");
				for(String httpListener : httpListenersArray){
					virtualServer.addHttpListener(httpListener);
				}
				
				virtualServerSet.add(virtualServer);
			}
			logger.info(h.getIp()+virtualServerSet);
			List<Element> httpListenerNodeList = webContainerNode.elements("http-listener");
		
			Set<HttpListener> httpListenerSet = new HashSet();
			for(Element httpListenerNode : httpListenerNodeList){
				String idAttrValue = httpListenerNode.attributeValue("id");
				String defaultVirtualServerAttrValue  = httpListenerNode.attributeValue("default-virtual-server");
				String portAttrValue = httpListenerNode.attributeValue("port"); 
				boolean securityEnabled = Boolean.parseBoolean(httpListenerNode.attributeValue("security-enabled"));
				
				HttpListener httpListener = new HttpListener();
				httpListener.setId(idAttrValue);
				httpListener.setDefaultVirtualServer(defaultVirtualServerAttrValue);
				httpListener.setPort(portAttrValue);
				httpListener.setSecurityEnabled(securityEnabled);
				httpListenerSet.add(httpListener);
				
			}
			
			logger.info(h.getIp()+httpListenerSet);
			Set<WebApp> webAppSet = new HashSet();
			
			for(Element webAppNode : webAppNodeList){
				String contextRoot = webAppNode.attributeValue("context-root");
				String sourcePath = webAppNode.attributeValue("source-path");
				String name = webAppNode.attributeValue("name");
				String vsNames = webAppNode.attributeValue("vs-names");
				
				WebApp webApp = new WebApp();
				webApp.setName(vsNames);
				webApp.setSourcePath(sourcePath.replaceAll(Pattern.quote("${tongweb.root}"), tongwebRoot));
				webApp.setContextRoot(contextRoot);
				
				String[] vsNamesArray = vsNames == null?new String[0]:vsNames.split(",");
				
				for(String vsName  : vsNamesArray){
					webApp.addVsName(vsName);
				}
				webAppSet.add(webApp);
			}
			logger.info(h.getIp()+webAppSet);
			
			Map<VirtualServer,Set<HttpListener>>	virtualServerAndHttpListenerSetMap = new HashMap();
	
			for(VirtualServer virtualServer : virtualServerSet){
				Set<HttpListener> httpListenerSetToVirtualServer = new HashSet();
				for(String httpListenerId : virtualServer.getHttpListenerSet()){
					
					for(HttpListener httpListener : httpListenerSet){
						if(httpListenerId.equals(httpListener.getId())){
							httpListenerSetToVirtualServer.add(httpListener);
						}
					}
				}
				virtualServerAndHttpListenerSetMap.put(virtualServer, httpListenerSetToVirtualServer);
			}
		
			logger.info(h.getIp()+virtualServerAndHttpListenerSetMap);
		
			for(WebApp webApp : webAppSet){
				for(String vsName : webApp.getVsNameSet()){
					VirtualServer virtualServer = new  VirtualServer(vsName);
					if(virtualServerAndHttpListenerSetMap.containsKey(virtualServer)){
						Set<HttpListener> httpListenerSetToVirtualServer = virtualServerAndHttpListenerSetMap.get(virtualServer);
						for(HttpListener httpListener : httpListenerSetToVirtualServer){
							App app = new App();
							appList.add(app);
							String schema = "http://";
							if(httpListener.isSecurityEnabled())
								schema = "https://";
							app.setAppName(schema+h.getIp()+":"+httpListener.getPort()+"/"+webApp.getContextRoot());//IP处需要虚拟主机名，目前无法获取
							app.setDir(webApp.getSourcePath());
							app.setPort(httpListener.getPort());
							app.setServiceIp("无");
							app.setServicePort("无");
							 
						}
					}
				}
			}
			
			
		}
		logger.info(h.getIp()+appList);
		return appList;
	}

	public  void collectMysqlForLinux(final Shell shell,final Host h){
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
	 * 获取CPU核数
	 * @author HP
	 * @param pattern
	 * @param cmdResult
	 * @return
	 */
	public  String parseLogicalCPUNumber(final String pattern,final String cmdResult){
		//获取
		Matcher m = Pattern.compile(pattern).matcher(cmdResult);
				int number = 0;
				while(m.find()){
					number += Integer.parseInt(m.group(1));
					
				}
				return number+"";
		    	
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
