package collect.dwr;

import java.util.Collection;

import org.directwebremoting.Browser;
import org.directwebremoting.ScriptBuffer;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.ScriptSessionFilter;

public class ReverseAjaxServerSide {

	public void push(final String msg){
		Browser.withAllSessionsFiltered(new ScriptSessionFilter() {  
            public boolean match(ScriptSession session){  
                if (session.getAttribute("userId") == null)  
                    return false;  
                else {
                	System.out.println(userId);
                	return (session.getAttribute("userId")).equals(userId);
                }
                      
            }  
        }, new Runnable(){  
              
            private ScriptBuffer script = new ScriptBuffer();  
              
            public void run(){  
                  
                script.appendCall("showMessage", autoMessage);  
                  
                Collection<ScriptSession> sessions = Browser.getTargetSessions();  
                  
                for (ScriptSession scriptSession : sessions){  
                    scriptSession.addScript(script);  
                }  
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
