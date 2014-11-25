package host;

import java.util.ArrayList;
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
