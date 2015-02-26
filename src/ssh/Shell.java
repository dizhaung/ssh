package ssh;

import host.FileManager;
import host.Host;
import host.Host.HostDetail;
import host.HostBase;
import host.LoadBalancer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.MalformedPatternException;



import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import constants.regex.Regex;


import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.EofMatch;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

public class Shell {

    private static Logger logger = Logger.getLogger(Shell.class);
    
    private Session session;
    private ChannelShell channel;
    private  Expect4j expect = null;
    private  long timeout = 5*1000;
    public  StringBuffer buffer= null;
    
    
    
    public static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
    public static final String BACKSLASH_R = "\r";
    public static final String BACKSLASH_N = "\n";
    public static final String COLON_CHAR = ":";
    public static final String ENTER_CHARACTER = BACKSLASH_R;
    public static final int SSH_PORT = 22;
    
    
    public static String[] errorMsg=new String[]{"could not acquire the config lock "};
    
  
    private HostBase host;
    /**
     * 模拟终端
     * @param host TODO
     * @throws ShellException  创建Shell失败，可能的原因1.网络失败  2.用户名或者密码错误	3输入输出流获取失败	4Expect创建失败
     */
    public Shell(HostBase host) throws ShellException{
        this.host = host;
        login();
    }
    
    
    public void setTimeout(long timeout) {
		this.timeout = timeout;
	}


	public void disconnect(){
        if(channel!=null){
            channel.disconnect();
        }
        if(session!=null){
            session.disconnect();
        }
        if(expect != null){
        	expect.close();
        }
        
    }
    

    public String getResponse(){
        return buffer.toString();
    }
    /**
     * 
     * @throws Exception   连接主机失败
     */
    private void login() throws ShellException{
    	if(host == null) {
    		logger.error("host为null,无法登录");
    		throw new ShellException("host为null,无法登录");
    	}
    	final   String ip = host.getIp();
    	final   int port = host.getSshPort();
    	final   String user = host.getJkUser();
    	final   String password = host.getJkUserPassword();
        try {
            logger.debug(String.format("Start logging to %s@%s:%s",user,ip,port));
            JSch jsch = new JSch();
            session = jsch.getSession(user, ip, port);
            session.setPassword(password);
            Hashtable<String, String> config = new Hashtable<String, String>();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", 
                    "publickey,keyboard-interactive,password");
            session.setConfig(config);
            session.connect();
            channel = (ChannelShell) session.openChannel("shell");
            this.expect = new Expect4j(channel.getInputStream(), channel
                    .getOutputStream());
            
            channel.connect();
            
            checkResult(expect.expect(getPatternList()));
            logger.debug(String.format("Logging to %s@%s:%s successfully!",user,ip,port));
        } catch (JSchException ex) {
        	logger.error("连接到 "+ip+":"+port+"失败,请检查网络、用户名和密码!");
        	
            //ex.printStackTrace();
            throw new ShellException(ex.toString()+"	连接到 "+ip+":"+port+"失败,请检查网络、用户名和密码!",ex);
        } catch (IOException ex) {
			// TODO Auto-generated catch block
        	logger.error(ex.getMessage()+"	成功连接到 "+ip+":"+port+",但是获取JSCH channel输入/输出流失败!");
			 
			throw new ShellException(ex.toString()+"	成功连接到 "+ip+":"+port+",但是获取JSCH channel输入/输出流失败!",ex);
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			throw new ShellException(ex.toString()+"	Expect失败，错误未知",ex);
		}
    }


    public boolean executeCommands(String[] commands) {
    	
    	//主机连接应为用户名  密码不正确  或者  网络不通  而连接主机失败的情况下 
        if(expect==null){
            return false;
        }
        for(String command:commands){
        	logger.debug(command);
        }
        List<Match> patternList = getPatternList();
        try {
            boolean isSuccess = true;
            for (String strCmd : commands){
                isSuccess = isSuccess(patternList, strCmd);
            }
            
            String response=buffer.toString().toLowerCase();
            for(String msg:errorMsg){
                if(response.indexOf(msg)>-1){
                    return false;
                }
            }
            
            return isSuccess;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    
	private List<Match>  getPatternList() {
		buffer  = new StringBuffer();
        //没有特别的正则   则使用匹配命令提示的标准正则
    	if(commandLinePromptRegex == null){
    		commandLinePromptRegex = COMMAND_LINE_PROMPT_REGEX_TEMPLATE;
    	}
        logger.debug("----------Running commands are listed as follows:----------");
        
        logger.debug("----------End----------");
        
        List<Match> patternList = new ArrayList();
        Closure closure = new Closure() {
            public void run(ExpectState state) throws Exception {
            	//匹配到模式串的情况下，例如：#等等待输入命令的提示符时，输出执行的命令和返回的信息
            
                buffer.append(state.getBuffer()); 
                state.exp_continue();
            }
        };
		String[] regEx = commandLinePromptRegex;
        //去掉特殊正则，默认回归标准的提示符匹配的正则
        commandLinePromptRegex = null;
        if (regEx != null && regEx.length > 0) {
            synchronized (regEx) {
                for (String regexElement : regEx) {// list of regx like, :>, />
                                                    // etc. it is possible
                                                    // command prompts of your
                                                    // remote machine
                    try {
                    	RegExpMatch mat  = null;
                    	//匹配到   --More--  代表执行结果无法在一页输出，需要键入  空格  翻页输出
                        if("--More--".equals(regexElement)){
                        	 mat = new RegExpMatch(regexElement, new Closure(){

								@Override
								public void run(ExpectState state) throws Exception {
									// TODO Auto-generated method stub
									buffer.append(state.getBuffer());
									expect.send(" ");   //翻页命令
						            logger.info(state.getMatch());
						            state.exp_continue();
								}
                        	 });
                        }else{
                        	 mat = new RegExpMatch(regexElement, closure);
                        }
                        
                        patternList.add(mat);
                    } catch (MalformedPatternException e) {
                    	e.printStackTrace();
                    } catch (Exception e) {
                       e.printStackTrace();
                    }
                }
               
                patternList.add(new TimeoutMatch(timeout, new Closure() {

					@Override
					public void run(ExpectState state) throws Exception {
						// TODO Auto-generated method stub
						  
                    	logger.info("---expect 等待输出超时---");
		                     
					}
                  
                }));
                //
                patternList.add(new EofMatch(new Closure(){

					@Override
					public void run(ExpectState state) throws Exception {
						// TODO Auto-generated method stub
						logger.info("---expect 匹配到文件结束---");
					}
                	
                }));
            }
        }
		return patternList;
	}

    private boolean isSuccess(List<Match> patternList, String command) {
        try {
            expect.send(command);
            expect.send("\n");
            return checkResult(expect.expect(patternList));
        } catch (MalformedPatternException ex) {
        	ex.printStackTrace();
            return false;
        } catch (Exception ex) {
        	ex.printStackTrace();
            return false;
        }
    }
    int count = 1;
    private boolean checkResult(int intRetVal) {
    	logger.info("第"+(count++)+"次尝试匹配	expect返回值="+intRetVal);
    	logger.info("匹配到的结果="+buffer.toString());
        if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
            return true;
        }
        return false;
    }
    
    
    public void setCommandLinePromptRegex(String[] commandLinePromptRegex){
    	this.commandLinePromptRegex = commandLinePromptRegex;
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
    public final static String[] COMMAND_LINE_PROMPT_REGEX_TEMPLATE = new String[] { "~]#", "~#", "#",
        ":~#", "/$", ">","SQL>","\\$"};
    private String[] commandLinePromptRegex = null;
      
    /**
     * 使用pattern从cmdResult获取必要的信息，若提取不到返回NONE
     * @param pattern     提取必要信息的模式
     * @param cmdResult   shell命名执行后返回的结果
     * @return
     */
    public static String parseInfoByRegex(final String pattern,final String cmdResult,final int groupNum){
    	//获取操作系统的类型
		Matcher m = null;
		try{
			m = Pattern.compile(pattern).matcher(cmdResult);
		}catch(PatternSyntaxException e){
			logger.error(e);
			e.printStackTrace();
			return "NONE";//正则表达式不合法的情况下，解析结果就是NONE
		}
		if(m.find()){
			return m.group(groupNum);
	
		}else{
			return "NONE";
		}
    	
    }
    /**
     * 使用enum pattern从cmdResult获取必要的信息，若提取不到返回NONE  委托于pattern为String类型的方法
     * @param pattern     提取必要信息的模式
     * @param cmdResult   shell命名执行后返回的结果
     * @return
     */
    public static String parseInfoByRegex(final Regex pattern,final String cmdResult,final int groupNum){
    	//获取操作系统的类型
		
    	return parseInfoByRegex(pattern+"",cmdResult,groupNum);
    }
}
