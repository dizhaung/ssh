package collect.dwr;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.ScriptBuffer;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.directwebremoting.proxy.dwr.Util;

public class DwrPageContext {

	private static Log logger = LogFactory.getLog(DwrPageContext.class);
	public static WebContext contex;
	public static Collection<ScriptSession> sessions ;
	public static  Util util;
	public static String functionName;
	/**
	 * 
	 * @param pagePath		要推送的页面页面
	 * @param functionName		推送消息到页面上，处理消息的回调函数
	 */
    @SuppressWarnings("deprecation")  
    public void init(String pagePath,String functionName){  
        //得到上下文  
        contex = WebContextFactory.get();
        String rootPath = contex.getServletContext().getContextPath();
       
        //得到要推送到 的页面  dwr3为项目名称
        sessions = contex.getScriptSessionsByPage(rootPath+pagePath);  
        logger.info(rootPath+pagePath);
        //
        util = new Util(sessions);  
        this.functionName = functionName;  
        
    }
    private static int count = 0;
    public static void run(String msg){
    	//下面是创建一个javascript脚本 ， 相当于在页面脚本中添加了一句  functionName(msg);   
        ScriptBuffer sb = new ScriptBuffer();  
        sb.appendScript(functionName+"(");  
        sb.appendData(msg);  
        sb.appendScript(")");  
      
        //推送 
        if(util != null){
        	 util.addScript(sb);  
        }
       
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
