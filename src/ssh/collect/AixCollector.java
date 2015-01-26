package ssh.collect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import constants.regex.Regex;

import host.Host;
import ssh.Shell;

public class AixCollector {
	private static Log logger = LogFactory.getLog(AixCollector.class);
	  public static String collectHostNameForAIX(final Shell shell,final Host h){
		  shell.executeCommands(new String[] { "uname -n" });
			String cmdResult = shell.getResponse();
			logger.info(h.getIp()+cmdResult);
			logger.info(h.getIp()+"正则表达式		"+Regex.AixRegex.HOST_NAME);
			logger.info(h.getIp()+"主机名="+shell.parseInfoByRegex(Regex.AixRegex.HOST_NAME,cmdResult,1));
			return shell.parseInfoByRegex(Regex.AixRegex.HOST_NAME,cmdResult,1);
	  }
		  
		  
}
