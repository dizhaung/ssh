package collect.dwr;

import host.FileManager;
import host.Host;
import host.TinyHost;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import ssh.bat.CommandExecutor;

public class RemoteCommandExecutor {

	private static Log logger = LogFactory.getLog(RemoteCommandExecutor.class);
	
	
	public String batCommand(final String command){
		//设置返回内容的编码格式
		WebContext context =WebContextFactory.get();
		ScriptSession scriptSession = context.getScriptSession();
		//读取主机配置文件
		logger.info("---读取主机登录信息文件---");
		/**
		 * 第一次执行命令需要读取主机登录信息文件，后续的命令执行不重复读取配置文件，并且不会重复采集设备基本信息
		 * 但是当再次刷新页面的时候，执行主机命令的批处理则需要读取主机配置文件
		 * 因为，刷新页面，将会重新创建一个ScriptSession
		 */
		List<TinyHost> list  = (List<TinyHost>)scriptSession.getAttribute("tinyhostlist");
		boolean isNotEverRead = false;
		if(list == null){
			//读取主机登录信息文件
			list = TinyHost.getHostList(FileManager.readFile("/batHostConfig.txt"));
			isNotEverRead = true;
		}
		//执行命令
		CommandExecutor.startBat(list,command,isNotEverRead);
		scriptSession.setAttribute("tinyhostlist", list);
		
		JSONArray commandResults = JSONArray.fromObject(list);
		return commandResults.toString();
	}

}
