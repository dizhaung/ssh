package collect.action;

import host.Host;

import java.io.IOException;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import collect.HostListAccessException;

public class ShowHostDetailActionServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		 	String ip = req.getParameter("ip");
		 	List<Host> list =(List<Host>)getServletContext().getAttribute("hostlist");
		    
		    Host host = null;
		    //执行采集前或者执行采集过程中，查看任意一个IP主机的信息，进入错误页面提示没有主机信息
		    if(list != null&&ip != null) {
		    	 //查找是这个IP的主机
			    for(Host h : list){
			    	if(ip.trim().endsWith(h.getIp().trim())){
			    		host = h;
			    		break;
			    	}
			    }
		    }
		    req.setAttribute("host", host);
		    RequestDispatcher rd = req.getRequestDispatcher("/host_detail.jsp");
		    rd.forward(req, resp);
		   
		    
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(req, resp);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
