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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

 
import export.io.File;
import export.io.POIReadAndWriteTool;

import ssh.SSHClient;

import net.sf.json.JSONArray;


public class ExportActionServlet extends HttpServlet {

	private static Log logger = LogFactory.getLog(ExportActionServlet.class); 
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
		 List<Host> hostList =  (List<Host>)getServletContext().getAttribute("hostlist");
		 PrintWriter out = resp.getWriter();

		// 采集之后方能导出文件  并且有被采集的服务器
		if (hostList != null && hostList.size() > 0) {
			String userDir = req.getRealPath("/");
			System.out.println(userDir);
			Date currentTime = new Date();
			SimpleDateFormat timeFormater = new SimpleDateFormat(
					"yyyy-MM-dd-HH-mm-ss");
			String time = timeFormater.format(currentTime);
			String fileName = time;
			String fileType = "xlsx";
			logger.info("-----将服务器数据写入文件-----");
			POIReadAndWriteTool writer = POIReadAndWriteTool.getInstance();
			
			writer.write(hostList, new File(userDir + "export"
					+ java.io.File.separator, fileName, fileType));
			
			logger.info("导出文件的路径=/ssh/export/" + fileName + "." + fileType);
			out.print("/ssh/export/" + fileName + "." + fileType);
			out.flush();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	}
		
		
}
