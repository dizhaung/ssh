package collect.action;

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

import net.sf.json.JSONArray;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ssh.SSHClient;

public class BatActionServlet extends HttpServlet {

	private static Log logger = LogFactory.getLog(BatActionServlet.class);
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(req,resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		//设置返回内容的编码格式
		resp.setCharacterEncoding("utf-8");
		String command = req.getParameter("command");
		
		//读取主机配置文件
		logger.info("---读取主机登录信息文件---");
		/**
		 * 第一次执行命令需要读取主机登录信息文件，后续的命令执行不重复读取配置文件，并且不会重复采集设备基本信息
		 */
		List<TinyHost> list  = (List<TinyHost>)req.getSession().getAttribute("tinyhostlist");
		boolean isNotEverRead = false;
		if(list == null){
			//读取主机登录信息文件
			list = TinyHost.getHostList(FileManager.readFile("/hostConfig.txt"));
			isNotEverRead = true;
		}
		//执行命令
		SSHClient.startBat(list,command,isNotEverRead);
		req.getSession().setAttribute("tinyhostlist", list);
		PrintWriter out = resp.getWriter();
		JSONArray o = JSONArray.fromObject(list);
		out.print(o);
		out.flush();
		
	}

}
