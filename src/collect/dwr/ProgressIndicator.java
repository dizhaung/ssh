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
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;




public class ProgressIndicator {
	
	private static Log logger = LogFactory.getLog(ProgressIndicator.class);
	private String scriptSessionId;
    public String getScriptSessionId() {
		return scriptSessionId;
	}
	public void setScriptSessionId(String scriptSessionId) {
		this.scriptSessionId = scriptSessionId;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((scriptSessionId == null) ? 0 : scriptSessionId.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProgressIndicator other = (ProgressIndicator) obj;
		if (scriptSessionId == null) {
			if (other.scriptSessionId != null)
				return false;
		} else if (!scriptSessionId.equals(other.scriptSessionId))
			return false;
		return true;
	}
	public  void realtimeCollect(final String msg){
    	final String rootPath = ServerContextFactory.get().getContextPath();
    	//下面是创建一个javascript脚本 ， 相当于在页面脚本中添加了一句  functionName(msg);   
    	final  ScriptBuffer sb = new ScriptBuffer();  
        sb.appendScript("show(");  
        sb.appendData(msg);  
        sb.appendScript(")");  
      /*
        index.html index_mobile.html等采集页面
       	点击执行按钮后，更新采集进度条
      	因为有可能，同一个浏览器中会打开多个相同的页面，这样有可能有多个执行按钮被同时点击
      	但他们各自都是独立的采集过程，采集进度也是独立的
      	因此， 要针对同一个会话的各个不同的脚本会话来更新进度 
       */
        Browser.withSession(scriptSessionId,new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				ScriptSessions.addScript(sb);
			}
        	
        });
       
    }
    public static void realtimeBat(final String msg){
    	WebContext context = WebContextFactory.get();
    	//下面是创建一个javascript脚本 ， 相当于在页面脚本中添加了一句  functionName(msg);   
    	final  ScriptBuffer sb = new ScriptBuffer();  
        sb.appendScript("show(");  
        sb.appendData(msg);  
        sb.appendScript(")");  
      
        String scriptSesssionId = context.getScriptSession().getId();
        Browser.withSession(scriptSesssionId,new Runnable(){

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
