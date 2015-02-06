package ssh.collect;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
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



import constants.Format;
import constants.Unit;
import constants.regex.Regex;
import constants.regex.Regex.AixRegex;
import constants.regex.Regex.CommonRegex;

import host.Host;
import host.Host.Database;
import host.Host.Database.DataFile;
import host.Host.HostDetail;
import host.Host.HostDetail.FileSystem;
import host.Host.HostDetail.NetworkCard;
import host.Host.Middleware;
import host.Host.Middleware.App;
import host.PortLoadConfig;
import host.command.CollectCommand;
import host.command.CollectCommand.AixCommand;
import host.database.db2.Tablespace;
import host.database.db2.Tablespace.Container;
import ssh.SSHClient;
import ssh.Shell;
import ssh.ShellException;

public class AixCollector extends HostCollector {
	private static Log logger = LogFactory.getLog(AixCollector.class);
	  public  String collectHostNameForAIX(final Shell shell,final Host h){
		  shell.executeCommands(new String[] { "uname -n" });
			String cmdResult = shell.getResponse();
			logger.info(h.getIp()+cmdResult);
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.HOST_NAME);
			logger.info(h.getIp()+"主机名="+shell.parseInfoByRegex(Regex.AixRegex.HOST_NAME,cmdResult,1));
			return shell.parseInfoByRegex(Regex.AixRegex.HOST_NAME,cmdResult,1);
	  }
		  
	   public void collectTongweb(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	 public void collectTomcat(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 采集DB2数据库
	 * 
	 * @date 2015-1-8下午4:49:58
	 * @author HP
	 */
	public void collectDB2(final Shell shell,final Host h){
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

	public  List<String> collectAllInstanceUserListForDB2(final Shell shell,final Host h){
		shell.executeCommands(new String[] { "strings /var/db2/global.reg" });
		String cmdResult = shell.getResponse();
		
		logger.info(cmdResult);
		Matcher db2InstanceUserMatcher  = Pattern.compile(Regex.AixRegex.DB2_INSTANCE_USER.toString()).matcher(cmdResult);
		
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

	public  boolean showContainersDetailSuccess(final String cmdResult){
		return match("Tablespace\\s+Containers\\s+for\\s+Tablespace",cmdResult);
	}

	public  boolean showTablespacesDetailSuccess(final String cmdResult){
		return match("Tablespaces\\s+for\\s+Current\\s+Database",cmdResult);
	}

	public  boolean connectDatabaseSuccess(final String cmdResult){
		return match("Database\\s+Connection\\s+Information",cmdResult);
	}

	/**
	     * @date 2015-1-9下午4:46:27
	     * @author HP
	     */
	   public void collectHostDetail(final Shell shell,final SSHClient ssh,final Host h,final List<PortLoadConfig> portListFromLoad){
	    
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
			
			ssh.setCommandLinePromptRegex(ssh.getPromptRegexArrayByTemplateAndSpecificRegex(SSHClient.COMMAND_LINE_PROMPT_REGEX_TEMPLATE,new String[]{"Full Core"}));
			
			cmdsToExecute.add("prtconf");
			
			try {
				cmdResult = ssh.execute(cmdsToExecute);
			} catch (ShellException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
	 * 采集weblogic的信息
	 * @date 2015-1-9下午4:45:15
	 * @author HP
	 */
	public  void collectWeblogic(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){
	
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

	/**
	* 
	* @param shell
	* @param h
	* @date 2015-1-9下午3:42:34
	* @author HP
	*/
	public void collectOracle(Shell shell,final Host h){
	
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
	 * 
	 * @param shell
	 * @param h
	 * @return
	 * @date 2015-1-12下午6:02:51
	 * @author HP
	 */
	public  Set<String>	collectRunningInstanceUserSetForDB2(final Shell shell,final Host h){
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
	public void collectWebSphere(final Shell shell,final Host h,final List<PortLoadConfig> portListFromLoad){
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

	public  List<App> collectAppListForWebsphere(final Shell shell,String userInstallRoot,final Host h){
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
		    	    		
		    	    		
		    	    		String hostName = collectHostNameForAIX(shell, h);//Junit单元测试和正式部署都可以使用这种方式，可选的方法可以通过h.getDetail().getHostName()得到主机名
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

	/**
	 * 采集weblogic的应用名字和部署路径
	 * @param shell
	 * @param userProjectsDirSource   user_projects的上一层目录
	 * @return
	 */
	public  List<Host.Middleware.App> collectWeblogicAppListForAIX(final Shell shell,final String userProjectsDirSource,final Host h){
	
		
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

	public  boolean isNotDeploymentManager(final String line,final String regex){
		return !match(line,regex);
	}

	public  Set<String> parseDirectoryNameSetFromDirDetail(final String cmdResult,final Shell shell){
		
		Set<String> dirNameSet = new HashSet<String>();
		String[] dirDetailInfoArray = cmdResult.split(Regex.CommonRegex.LINE_REAR.toString());
		if(dirDetailInfoArray.length > 2){
			for(int i = 1,size = dirDetailInfoArray.length -1;i < size;i++){
	    		dirNameSet.add(shell.parseInfoByRegex("(\\S+)$", dirDetailInfoArray[i], 1));
			}
		}
		
		
		return dirNameSet;
	}	  
}
