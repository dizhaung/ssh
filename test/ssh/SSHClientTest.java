package ssh;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import host.FileManager;
import host.Host;
import host.middleware.tomcat.Service;
import host.middleware.tomcat.Service.Engine;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;

public class SSHClientTest {
	private static Host  host = new Host();
	@Before
	public void setUp() throws Exception {
	}

//	@Test
	public void testStartBat() {
		//System.out.println( Matcher.quoteReplacement("ls|oo $"));
	
		//System.out.println("/$".replaceAll(Pattern.quote("$"), ""));
	}

//	@Test
	public void testBatHosts() {
		List<Host> list = Host.getHostList(FileManager.readFile("/hostConfig.txt"));
		for(Iterator<Host> it = list.iterator();it.hasNext();){
			Host h = it.next();
			Shell shell = null;
			try {
				shell = new Shell(h.getIp(), 22,h.getJkUser(), h.getJkUserPassword());
			} catch (ShellException e) {
				// TODO Auto-generated catch block
				
				e.printStackTrace();
				continue;
			}
			shell.setTimeout(2*1000);
			SSHClient.collectDB2ForAix(shell, h);
		}
		
		System.out.println(list);
	}
	
//	@Test
	public void testStringMatches(){
		/*if("ps -ef|grep db2sysc".matches("grep db2sysc")){
			System.out.println("匹配");
		}*/
		System.out.println(System.getProperty("user.dir"));
	}
	
//	@Test
	 public void collectWebSphere(){
		List<Host> list = Host.getHostList(FileManager.readFile("/hostConfig.txt"));
		for(Iterator<Host> it = list.iterator();it.hasNext();){
			Host h = it.next();
			Shell shell = null;
			try {
				shell = new Shell(h.getIp(), 22,h.getJkUser(), h.getJkUserPassword());
			} catch (ShellException e) {
				// TODO Auto-generated catch block
				
				e.printStackTrace();
				continue;
			}
			shell.setTimeout(2*1000);
			SSHClient.collectWebSphereForAIX(shell, h,new ArrayList());
		}
		
		System.out.println(list);
	}
//	 @Test
	 public void testCollectOracleForLinux(){
		 List<Host> list = Host.getHostList(FileManager.readFile("/hostConfig.txt"));
			for(Iterator<Host> it = list.iterator();it.hasNext();){
				Host h = it.next();
				Shell shell = null;
				try {
					shell = new Shell(h.getIp(), 22,h.getJkUser(), h.getJkUserPassword());
				} catch (ShellException e) {
					// TODO Auto-generated catch block
					
					e.printStackTrace();
					continue;
				}
				shell.setTimeout(2*1000);
				SSHClient.collectOracleForLinux(shell,h);
			}
		 
	 }
//	@Test
	 public void match(){
		 System.out.println(SSHClient.connectDatabaseSuccess(" database Connection Information Database server        = DB2/AIX64 9.7.9 SQL authorization ID   = DB2INSTLocal database alias   = TBMPRD "));
	 }
	 
	 
	 @Test
	 public void testCollectTomcatForLinux(){
		 List<Host> list = Host.getHostList(FileManager.readFile("/hostConfig.txt"));
			for(Iterator<Host> it = list.iterator();it.hasNext();){
				Host h = it.next();
				Shell shell = null;
				try {
					shell = new Shell(h.getIp(), 22,h.getJkUser(), h.getJkUserPassword());
				} catch (ShellException e) {
					// TODO Auto-generated catch block
					
					e.printStackTrace();
					continue;
				}
				shell.setTimeout(2*1000);
				SSHClient.collectTomcatForLinux(shell,h,new ArrayList());
			}
	 }
	 private static Log logger = LogFactory.getLog(SSHClientTest.class);
//	 @Test
	 public void testServerDocXml(){
		 
		 	//String serverDotXml = parseServerDotXmlFrom(cmdResult);
		 	File file = new File("server.xml");
		 	Reader fileReader = null;
			try {
				fileReader = new BufferedReader(new FileReader(file));
				SAXReader reader = new SAXReader();
				
				//Document serverXmlDoc = DocumentHelper.parseText(serverDotXml);
				Document serverXmlDoc  = reader.read(fileReader);
				/**
				 * server.xml文件中，HTTP访问端口  与 service中protocal为HTTP的connector连接器相对应
				 * Server是根   下面的结构是Service->Engine->Connector---Host   Host->Context
				 *  目的是 解析出虚拟主机（应用在虚拟主机下面）和HTTP连接器的对应关系
				 *  考虑到下面两种应用部署的情况：
				 *  1$CATALINA_BASE/conf/[enginename]/[hostname]/context.xml
				 *	$CATALINA_BASE/webapps/[webappname]/META-INF/context.xml
				 *	需要解析出Engine和Host对应关系
				 *
				 */
				
				
				Element serverNode = serverXmlDoc.getRootElement();
				logger.info(serverNode.getText());
				List<Element> serviceNodeList = serverNode.elements("Service");
				Set<Service> serviceSet = new HashSet();
				for(Element serviceNode:serviceNodeList){
					Service service = new Service();
					serviceSet.add(service);
					service.setName(serviceNode.attributeValue("name"));
					
					List<Element> engineNodeList = serviceNode.elements("Engine");
					for(Element engineNode:engineNodeList){
						Service.Engine engine = service.new Engine(); 
						engine.setDefaultHost(engineNode.attributeValue("defaultHost"));
						engine.setName(engineNode.attributeValue("name"));
						service.addEngine(engine);
						
						List<Element> hostNodeList = engineNode.elements("Host");
						for(Element hostNode:hostNodeList){
							Service.Engine.Host host = engine.new Host();
							engine.addHost(host);
							host.setName(hostNode.attributeValue("name"));
							host.setAppBase(hostNode.attributeValue("appBase"));
							
							List<Element> contextNodeList = hostNode.elements("Context");
							for(Element contextNode:contextNodeList){
								Service.Engine.Host.Context context = host.new Context();
								host.addContext(context);
								context.setPath(hostNode.attributeValue("path"));
								context.setDocBase(hostNode.attributeValue("docBase"));
							}
							
						}
						
					}
					
					List<Element> connectorNodeList = serviceNode.elements("Connector");
					for(Element connectorNode:connectorNodeList){
						logger.info(connectorNode);
						String connectorProtocolAttr = connectorNode.attributeValue("protocol");
						if(Pattern.compile("HTTP",Pattern.CASE_INSENSITIVE).matcher(connectorProtocolAttr).find()){
							String connectorSslEnabledAttr = connectorNode.attributeValue("SSLEnabled");
							if(connectorSslEnabledAttr == null){
								Service.Connector httpConnector = service.new HttpConnector();
								service.addConnector(httpConnector);
								httpConnector.setPort(connectorNode.attributeValue("port"));
								httpConnector.setProtocol(connectorProtocolAttr);
							}
						}
					}
					
					
				}
				
				logger.info(serviceSet);
				/**
				 * 1Context会在appBase下
				 * 2在$CATALINA_BASE/conf/[enginename]/[hostname]/appName.xml中
				 * 3在appName/META-INF/context.xml  中
				 * 4在server.xml Host元素中
				 */
				
				/**
				 * 1server.xml Host元素中的Context已经被解析出来
				 * 2查找$CATALINA_BASE/APPBase下的文件夹，作为appName和$CATALINA_BASE/APPBase/appName部署路径
				 * 3APPBASE路径下，appName/META-INF/context.xml文件会被复制到$CATALINA_BASE/conf/[enginename]/[hostname]/下
				 * 并被命名为appName.xml文件
				 * 这样，通过$CATALINA_BASE/conf/[enginename]/[hostname]/下appName.xml解析，矫正appName/META-INF/context.xml的docBase
				 */
				String catalinaHome = "";
				for(Iterator<Service> serviceIt = serviceSet.iterator();serviceIt.hasNext(); ){
					Set<Engine> engineSet = serviceIt.next().getEngineSet();
					for(Iterator<Engine> engineIt = engineSet.iterator();engineIt.hasNext(); ){
						Set<host.middleware.tomcat.Service.Engine.Host> hostSet = engineIt.next().getHostSet();
						for(Iterator<host.middleware.tomcat.Service.Engine.Host> hostIt = hostSet.iterator();hostIt.hasNext();){
							String appDir = catalinaHome+"/"+hostIt.next().getAppBase();
						}
					}
				}
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				logger.error("串不是合法的xml");
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				if(fileReader != null){
					try {
						fileReader.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		
	 }

	
}
