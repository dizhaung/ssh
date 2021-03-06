package collect.action;

import host.FileManager;
import host.Host;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ssh.SSHClient;

import net.sf.json.JSONArray;


public class CollectActionServlet extends HttpServlet {

	static Log logger = LogFactory.getLog(CollectActionServlet.class);
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		//设置返回内容的编码格式
		resp.setCharacterEncoding("utf-8");
		
		logger.info("---读取主机登录信息文件---");
		//读取主机登录信息文件
		List<Host> list = Host.getHostList(FileManager.readFile("/hostConfig.txt"));
		logger.info("---采集---");
		//采集
		SSHClient.startCollect(list);
		getServletContext().setAttribute("hostlist", list);
		PrintWriter out = resp.getWriter();
		JSONArray o = JSONArray.fromObject(list);
		out.print(o);
		out.flush();
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String s = "ORACLE_HOME=/home/oracle/app/ora10g/product/10.2.0/db_1\r\n#";	
		Matcher m = Pattern.compile("ORACLE_HOME=([^\r\n]+)").matcher(s);
		if(m.find()){
			System.out.println(m.group(1));
		}
		
		System.out.print(s);
	}
		
		
}
