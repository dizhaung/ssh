package collect.dwr;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.Container;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.ServerContextFactory;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.directwebremoting.event.ScriptSessionEvent;
import org.directwebremoting.event.ScriptSessionListener;
import org.directwebremoting.extend.ScriptSessionManager;



public class ScriptSessionListenerServlet extends HttpServlet {

	private static Log logger = LogFactory.getLog(ScriptSessionListenerServlet.class);
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		super.destroy();
	}

	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		super.init();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		logger.info("初始化ssl");
		//服务器启动时，加载ScriptSession监听，监听ScriptSession的创建和销毁
		//主要是，在页面刷新和关闭的时候，对服务器的ScriptSession销毁通知进行处理
		Container container = ServerContextFactory.get().getContainer();
		ScriptSessionManager manager = container.getBean(ScriptSessionManager.class);
		ScriptSessionListener listener = new ScriptSessionListener(){

			@Override
			public void sessionCreated(ScriptSessionEvent event) {
				// TODO Auto-generated method stub
				ScriptSession scriptSession = event.getSession();
				logger.info("create ScriptSessioin "+scriptSession.getId());
			}

			/**
			 * 当刷新   或者   关闭页面的情况下，销毁正在采集的线程和线程组
			 * @author HP
			 */
			@Override
			public void sessionDestroyed(ScriptSessionEvent event) {
				// TODO Auto-generated method stub
				ScriptSession scriptSession = event.getSession();
				logger.info("destroy ScriptSessioin "+scriptSession.getId());
				ThreadGroup batThreadGroup  = (ThreadGroup)scriptSession.getAttribute("batThreadGroup");
				if(batThreadGroup != null&&!batThreadGroup.isDestroyed()){
					Thread[] threads = new Thread[batThreadGroup.activeCount()];
					batThreadGroup.enumerate(threads);
					for(Thread thread:threads){
						thread.stop();
					}
					
				}
			}
			
		};
		manager.addScriptSessionListener(listener);
	}

}
