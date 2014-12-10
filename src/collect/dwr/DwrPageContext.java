package collect.dwr;

import java.util.Collection;

import org.directwebremoting.ScriptBuffer;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.directwebremoting.proxy.dwr.Util;

public class DwrPageContext {

	public static WebContext contex;
	public static Collection<ScriptSession> sessions ;
	public static  Util util;
	public static String functionName;
    @SuppressWarnings("deprecation")  
    public void init(String pagePath,String functionName){  
        //得到上下文  
        contex = WebContextFactory.get();  
        System.out.println(pagePath);  
        //得到要推送到 的页面  dwr3为项目名称 ， 一定要加上。  
        sessions = contex.getScriptSessionsByPage(pagePath);  
          
        //不知道该怎么解释这个 ，   
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
