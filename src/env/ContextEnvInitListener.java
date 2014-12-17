package env;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 初始化log4j日志文件输出根路径
 * @author HP
 *
 */
public class ContextEnvInitListener implements ServletContextListener {

    @Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		System.getProperties().remove("log4jdir");
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		String path = arg0.getServletContext().getRealPath("/");
		System.setProperty("log4jdir", path);
	}

	/**
     * Default constructor. 
     */
    public ContextEnvInitListener() {
        // TODO Auto-generated constructor stub
    }
	
}
