package collect.dwr;

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.Browser;
import org.directwebremoting.ScriptBuffer;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.ScriptSessionFilter;
import org.directwebremoting.ScriptSessions;
import org.directwebremoting.ServerContextFactory;




public class DwrPageContext {

	private static Log logger = LogFactory.getLog(DwrPageContext.class);
	private static final String[] SENDED_PAGE_REGEX = {"/index_mobile\\.html\\??","/index\\.html\\??"}; 
    public static void realtimeCollect(final String msg){
    	final String rootPath = ServerContextFactory.get().getContextPath();
    	//下面是创建一个javascript脚本 ， 相当于在页面脚本中添加了一句  functionName(msg);   
    	final  ScriptBuffer sb = new ScriptBuffer();  
        sb.appendScript("show(");  
        sb.appendData(msg);  
        sb.appendScript(")");  
      
        Browser.withAllSessionsFiltered(new ScriptSessionFilter(){

        	
			@Override
			public boolean match(ScriptSession scriptSession) {
				// TODO Auto-generated method stub
				String pageUrl = scriptSession.getPage();
				
				for(String pageRegex:SENDED_PAGE_REGEX){
					Pattern pagePattern = Pattern.compile(pageRegex);
					if( pagePattern.matcher(pageUrl).find())
						return true;
				}
				
				return Pattern.compile(rootPath+"\\??").matcher(pageUrl).find();
			}
        	
        },new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				ScriptSessions.addScript(sb);
			}
        	
        });
       
    }
    public static void realtimeBat(final String msg){
    	String rootPath = ServerContextFactory.get().getContextPath();
    	//下面是创建一个javascript脚本 ， 相当于在页面脚本中添加了一句  functionName(msg);   
    	final  ScriptBuffer sb = new ScriptBuffer();  
        sb.appendScript("show(");  
        sb.appendData(msg);  
        sb.appendScript(")");  
      
        Browser.withPage(rootPath+"/bat.html",new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				ScriptSessions.addScript(sb);
			}
        	
        });
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
