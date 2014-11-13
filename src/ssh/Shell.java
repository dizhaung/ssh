package ssh;

import host.FileManager;
import host.Host;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.MalformedPatternException;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;


import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.EofMatch;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

public class Shell {

    private static Logger log = Logger.getLogger(Shell.class);
    
    private Session session;
    private ChannelShell channel;
    private  Expect4j expect = null;
    private static final long defaultTimeOut = 1000;
    public  StringBuffer buffer= null;
    
    public static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
    public static final String BACKSLASH_R = "\r";
    public static final String BACKSLASH_N = "\n";
    public static final String COLON_CHAR = ":";
    public static String ENTER_CHARACTER = BACKSLASH_R;
    public static final int SSH_PORT = 22;
    
    public static String[] linuxPromptRegEx = new String[] { "~]#", "~#", "#",
            ":~#", "/$", ">","\\$" };
    
    public static String[] errorMsg=new String[]{"could not acquire the config lock "};
    
    private String ip;
    private int port;
    private String user;
    private String password;
    
    public Shell(String ip,int port,String user,String password) {
        this.ip=ip;
        this.port=port;
        this.user=user;
        this.password=password;
        expect = getExpect();
    }
    
    public void disconnect(){
        if(channel!=null){
            channel.disconnect();
        }
        if(session!=null){
            session.disconnect();
        }
    }
    

    public String getResponse(){
        return buffer.toString();
    }
    
    private Expect4j getExpect() {
        try {
            log.debug(String.format("Start logging to %s@%s:%s",user,ip,port));
            JSch jsch = new JSch();
            session = jsch.getSession(user, ip, port);
            session.setPassword(password);
            Hashtable<String, String> config = new Hashtable<String, String>();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", 
                    "publickey,keyboard-interactive,password");
            session.setConfig(config);
            localUserInfo ui = new localUserInfo();
            session.setUserInfo(ui);
            session.connect();
            channel = (ChannelShell) session.openChannel("shell");
            Expect4j expect = new Expect4j(channel.getInputStream(), channel
                    .getOutputStream());
            channel.connect();
            log.debug(String.format("Logging to %s@%s:%s successfully!",user,ip,port));
            return expect;
        } catch (Exception ex) {
            log.error("Connect to "+ip+":"+port+"failed,please check your username and password!");
            ex.printStackTrace();
        }
        return null;
    }

    public boolean executeCommands(String[] commands) {
        if(expect==null){
            return false;
        }
        buffer = new StringBuffer();
        log.debug("----------Running commands are listed as follows:----------");
        for(String command:commands){
            log.debug(command);
        }
        log.debug("----------End----------");
        
        Closure closure = new Closure() {
            public void run(ExpectState expectState) throws Exception {
            	//匹配到模式串的情况下，例如：#等等待输入命令的提示符时，输出执行的命令和返回的信息
            	//System.out.println(expectState.getBuffer());
                buffer.append(expectState.getBuffer());// buffer is string
                                                        // buffer for appending
                                                        // output of executed
                                                        // command
              
                expectState.exp_continue();
                
            }
        };
        List<Match> lstPattern = new ArrayList<Match>();
        String[] regEx = linuxPromptRegEx;
        if (regEx != null && regEx.length > 0) {
            synchronized (regEx) {
                for (String regexElement : regEx) {// list of regx like, :>, />
                                                    // etc. it is possible
                                                    // command prompts of your
                                                    // remote machine
                    try {
                        RegExpMatch mat = new RegExpMatch(regexElement, closure);
                        lstPattern.add(mat);
                    } catch (MalformedPatternException e) {
                        return false;
                    } catch (Exception e) {
                        return false;
                    }
                }
                lstPattern.add(new EofMatch(new Closure() { // should cause
                                                            // entire page to be
                                                            // collected
                            public void run(ExpectState state) {
                            }
                        }));
                lstPattern.add(new TimeoutMatch(defaultTimeOut, new Closure() {
                    public void run(ExpectState state) {
                    }
                }));
            }
        }
        try {
            boolean isSuccess = true;
            for (String strCmd : commands){
                isSuccess = isSuccess(lstPattern, strCmd);
            }
            isSuccess = !checkResult(expect.expect(lstPattern));
            
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

    private boolean isSuccess(List<Match> objPattern, String strCommandPattern) {
        try {
            boolean isFailed = checkResult(expect.expect(objPattern));
            if (!isFailed) {
                expect.send(strCommandPattern);
                expect.send("\n");
                return true;
            }
            return false;
        } catch (MalformedPatternException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean checkResult(int intRetVal) {
        if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
            return true;
        }
        return false;
    }
    
    public static class localUserInfo implements UserInfo {
        String passwd;

        public String getPassword() {
            return passwd;
        }

        public boolean promptYesNo(String str) {
            return true;
        }

        public String getPassphrase() {
            return null;
        }

        public boolean promptPassphrase(String message) {
            return true;
        }

        public boolean promptPassword(String message) {
            return true;
        }

        public void showMessage(String message) {
            
        }
    }
   
    public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException{
    
    	String userDir = System.getProperty("user.dir");
		List<Host> list = FileManager.getHostList(userDir+"/WebRoot/WEB-INF/classes/config.txt");
		
		for (Host h : list) {
			System.out.println(h);

			// 设置参数
			String ip = h.getIp();
			String jkUser = h.getJkUser();
			String jkUserPassword = h.getJkUserPassword();
			
			//当需要特别权限的情况下使用root用户
			String rootUser = h.getRootUser();
			String rootUserPassword = h.getRootUserPassword();
			
			// 建立连接
			Shell shell = new Shell(ip, SSH_PORT, jkUser, jkUserPassword);

			// 切换到root用户
			/*
			 * String[] commands = new String[]{"su -"}; boolean b =
			 * shell.executeCommands(commands); if (b) { commands = new
			 * String[]{"root",}; b = shell.executeCommands(commands); } if (b)
			 * { b = shell.executeCommands(new String[]{"su - mysql"}); } if (b)
			 * { b = shell.executeCommands(new String[]{"mysql"}); } if (b) { b
			 * = shell.executeCommands(new String[]{"use test;"}); } if (b) { b
			 * = shell.executeCommands(new String[]{"exit;"}); } if (b) { b =
			 * shell.executeCommands(new String[]{"exit;"}); }
			 */
			// if (b) {
			// b =
			shell.executeCommands(new String[] { "uname" });
			// }

			String cmdResult = shell.getResponse();
			//获取操作系统的类型
			System.out.println(cmdResult);
			h.setOs(parseInfoByRegex("\\s*uname\r\n(.*)\r\n",cmdResult));
			if("AIX".equalsIgnoreCase(h.getOs())){
				/*
				 //获取主机型号
				shell.executeCommands(new String[] { "uname -M" });
				cmdResult = shell.getResponse();
				
				System.out.print(parseInfoByRegex("\\s*uname -M\r\n(.*)\r\n",cmdResult));
				
				
				//获取主机名
				shell.executeCommands(new String[] { "uname -n" });
				cmdResult = shell.getResponse();
				
				System.out.print(parseInfoByRegex("\\s*uname -n\r\n(.*)\r\n",cmdResult));
				
				//获取系统版本号
				
				shell.executeCommands(new String[] { "uname -v" });
				cmdResult = shell.getResponse();
				
				String version = parseInfoByRegex("\\s*uname -v\r\n(.*)\r\n",cmdResult);
				
				shell.executeCommands(new String[] { "uname -r" });
				cmdResult = shell.getResponse();
				
				System.out.print(version+"."+parseInfoByRegex("\\s*uname -r\r\n(.*)\r\n",cmdResult));
				*/
				//获取内存大小
				shell.executeCommands(new String[] { "prtconf |grep \"Good Memory Size:\"" });
				cmdResult = shell.getResponse();
				System.out.println(cmdResult);
				System.out.print("内存大小"+parseInfoByRegex("\\s*prtconf \\|grep \"Good Memory Size:\"\r\n(.*)\r\n",cmdResult));
				
				/*//获取CPU个数
				shell.executeCommands(new String[] { "prtconf |grep \"Number Of Processors:\"" });
				cmdResult = shell.getResponse();
				
				System.out.print("CPU个数"+parseInfoByRegex("\\s*prtconf |grep \"Number Of Processors:\"\r\n(.*)\r\n",cmdResult));
				//获取CPU频率
				shell.executeCommands(new String[] { "prtconf |grep \"Processor Clock Speed:\"" });
				cmdResult = shell.getResponse();
				
				System.out.print("CPU频率"+parseInfoByRegex("\\s*prtconf |grep \"Processor Clock Speed:\"\r\n(.*)\r\n",cmdResult));
				
				//获取CPU核数
				shell.executeCommands(new String[] { "bindprocessor -q" });
				cmdResult = shell.getResponse();
				
				System.out.print("CPU核数"+parseInfoByRegex("\\s*bindprocessor -q\r\n(.*)\r\n",cmdResult));
				
				//获取网卡信息
				shell.executeCommands(new String[] { "bindprocessor -q" });
				cmdResult = shell.getResponse();
				
				System.out.print(parseInfoByRegex("\\s*bindprocessor -q\r\n(.*)\r\n",cmdResult));
				*/
				
			}else if("LINUX".equalsIgnoreCase(h.getOs())){
				
			}else if("HP-UNIX".equalsIgnoreCase(h.getOs())){
				
			}
			shell.disconnect();

		}
    }
    /**
     * 使用pattern从cmdResult获取必要的信息，若提取不到返回NONE
     * @param pattern     提取必要信息的模式
     * @param cmdResult   shell命名执行后返回的结果
     * @return
     */
    public static String parseInfoByRegex(final String pattern,final String cmdResult){
    	//获取操作系统的类型
		Matcher m = Pattern.compile(pattern).matcher(cmdResult);
		if(m.find()){
			return m.group(1);
	
		}else{
			return "NONE";
		}
    	
    }
}
