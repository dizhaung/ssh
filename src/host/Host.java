package host;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 配置文件每行的数据实体
 * @author tiger
 *
 *
 */
public class Host {
	/**
	 * 业务名称
	 */
	private String buss;
	
	/**
	 * IP地址
	 */
	private String ip;
	
	/**
	 * 用户名
	 */
	private String rootUser;
	
	/**
	 * 密码
	 */
	private String rootUserPassword;
	
	/**
	 * jk用户名
	 */
	private String jkUser;
	
	/**
	 * jk用户密码
	 */
	
	private String jkUserPassword;
	/**
	 * 主机类型,DB为数据库主机，APP为应用主机
	 */
	private String hostType;
	
	
	private String os;
	public String getOs() {
		return os;
	}
	public void setOs(String os) {
		this.os = os;
	}
	public String getBuss() {
		return buss;
	}
	public void setBuss(String buss) {
		this.buss = buss;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getRootUser() {
		return rootUser;
	}
	public void setRootUser(String rootUser) {
		this.rootUser = rootUser;
	}
	public String getRootUserPassword() {
		return rootUserPassword;
	}
	public void setRootUserPassword(String rootUserPassword) {
		this.rootUserPassword = rootUserPassword;
	}
	public String getJkUser() {
		return jkUser;
	}
	public void setJkUser(String jkUser) {
		this.jkUser = jkUser;
	}
	public String getJkUserPassword() {
		return jkUserPassword;
	}
	public void setJkUserPassword(String jkUserPassword) {
		this.jkUserPassword = jkUserPassword;
	}
	public String getHostType() {
		return hostType;
	}
	public void setHostType(String hostType) {
		this.hostType = hostType;
	}
	
		
	@Override
	public String toString() {
		return "Host [buss=" + buss + ", ip=" + ip + ", rootUser=" + rootUser
				+ ", rootUserPassword=" + rootUserPassword + ", jkUser="
				+ jkUser + ", jkUserPassword=" + jkUserPassword + ", hostType="
				+ hostType + ", os=" + os + ", detail=" + detail + ", dList="
				+ dList + ", mList=" + mList + "]";
	}

	/**
	 * 主机的详细信息
	 */
	private HostDetail detail;
	

	/**
	 * 转换主机基本信息  为可打印到excel的格式
	 * @author HP
	 * @return
	 */
	public  List<List<String>> revserseServerBaseInfo(){
		List<List<String>> table = new  LinkedList();
		List<String> tr = new LinkedList();
    	 
    	tr.add("主机类型");
    	tr.add( detail.getHostType() );
    	tr.add( "操作系统" );
    	tr.add( detail.getOs() );
    	tr.add( "IP地址" );
    	tr.add( detail.getOsVersion() );
    	table.add(tr);
    	
    	tr = new LinkedList();
    	tr.add( "主机名" );
    	tr.add( detail.getHostName() );
    	tr.add( "主机操作系统版本" );
    	tr.add( detail.getOsVersion() );
    	table.add(tr);
    	 
    	tr = new LinkedList();
    	tr.add("是否双机");
    	tr.add(detail.getIsCluster());
    	tr.add("双机虚地址");
    	tr.add(detail.getClusterServiceIP());
    	table.add(tr);
    	 
    	tr = new LinkedList();
    	tr.add("是否负载均衡");
    	tr.add(detail.getIsLoadBalanced());
    	tr.add("负载均衡虚地址");
    	tr.add(detail.getLoadBalancedVirtualIP());
    	table.add(tr);
    	
    	tr = new LinkedList();
    	tr.add("内存大小");
    	tr.add(detail.getMemSize());
    	tr.add("CPU个数");
    	tr.add(detail.getCPUNumber());
    	tr.add("CPU主频");
    	tr.add(detail.getCPUClockSpeed());
    	tr.add("CPU核数");
    	tr.add(detail.getLogicalCPUNumber());
    	table.add(tr);
    	return table;
    }
	public HostDetail getDetail() {
		return detail;
	}
	public void setDetail(HostDetail detail) {
		this.detail = detail;
	}

	public static class HostDetail{
		
		@Override
		public String toString() {
			return "HostDetail [os=" + os + ", hostType=" + hostType
					+ ", hostName=" + hostName + ", osVersion=" + osVersion
					+ ", memSize=" + memSize + ", CPUNumber=" + CPUNumber
					+ ", CPUClockSpeed=" + CPUClockSpeed
					+ ", logicalCPUNumber=" + logicalCPUNumber + ", cardList="
					+ cardList + ", fsList=" + fsList + "]";
		}

		private String os;
		private String hostType;
		private String hostName;
		private String osVersion;
		private String memSize;
		private String isCluster;
		private String clusterServiceIP;
		private String isLoadBalanced;
		private String loadBalancedVirtualIP;
		
		public String getIsLoadBalanced() {
			return isLoadBalanced;
		}

		public void setIsLoadBalanced(String isLoadBalanced) {
			this.isLoadBalanced = isLoadBalanced;
		}

		public String getLoadBalancedVirtualIP() {
			return loadBalancedVirtualIP;
		}

		public void setLoadBalancedVirtualIP(String loadBalancedVirtualIP) {
			this.loadBalancedVirtualIP = loadBalancedVirtualIP;
		}

		public String getIsCluster() {
			return isCluster;
		}

		public void setIsCluster(String isCluster) {
			this.isCluster = isCluster;
		}

		public String getClusterServiceIP() {
			return clusterServiceIP;
		}

		public void setClusterServiceIP(String clusterServiceIP) {
			this.clusterServiceIP = clusterServiceIP;
		}

		public String getOs() {
			return os;
		}

		public void setOs(String os) {
			this.os = os;
		}

		public String getHostType() {
			return hostType;
		}

		public void setHostType(String hostType) {
			this.hostType = hostType;
		}

		public String getHostName() {
			return hostName;
		}

		public void setHostName(String hostName) {
			this.hostName = hostName;
		}

		public String getOsVersion() {
			return osVersion;
		}

		public void setOsVersion(String osVersion) {
			this.osVersion = osVersion;
		}

		public String getMemSize() {
			return memSize;
		}

		public void setMemSize(String memSize) {
			this.memSize = memSize;
		}

		public String getCPUNumber() {
			return CPUNumber;
		}

		public void setCPUNumber(String cPUNumber) {
			CPUNumber = cPUNumber;
		}

		public String getCPUClockSpeed() {
			return CPUClockSpeed;
		}

		public void setCPUClockSpeed(String cPUClockSpeed) {
			CPUClockSpeed = cPUClockSpeed;
		}

		public String getLogicalCPUNumber() {
			return logicalCPUNumber;
		}

		public void setLogicalCPUNumber(String logicalCPUNumber) {
			this.logicalCPUNumber = logicalCPUNumber;
		}

		public List<NetworkCard> getCardList() {
			return cardList;
		}

		public void setCardList(List<NetworkCard> cardList) {
			this.cardList = cardList;
		}
		/**
		 * 转化网口列表为可打印的表格
		 * @return
		 */
		public List<List<String>> reverseCardListToTable(){
			List<List<String>> table = new LinkedList();
	    	List<String> tr = new LinkedList();
	    	table.add(tr);
	    	
	    	tr.add("序号");
	    	tr.add("网卡名称");
	    	tr.add("网卡类型");
	    	///打印表格主体
	    	int i = 0;
	    	if(cardList != null){
		    	for(Host.HostDetail.NetworkCard card:cardList){
		    		i++;
		    		tr = new LinkedList();
		        	tr.add(""+i);  //序号
		        	tr.add(card.getCardName());
		        	tr.add(card.getIfType());
		        	table.add(tr);
		    	}
	    	}
	    	return table;
		}
		/**
		 * 转化磁盘列表为可打印的表格
		 * @return
		 */
		public List<List<String>> reverseFsListToTable(){
			List<List<String>> table = new LinkedList();
	    	List<String> tr = new LinkedList();
	    	table.add(tr);
	    	
	    	tr.add("文件系统序号");
	    	tr.add("挂载点");
	    	tr.add("大小");
	    	tr.add("利用率");
	    	int i = 0;
	     
	    	if(fsList != null){ //测试用，采集后没有磁盘    fsList size是0
		    	for(Host.HostDetail.FileSystem fs:fsList){
		    		i++;
		    		tr = new LinkedList();
		        	tr.add(""+i);  //序号
		        	tr.add(fs.getMountOn());
		        	tr.add(fs.getBlocks());
		        	tr.add(fs.getUsed());
		        	table.add(tr);
		    	}
	    	}
	    	return table;
		}
		public List<FileSystem> getFsList() {
			return fsList;
		}

		public void setFsList(List<FileSystem> fsList) {
			this.fsList = fsList;
		}

		private String CPUNumber;
		private String CPUClockSpeed;
		private String logicalCPUNumber;
		private List<NetworkCard> cardList;
		private List<FileSystem> fsList;
		
		/**
		 * 网卡
		 * @author HP
		 *
		 */
		public static class  NetworkCard {
			private String cardName;
			private String ifType;
			@Override
			public String toString() {
				return "NetworkCard [cardName=" + cardName + ", ifType="
						+ ifType + "]";
			}
			public String getCardName() {
				return cardName;
			}
			public void setCardName(String cardName) {
				this.cardName = cardName;
			}
			public String getIfType() {
				return ifType;
			}
			public void setIfType(String ifType) {
				this.ifType = ifType;
			}
		}
		/**
		 * 文件系统
		 * @author HP
		 *
		 */
		public static class FileSystem{
			private String mountOn;
			private String blocks;
			private String used;
			public String getMountOn() {
				return mountOn;
			}
			public void setMountOn(String mountOn) {
				this.mountOn = mountOn;
			}
			public String getBlocks() {
				return blocks;
			}
			public void setBlocks(String blocks) {
				this.blocks = blocks;
			}
			public String getUsed() {
				return used;
			}
			public void setUsed(String used) {
				this.used = used;
			}
			@Override
			public String toString() {
				return "FileSystem [mountOn=" + mountOn + ", blocks=" + blocks
						+ ", used=" + used + "]";
			}
			
		}
	}
	
	
	private List<Database> dList;
	private List<Middleware> mList;
	/**
	 * @author HP
	 * @return 一个可打印的包含数据库信息的表格列表
	 */
	public List<List<List<String>>> reverseDatabaseList(){
		List<List<List<String>>>  tables = new LinkedList();
		  if(dList != null){
	        for(Host.Database db:dList){
	        	//每个数据库的基本信息
	        	List<List<String>> table = new  LinkedList();
	        	List< String> tr = new  LinkedList();
	        	tr.add("数据库类型");
	        	tr.add(db.getType());
	        	tr.add("数据库版本号");
	        	tr.add(db.getVersion());
	        	tr.add("服务器IP");
	        	tr.add(db.getIp());
	        	tr.add("数据库名称");
	        	tr.add(db.getDbName());
	        	table.add(tr);
	        	tr = new  LinkedList();
	        	tr.add("数据库部署路径");
	        	tr.add(db.getDeploymentDir());
	        	table.add(tr);
	        	tables.add(table);
	        	
	        	//每个数据库的数据文件列表
	        	 table = new  LinkedList();
	        	 ///标题
	        	 tr = new  LinkedList();
		   		  tr.add("数据库数据文件列表");
		   		  table.add(tr);
		   		  ///表头
		   		  tr = new  LinkedList();
		   		 tr.add("序号");
		   		 tr.add("文件路径");
		   		 tr.add("文件大小");
		   		  table.add(tr);
		   		  
		   		  ///表体
		        	  List<Database.DataFile> dfList = db.getDfList();
	        	  int i  = 0;
	        	  for(Database.DataFile df:dfList){
	        		  tr = new  LinkedList();
	        		  tr.add(++i+"");
	        		  tr.add(df.getFileName());
	        		  tr.add(df.getFileSize());
	        		  table.add(tr);
	        	 }
	        	tables.add(table);
	        	
	        }
		  }
		  return tables;
	}
	/**
	 * @author HP
	 * @return 一个可打印的包含中间件信息的表格列表
	 */
	public List<List<List<String>>> reverseMiddlewareList(){
		List<List<List<String>>>  tables = new LinkedList();
		  if(mList != null){
	        for(Middleware mw:mList){
	        	//每个中间件的基本信息
	        	List<List<String>> table = new  LinkedList();
	        	List< String> tr = new  LinkedList();
	        	tr.add("中间件类型");
	        	tr.add(mw.getType());
	        	tr.add("中间件版本号");
	        	tr.add(mw.getVersion());
	        	tr.add("服务器IP");
	        	tr.add(mw.getIp());
	        	table.add(tr);
	        	
	        	tr = new  LinkedList();
	        	tr.add("中间件部署路径");
	        	tr.add(mw.getDeploymentDir());
	        	tr.add("JDK版本");
	        	tr.add(mw.getJdkVersion());
	        	table.add(tr);
	        	tables.add(table);
	        	
	        	//每个中间件的应用列表
	        	 table = new  LinkedList();
	        	 ///标题
	        	 tr = new  LinkedList();
	        	 tr.add("应用列表");
	        	 table.add(tr);
	        	 ///表头
	        	 tr = new  LinkedList();
	        	 tr.add("序号");
	        	 tr.add("应用名称");
	        	 tr.add("部署路径");
	        	 table.add(tr);
	        	 ///表体
	        	 int i = 1;
	        	 for(Middleware.App app:mw.getAppList()){
	        		 tr = new  LinkedList();
		        	 tr.add(""+i++);
		        	 tr.add(app.getAppName());
		        	 tr.add(app.getDir());
		        	 table.add(tr);
	        	 }
	        	tables.add(table);
	        	
	        }
		  }
		  return tables;
	}
	public List<Database> getdList() {
		return dList;
	}
	public void setdList(List<Database> dList) {
		this.dList = dList;
	}
	

	public List<Middleware> getmList() {
		return mList;
	}
	public void setmList(List<Middleware> mList) {
		this.mList = mList;
	}

	/**
	 * 数据库的详细信息
	 * @author HP
	 *
	 */
	public static class Database{
		private String type;
		private String version;
		private String dbName;
		private String ip;
		private String deploymentDir;
		private String dataFileDir;
		private List<DataFile> dfList;
		
		
		@Override
		public String toString() {
			return "Database [type=" + type + ", version=" + version
					+ ", dbName=" + dbName + ", ip=" + ip + ", deploymentDir="
					+ deploymentDir + ", dataFileDir=" + dataFileDir
					+ ", dfList=" + dfList + "]";
		}


		public String getType() {
			return type;
		}


		public void setType(String type) {
			this.type = type;
		}


		public String getVersion() {
			return version;
		}


		public void setVersion(String version) {
			this.version = version;
		}


		public String getDbName() {
			return dbName;
		}


		public void setDbName(String dbName) {
			this.dbName = dbName;
		}


		public String getIp() {
			return ip;
		}


		public void setIp(String ip) {
			this.ip = ip;
		}


		public String getDeploymentDir() {
			return deploymentDir;
		}


		public void setDeploymentDir(String deploymentDir) {
			this.deploymentDir = deploymentDir;
		}


		public String getDataFileDir() {
			return dataFileDir;
		}


		public void setDataFileDir(String dataFileDir) {
			this.dataFileDir = dataFileDir;
		}


		public List<DataFile> getDfList() {
			return dfList;
		}


		public void setDfList(List<DataFile> dfList) {
			this.dfList = dfList;
		}

		/**
		 * 数据文件的详细信息
		 * @author HP
		 *
		 */
		public static class DataFile{
			private String fileName;
			private String fileSize;
			public String getFileName() {
				return fileName;
			}
			public void setFileName(String fileName) {
				this.fileName = fileName;
			}
			public String getFileSize() {
				return fileSize;
			}
			public void setFileSize(String fileSize) {
				this.fileSize = fileSize;
			}
			@Override
			public String toString() {
				return "DataFile [fileName=" + fileName + ", fileSize="
						+ fileSize + "]";
			}
			
		}
	}
	/**
	 * 中间件的详细信息
	 * @author HP
	 *
	 */
	public static class Middleware{
		private String type;
		private String version;
		private String ip;
		private String deploymentDir;
		private String jdkVersion;
		private List<App> appList;
		
		@Override
		public String toString() {
			return "Middleware [type=" + type + ", version=" + version
					+ ", ip=" + ip + ", deploymentDir=" + deploymentDir
					+ ", jdkVersion=" + jdkVersion + ", appList=" + appList
					+ "]";
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public String getDeploymentDir() {
			return deploymentDir;
		}

		public void setDeploymentDir(String deploymentDir) {
			this.deploymentDir = deploymentDir;
		}

		public String getJdkVersion() {
			return jdkVersion;
		}

		public void setJdkVersion(String jdkVersion) {
			this.jdkVersion = jdkVersion;
		}

		public List<App> getAppList() {
			return appList;
		}

		public void setAppList(List<App> appList) {
			this.appList = appList;
		}

		public static class App{
			private String appName;
			private String dir;
			private String port;
			private String serviceIp;
			private	String servicePort;
			
			
			public String getServicePort() {
				return servicePort;
			}
			public void setServicePort(String servicePort) {
				this.servicePort = servicePort;
			}
			public String getServiceIp() {
				return serviceIp;
			}
			public void setServiceIp(String serviceIp) {
				this.serviceIp = serviceIp;
			}
			public String getPort() {
				return port;
			}
			public void setPort(String port) {
				this.port = port;
			}
			public String getAppName() {
				return appName;
			}
			public void setAppName(String appName) {
				this.appName = appName;
			}
			public String getDir() {
				return dir;
			}
			public void setDir(String dir) {
				this.dir = dir;
			}
			
			 
			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				App other = (App) obj;
				if (port == null) {
					if (other.port != null)
						return false;
				} else if (!port.equals(other.port))
					return false;
				return true;
			}
			@Override
			public String toString() {
				return "App [appName=" + appName + ", dir=" + dir + "]";
			}
			
			
		}
	}
	
	/**
	 * 从主机配置文件内容构造主机List
	 * @author HP
	 * @param fileContent  主机配置文件读取的内容，每行\n分隔
	 * @return
	 */
	public static List<Host> getHostList(final String fileContent) {
		
          List<Host> hostList =  new ArrayList<Host>();
          
            String[] lines = fileContent.split("\n");
              for(String tempString:lines){
                System.out.println(tempString);
                if(!tempString.matches("^#.*")){  //注释掉的主机过滤掉不链接
                	String[] strs = tempString.split("\\|");
                    
                	Host host = new Host();
                	host.setBuss(strs[0]);
                	host.setIp(strs[1]);
                	host.setJkUser(strs[2]);
                	host.setJkUserPassword(strs[3]);
                	host.setRootUser(strs[4]);
                	host.setRootUserPassword(strs[5]);
                
                	hostList.add(host);
                }
               }
          
		return hostList;
    }
}
