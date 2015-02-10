package ssh;

import host.HostBase;

import java.io.IOException;
import java.util.Hashtable;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import expect4j.Expect4j;

public class SSH {
	private Session session;
	private  ChannelShell channel;
	
	public Session getSession() {
		return session;
	}
	public ChannelShell getChannel() {
		return channel;
	}
	private HostBase host;
	
	
    public SSH() {
		super();
	}
	/**
     *
     * @param host TODO
     */
    public SSH(final HostBase host) {
        this.host = host;
        
    }
	/**
	 *
	 * @param hostname
	 * @param username
	 * @param password
	 * @param port
	 * @return
	 * @param sshClient TODO
	 * @throws ShellException 		shell创建和执行命令失败抛出异常   
	 */
	public Expect4j getExpect(SSHClient sshClient) throws ShellException {
	    JSch jsch = new JSch();
	    
	    try {
			session = jsch.getSession( host.getJkUser(),  host.getIp(),  host.getSshPort());
			
	        if ( host.getJkUserPassword() != null) {
	            session.setPassword( host.getJkUserPassword());
	        }
	        Hashtable<String,String> config = new Hashtable<String,String>();
	        config.put("StrictHostKeyChecking", "no");
	        //据stackoverflow.com上的解答，加入下面一条配置项
	        config.put("PreferredAuthentications", 
	                "publickey,keyboard-interactive,password");
	        session.setConfig(config);
	        session.connect(60000);
	        channel = (ChannelShell) session.openChannel("shell");
	        sshClient.expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
	        sshClient.expect.setDefaultTimeout(SSHClient.DEFAULT_TIMEOUT);
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
	    return sshClient.expect;
	}
	public void close() {
		if(getChannel()!=null){
			getChannel().disconnect();
	    }
	    if(getSession() !=null){
	    	getSession().disconnect();
	    }
	}

}