package poll.action;

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

import ssh.SSHClient;

import net.sf.json.JSONArray;


public class PollActionServlet extends HttpServlet {

	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doGet(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		//设置返回内容的编码格式
		resp.setCharacterEncoding("utf-8");
		
		String userDir = req.getRealPath("/");
		//读取主机登录信息文件
		List<Host> list = FileManager.getHostList(userDir+"/WEB-INF/classes/config.txt");
		//采集
		SSHClient.startPoll(list);
		req.getSession().getServletContext().setAttribute("host", list);
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
		String s = "# prtconf |grep \"Good Memory Size:\"\r\nGood Memory Size: 32000 MB\r\n#";	
		Matcher m = Pattern.compile("\\s*prtconf \\|grep \"Good Memory Size:\"\r\n(.*)\r\n").matcher(s);
		if(m.find()){
			System.out.println(m.group(1));
		}
		System.out.print(s);
	}
		
		
}
