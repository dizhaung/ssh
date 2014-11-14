package host;

import java.util.List;

/**
 * 配置文件每行的数据实体
 * @author tiger
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
				+ hostType + ", os=" + os + ", detail=" + detail + "]";
	}


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
}
