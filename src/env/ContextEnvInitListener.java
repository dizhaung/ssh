package env;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Application Lifecycle Listener implementation class TrapServiceInitListener
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
