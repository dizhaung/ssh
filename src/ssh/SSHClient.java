package ssh;

import host.FileManager;
import host.Host;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oro.text.regex.MalformedPatternException;

import ssh.collect.HostCollector;


import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import constants.regex.Regex.CommonRegex;
 
import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
 
public class SSHClient {
 
	private static Log logger = LogFactory.getLog(SSHClient.class);
	private Session session;
	private ChannelShell channel;
	   
    private static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
    private static final String ENTER_CHARACTER = "\r";
    public static final int SSH_PORT = 22;
    private List<String> lstCmds = new ArrayList<String>();
    public static final String[] COMMAND_LINE_PROMPT_REGEX_TEMPLATE = new String[] { "~]#", "~#", "#",
        ":~#", "/$", ">","\\$","SQL>"};//加入\\$匹配登录后命令提示符为\$的情况
    private String[] commandLinePromptRegex = null;
    private Expect4j expect = null;
    private StringBuilder buffer = null;
    private String userName;
    private String password;
    private String host;
 
    public void setCommandLinePromptRegex(String[] commandLinePromptRegex){
    	this.commandLinePromptRegex = commandLinePromptRegex;
    }
    /**
     *
     * @param host
     * @param userName
     * @param password
     */
    public SSHClient(String host, String userName, String password) {
        this.host = host;
        this.userName = userName;
        this.password = password;
    }
   /**
    * 
    * @param cmdsToExecute
    * @return
    * @throws ShellException     Shell创建或者执行失败异常    ，失败有下面几个可能得原因导致的	1.网络不通 		2用户名或者密码错误	3创建输入输出流错误	4expect创建或者执行异常
    */
    public String execute(List<String> cmdsToExecute) throws ShellException {
    	//没有特别的正则   则使用匹配命令提示的标准正则
    	if(commandLinePromptRegex == null){
    		commandLinePromptRegex = COMMAND_LINE_PROMPT_REGEX_TEMPLATE;
    	}
        this.lstCmds = cmdsToExecute;
       
        Closure closure = new Closure() {
            public void run(ExpectState expectState) throws Exception {
            	 buffer = new StringBuilder();
                buffer.append(expectState.getBuffer());
            }
        };
        List<Match> lstPattern =  new ArrayList<Match>();
        for (String regexElement : commandLinePromptRegex) {
            try {
                Match mat = new RegExpMatch(regexElement, closure);
                lstPattern.add(mat);
            } catch (MalformedPatternException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
 
        
        	/**
        	 * 每执行一条命令都重新连接服务器，异常的低效，为了兼容复杂的shell命令
        	 * 例如：prtconf |grep "Good Memory Size:"
        	 * 执行命令的错误提示：
        	 * expect4j.BlockingConsumer run
				信息: Found EOF to stop while loop
        	 */
        	
        try {
			expect = SSH();
			
            boolean isSuccess = true;
            for(String strCmd : lstCmds) {
                isSuccess = isSuccess(lstPattern,strCmd);
                if (!isSuccess) {
                    isSuccess = isSuccess(lstPattern,strCmd);
                }
            }
 
            checkResult(expect.expect(lstPattern));
        } catch (ShellException e) {
				// TODO Auto-generated catch block
				throw e;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ShellException(e.toString(),e);
		} finally {
        	//去掉特殊正则，默认回归标准的提示符匹配的正则
			commandLinePromptRegex = null;
        	this.disconnect();
        }
        return buffer.toString();
    }
    /**
     *
     * @param objPattern
     * @param strCommandPattern
     * @return
     */
    private boolean isSuccess(List<Match> objPattern,String strCommandPattern) {
        try {
            boolean isFailed = checkResult(expect.expect(objPattern));
 
            if (!isFailed) {
                expect.send(strCommandPattern);
                expect.send(ENTER_CHARACTER);
                return true;
            }
            return false;
        } catch (MalformedPatternException ex) {
            ex.printStackTrace();
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
    private final static long DEFAULT_TIMEOUT = 10*1000;
    
    /**
     *
     * @param hostname
     * @param username
     * @param password
     * @param port
     * @return
     * @throws ShellException 		shell创建和执行命令失败抛出异常   
     */
    private Expect4j SSH() throws ShellException {
        JSch jsch = new JSch();
        
        try {
			session = jsch.getSession(userName, host, SSH_PORT);
			
	        if (password != null) {
	            session.setPassword(password);
	        }
	        Hashtable<String,String> config = new Hashtable<String,String>();
	        config.put("StrictHostKeyChecking", "no");
	        //据stackoverflow.com上的解答，加入下面一条配置项
	        config.put("PreferredAuthentications", 
	                "publickey,keyboard-interactive,password");
	        session.setConfig(config);
	        session.connect(60000);
	        channel = (ChannelShell) session.openChannel("shell");
	        expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
	        expect.setDefaultTimeout(DEFAULT_TIMEOUT);
	        channel.connect();
        } catch (JSchException e) {
			// TODO Auto-generated catch block
			 
			throw new ShellException(e.toString(),e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			 
			throw new ShellException(e.toString(),e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			 
			throw new ShellException(e.toString(),e);
		}
        return expect;
    }
    public void disconnect(){
    	 if (expect!=null) {
             expect.close();
         }
     
        if(channel!=null){
            channel.disconnect();
        }
        if(session!=null){
            session.disconnect();
        }
    }
    /**
     *
     * @param intRetVal
     * @return
     */
    private boolean checkResult(int intRetVal) {
        if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
            return true;
        }
        return false;
    }
         
    /**
     * 采用标准提示符匹配的正则和特殊正则来构造
     * @param template
     * @param specific
     * @return
     */
    public String[] getPromptRegexArrayByTemplateAndSpecificRegex(final String[] template,final String[] specific){
    	String[] regexArray = new String[template.length+specific.length];
    	System.arraycopy(template, 0, regexArray, 0, template.length);
    	System.arraycopy(specific, 0, regexArray, template.length, specific.length);
    	return regexArray;
    }
}
