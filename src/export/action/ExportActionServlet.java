package export.action;

import host.FileManager;
import host.Host;

 
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

 
import export.io.File;
import export.io.POIReadAndWriteTool;

import ssh.SSHClient;

import net.sf.json.JSONArray;


public class ExportActionServlet extends HttpServlet {


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
		String userDir = req.getRealPath("/");
		 System.out.println(userDir);
		 Date currentTime    = new Date();
		 SimpleDateFormat timeFormater = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		 String time = timeFormater.format(currentTime);
	        String fileName = time;  
	        String fileType = "xlsx";   
	        POIReadAndWriteTool writer = POIReadAndWriteTool.getInstance();
	        writer.write((List<Host>)req.getSession().getServletContext().getAttribute("host"),new File(userDir+"export"+java.io.File.separator, fileName, fileType));
	        
	        PrintWriter out = resp.getWriter();
	       out.print("/ssh/export/"+fileName+"."+fileType);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	}
		
		
}
