package ssh;

import host.FileManager;
import host.Host;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.oro.text.regex.MalformedPatternException;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
 
import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
 
public class SSHClient {
 
	private Session session;
	private ChannelShell channel;
	   
    private static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
    private static String ENTER_CHARACTER = "\r";
    private static final int SSH_PORT = 22;
    private List<String> lstCmds = new ArrayList<String>();
    public static String[] linuxPromptRegEx = new String[] { "~]#", "~#", "#",
        ":~#", "/$", ">","\\$"};
 
    private Expect4j expect = null;
    private StringBuilder buffer = null;
    private String userName;
    private String password;
    private String host;
 
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
     */
    public String execute(List<String> cmdsToExecute) {
        this.lstCmds = cmdsToExecute;
        buffer = new StringBuilder();
        Closure closure = new Closure() {
            public void run(ExpectState expectState) throws Exception {
                buffer.append(expectState.getBuffer());
            }
        };
        List<Match> lstPattern =  new ArrayList<Match>();
        for (String regexElement : linuxPromptRegEx) {
            try {
                Match mat = new RegExpMatch(regexElement, closure);
                lstPattern.add(mat);
            } catch (MalformedPatternException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
 
        try {
        	/**
        	 * 每执行一条命令都重新连接服务器，异常的低效，为了兼容复杂的shell命令
        	 * 例如：prtconf |grep "Good Memory Size:"
        	 */
        	
            expect = SSH();
            boolean isSuccess = true;
            for(String strCmd : lstCmds) {
                isSuccess = isSuccess(lstPattern,strCmd);
                if (!isSuccess) {
                    isSuccess = isSuccess(lstPattern,strCmd);
                }
            }
 
            checkResult(expect.expect(lstPattern));
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
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
    /**
     *
     * @param hostname
     * @param username
     * @param password
     * @param port
     * @return
     * @throws Exception
     */
    private Expect4j SSH() throws Exception {
        JSch jsch = new JSch();
         session = jsch.getSession(userName, host, SSH_PORT);
        if (password != null) {
            session.setPassword(password);
        }
        Hashtable<String,String> config = new Hashtable<String,String>();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(60000);
         channel = (ChannelShell) session.openChannel("shell");
        Expect4j expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
        channel.connect();
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
     *
     */
   
       
    /**
     *
     * @param args
     */
    public static void main(String[] args) {
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
			
			// 初始化服务器连接信息
			SSHClient ssh = new SSHClient(ip, jkUser, jkUserPassword);

			
			// 建立连接
			Shell shell = new Shell(ip, SSH_PORT, jkUser, jkUserPassword);
			shell.executeCommands(new String[] { "uname" });
			String cmdResult = shell.getResponse();
			//获取操作系统的类型
			System.out.println(cmdResult);
			h.setOs(parseInfoByRegex("\\s*uname\r\n(.*)\r\n",cmdResult));
			if("AIX".equalsIgnoreCase(h.getOs())){
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
				
				//获取内存大小
				List<String> cmdsToExecute = new ArrayList<String>();
				cmdsToExecute.add("prtconf |grep \"Good Memory Size:\"");
				
				cmdResult = ssh.execute(cmdsToExecute);;
			
				System.out.print("内存大小"+parseInfoByRegex("\\s*prtconf \\|grep \"Good Memory Size:\"\r\n(.*)\r\n",cmdResult));
				
				//获取CPU个数
				cmdsToExecute = new ArrayList<String>();
				cmdsToExecute.add("prtconf |grep \"Number Of Processors:\"");
				
				cmdResult =ssh.execute(cmdsToExecute);
				
				System.out.print("CPU个数"+parseInfoByRegex("\\s*prtconf \\|grep \"Number Of Processors:\"\r\n(.*)\r\n",cmdResult));
				//获取CPU频率
				
				
				cmdsToExecute = new ArrayList<String>();
				cmdsToExecute.add("prtconf |grep \"Processor Clock Speed:\"" );
				
				cmdResult =ssh.execute(cmdsToExecute);
				System.out.print("CPU频率"+parseInfoByRegex("\\s*prtconf \\|grep \"Processor Clock Speed:\"\r\n(.*)\r\n",cmdResult));
				
				//获取CPU核数
				cmdsToExecute = new ArrayList<String>();
				cmdsToExecute.add("bindprocessor -q" );
				
				cmdResult =ssh.execute(cmdsToExecute);
				
				
				System.out.print("CPU核数"+parseInfoByRegex("\\s*bindprocessor -q\r\n(.*)\r\n",cmdResult));
				
				//获取网卡信息
				
				
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