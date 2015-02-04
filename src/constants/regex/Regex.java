package constants.regex;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 所有主机类型的正则作为实现Regex接口   并  作为整个接口的常量
 * 确保使用统一的接口，非实现Regex接口的enum或者class不能当做正确的参数传入
 * @author HP
 *
 */
public interface Regex {

	
	/**
	 * 连缀正则
	 * @param regex
	 * @return
	 */
	Regex plus(final Regex regex);
	
	/**
	 * 所有主机类型公用的正则
	 * @author HP
	 *
	 */
	enum CommonRegex implements Regex {
		HOST_OS("\\s*uname\r\n(.*)\r\n"),HOST_TYPE("\\s*uname -M\r\n(.*)\r\n")
		,
		LINE_REAR("[\r\n]+")
		,
		BLANK_DELIMITER("\\s+")
		,
		FARM_PORT_PREFIX("appdirector farm server table create (.*?) ")
		,
		FARM_PORT_SUFFIX(" (\\d{1,5})")
		,
		SERVICEIP_PORT("appdirector l4-policy table create (.*?) (TCP|UDP) (\\d{1,5}) (\\d{1,3}\\.){3}\\d{1,3}\\\\\\s+ .*? -fn ")
		,
		LINE_COMMENT("^#.*")
		,
		ITEM_DELIMITER("\\|")
		,
		DIR_NAME("(\\S+)$")
		;
		
		private final String regex;
		private CommonRegex(String regex){
			this.regex = regex;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return regex;
		}

		/**
		 * regex enum 做为+运算的左值
		 * @param regex
		 */
		@Override
		public Regex plus(Regex regex) {
			// TODO Auto-generated method stub
			return RegexEntity.newInstance().plus(this).plus(regex);
		}
		
	}
	/**
	 * aix正则
	 * @author HP
	 *
	 */
	public enum AixRegex implements Regex {
		HOST_TYPE("\\s*uname -M\r\n(.*)\r\n"),
		HOST_NAME("\\s*uname -n\r\n(.*)\r\n"),
		OS_MAIN_VERSION("\\s*uname -v\r\n(.*)\r\n"),
		OS_SECOND_VERSION("\\s*uname -r\r\n(.*)\r\n"),
		MEMORY_SIZE("[Gg]ood\\s+[Mm]emory\\s+[Ss]ize:\\s+(\\d+\\s+[MmBbKkGg]{0,2})"),
		CPU_NUMBER("[Nn]umber\\s+[Oo]f\\s+[Pp]rocessors:\\s+(\\d+)"),
		CPU_CLOCK_SPEED("[Pp]rocessor\\s+[Cc]lock\\s+[Ss]peed:\\s+(\\d+\\s+[GgHhMmZz]{0,3})")
		,LOGICAL_CPU_NUMBER("0\\s+(\\d+\\s*)+")
		,
		CLUSTER_SERVICE_IP("Service\\s+IP\\s+Label\\s+[\\d\\w]+:\\s+IP\\s+address:\\s+((\\d{1,3}\\.){3}\\d{1,3})")
		,
		NETCARD_NAME("^(ent\\d+)")
		,
		
		
		ORACLE_USER_DIR(":/(.+):")
		,
		ORACLE_HOME_DIR("ORACLE_HOME=([^\r\n]+)")
		,
		ORACLE_BASE_DIR("ORACLE_BASE=([^\r\n]+)")
		,
		ORACLE_SID("ORACLE_SID=([^\r\n]+)")
		,
		ORACLE_DATAFILE_LOCATION("\\s+(/.*)\\s+")
		,
		ORACLE_DATAFILE_SIZE("\\s+(\\d+)MB\\s+")
		,
		ORACLE_VERSION("((\\d+\\.?)+\\d*)")
		,
		
		
		
		
		WEBLOGIC_DEPLOY_DIR("-Djava.security.policy=(/.+)/server/lib/weblogic.policy")
		,
		WEBLOGIC_VERSION("([\\d.]+)$")
		,
		WEBLOGIC_JDK_JAVA_COMMAND("(/.+/bin/java)")
		,
		WEBLOGIC_JDK_VERSION("java\\s+version\\s+\"([\\w.]+)\"")
		,
		WEBLOGIC_ROOT_DIR("-Djava.security.policy=(/.+)/[\\w.]+/server/lib/weblogic.policy")
		,
		WEBLOGIC_10_APP_NAME("<app-deployment>[\\s\\S]*?<name>(.*)</name>[\\s\\S]*?</app-deployment>")
		,
		WEBLOGIC_10_APP_DIR("<app-deployment>[\\s\\S]*?<source-path>(.*)</source-path>[\\s\\S]*?</app-deployment>")
		,
		WEBLOGIC_10_APP_PORT("<[Ll]isten-[Pp]ort>(\\d{1,5})</[Ll]isten-[Pp]ort>")
		
		
		,
		WEBLOGIC_8_APP_NAME("<[Aa]pplication[\\s\\S]+?[Nn]ame=\"([\\S]+)\"")
		,
		WEBLOGIC_8_APP_DIR("<[Aa]pplication[\\s\\S]+?[Pp]ath=\"([\\S]+)\"")
		,
		WEBLOGIC_8_APP_PORT("[Ll]isten[Pp]ort\\s*=\\s*[\"']?(\\d{1,5})[\"']")
		;
		private final String regex;
		private AixRegex(String regex){
			this.regex = regex;
		}
		private AixRegex(Regex regex){
			this.regex = regex.toString();
		}
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return regex;
		}
		/**
		 * regex enum 做为+运算的左值
		 * @param regex
		 */
		@Override
		public Regex plus(Regex regex) {
			// TODO Auto-generated method stub
			return RegexEntity.newInstance().plus(this).plus(regex);
		}
	}
	/**
	 * linux主机正则
	 * @author HP
	 *
	 */
	public enum LinuxRegex implements Regex {
		HOST_TYPE("\\s*uname -M\r\n(.*)\r\n"),
		
		MYSQL_DEPLOYMENT_DIR("--basedir=(\\S+)"),
		MYSQL_DATA_DIR("--datadir=(\\S+)"),
		MYSQL_VERSION("Distrib\\s*(\\S+),")
		;
		private final String regex;
		private LinuxRegex(String regex){
			this.regex = regex;
		}
		
		
		private LinuxRegex(Regex regex){
			this.regex = regex.toString();
		}
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return regex;
		}

		/**
		 * regex enum 做为+运算的左值
		 * 
		 * @param regex
		 */
		@Override
		public Regex plus(Regex regex) {
			// TODO Auto-generated method stub
			return RegexEntity.newInstance().plus(this).plus(regex);
		}
		
	}
}
