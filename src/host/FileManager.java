package host;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import ssh.Shell;

public class FileManager {
	
	
	/**
	 * 读取文件信息并返回
	 * @author HP
	 * @param fileName 
	 * @return
	 */
	public static String readFile(final String fileName){
		 
			File file = null;
			try {
				file = new File(FileManager.class.getResource(fileName).toURI());
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        BufferedReader reader = null;
	       System.out.println("以行为单位读取文件内容，一次读一整行：");
	       StringBuilder sb = new StringBuilder();
	            try {
					reader = new BufferedReader(new FileReader(file));
					String tempString = null;
		            
		            // 一次读入一行，直到读入null为文件结束
		            while ((tempString = reader.readLine()) != null) {
		            	sb.append(tempString)
		            	.append("\n");
		            }
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
		return sb.toString();
	}
	 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//读取主机配置信息文件测试
		/*
		System.out.println(System.getProperty("user.dir"));
		String userDir = System.getProperty("user.dir");
		List<Host> list = FileManager.getHostList(userDir+"/WebRoot/WEB-INF/classes/config.txt");
		
		for(Host h:list){
			System.out.println(h);
		}*/
		
		//
		/*System.out.println(Thread.currentThread().getContextClassLoader().getResource(""));
		System.out.println(FileManager.class.getClassLoader().getResource(""));
		System.out.println(ClassLoader.getSystemResource(""));
		System.out.println(FileManager.class.getResource(""));
		System.out.println(FileManager.class.getResource("/"));
		
		System.out.println(new File("").getAbsolutePath());
		System.out.println(System.getProperty("user.dir"));
		String content = readFile("/srv.txt");
		System.out.println(content);
		//构造IP正则表达式     例如：\s10.204.\s{2}7.153\s
		String[] ipFragments = "10.204.7.153".split("\\.");
		for(int i = 0,size=ipFragments.length;i<size;i++){
			switch(ipFragments[i].length()){
			case 1:ipFragments[i] = "\\s{2}"+ipFragments[i];break;
			case 2:ipFragments[i] = "\\s"+ipFragments[i];break;
			case 3:ipFragments[i] = ipFragments[i];break;
			}
		}
		StringBuilder ipRegex = new StringBuilder();
		ipRegex.append("\\s(")
		.append(ipFragments[0])
		.append(".")
		.append(ipFragments[1])
		.append(".")
		.append(ipFragments[2])
		.append(".")
		.append(ipFragments[3])
		.append(")\\s");
		
		System.out.println(ipRegex);
		System.out.println(Shell.parseInfoByRegex(ipRegex.toString(),content));
		
		//找出Farm  \s+([^\s]+)\s+
		System.out.println(Shell.parseInfoByRegex("\\s+([^\\s]+)\\s+"+ipRegex.toString(),content));
		*/
		//重构 测试
		System.out.println(LoadBalancer.getLoadBalancerList(readFile("/loadBalancerConfig.txt")));
	
	}

}
