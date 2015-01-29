package export.action;

import host.Host;
import host.TinyHost;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import export.io.File;
import export.io.POIReadAndWriteTool;

public class BatExportActionServlet extends HttpServlet {

	private static Log logger = LogFactory.getLog(BatExportActionServlet.class);
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
		 List<TinyHost> hostList =  (List<TinyHost>)req.getSession().getAttribute("tinyhostlist");
		 PrintWriter out = resp.getWriter();
		 
		// 采集之后方能导出文件  并且有被采集的服务器
		if (hostList != null && hostList.size() > 0) {
			String userDir = req.getRealPath("/");
			Date currentTime = new Date();
			SimpleDateFormat timeFormater = new SimpleDateFormat(
					"yyyy-MM-dd-HH-mm-ss");
			String time = timeFormater.format(currentTime);
			String fileName =  "command"+ time;
			String fileType = "xlsx";
			logger.info("-----将服务器数据写入文件-----");
			POIReadAndWriteTool writer = POIReadAndWriteTool.getInstance();
			
			writer.write(hostList,TinyHost.class, new File(userDir + "export"
					+ java.io.File.separator, fileName, fileType));
			
			logger.info("导出文件的路径=/ssh/export/" + fileName + "." + fileType);
			out.print("/ssh/export/" + fileName + "." + fileType);
			out.flush();
		}
	}

}
