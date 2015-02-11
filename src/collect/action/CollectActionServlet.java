package collect.action;

import host.FileManager;
import host.Host;
import host.PortLoadConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import collect.CollectedResource;
import collect.dwr.DwrPageContext;
import collect.model.HintMsg;

import ssh.SSHClient;
import ssh.Shell;
import ssh.ShellException;
import ssh.collect.AixCollector;
import ssh.collect.HostCollector;
import ssh.collect.LinuxCollector;
import ssh.collect.LoadBalancerCollector;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


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
		this.startCollect(list);
		getServletContext().setAttribute("hostlist", list);
		PrintWriter out = resp.getWriter();
		JSONArray o = JSONArray.fromObject(list);
		out.print(o);
		out.flush();
		
	}

	/**
	 * 采集
	 * @param list
	 */
	private  void startCollect(final List<Host> list){
		//向用户传递采集进度
		int maxNum = list.size();
		
		if(maxNum == 0){
			HintMsg msg = new HintMsg(0,0,"无","");
			DwrPageContext.realtimeCollect(JSONObject.fromObject(msg).toString());
			HostCollector.logger.info(msg);
			return;
		} 
			//采集负载均衡配置
		HostCollector.logger.info("------开始采集负载均衡-----");
		StringBuilder allLoadBalancerFarmAndServerInfo  = LoadBalancerCollector.collectLoadBalancer();
		HostCollector.logger.info("------开始采集主机-----");
		this.collectHosts(list,allLoadBalancerFarmAndServerInfo);
	}

	/**
	 * 采集主机
	 * @param list	主机列表
	 * @param allLoadBalancerFarmAndServerInfo   负载均衡上采集到的配置信息
	 */
	private  void collectHosts(final List<Host> list,final StringBuilder allLoadBalancerFarmAndServerInfo){
		
		final int maxNum = list.size(), nowNum = 0;
		final CollectedResource resource = new CollectedResource(0); 
		if(list.size() > 0){
			for (Iterator<Host> it = list.iterator();it.hasNext();) {
				final Host h = it.next();
				
				Thread thread = new Thread(new Runnable(){
	
					@Override
					public void run() {
						// TODO Auto-generated method stub
						
						logger.info(h);
		    			// 登录
		    			SSHClient ssh = new SSHClient(h);
	
		    			// 建立连接
		    			Shell shell;
		    			try {
		    				shell = new Shell(null);
		    				shell.setTimeout(2*1000);
		    				
		    				logger.error("	连接到 	"+h.getIp());
		    				HostCollector collector = new HostCollector();
		    				collector.grantRoot(shell,h);
			    			
		    				collector.collectOs(shell,h);
			    			
			    			List<PortLoadConfig> portListFromLoad  =   LoadBalancerCollector.parsePortList(h, allLoadBalancerFarmAndServerInfo.toString());
			    			if("AIX".equalsIgnoreCase(h.getOs())){
			    				 collector = new AixCollector();
			    				collector.collect(shell,ssh,h,portListFromLoad);
			    			}else if("LINUX".equalsIgnoreCase(h.getOs())){
			    				 collector = new LinuxCollector();
			    				collector.collect(shell,ssh,h,portListFromLoad);
			    			}else if("HP-UNIX".equalsIgnoreCase(h.getOs())){
			    				
			    			}
			    			logger.info(h);
			    			shell.disconnect();
		    			} catch (ShellException e) {
		    				// TODO Auto-generated catch block
		    				logger.error("无法采集主机"+h.getIp()+"，连接下一个主机");
		    				e.printStackTrace();
		    			}finally{
		    				synchronized(resource){
		    					logger.info(h.getIp()+"主机采集完毕---------");
		    					resource.increase();
		    				}
		    			}
		    			
					}
					
				},h.getIp());
				
				thread.start();
	
			}
			 
			synchronized(resource){
				
				while(resource.getNumber() < maxNum){
					HintMsg msg = new HintMsg(resource.getNumber(),maxNum,"","当前主机采集进度,已完成"+resource.getNumber()+"个,共"+maxNum+"个");
					DwrPageContext.realtimeCollect(JSONObject.fromObject(msg).toString());
					 
					logger.info(msg);
					try {
						resource.wait();
					 } catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			
			}
		}
		
		HintMsg msg = new HintMsg(resource.getNumber(),maxNum,"采集完毕","当前主机采集进度");
		DwrPageContext.realtimeCollect(JSONObject.fromObject(msg).toString());
		logger.info(msg);
	}
		
}
