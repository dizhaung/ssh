package host;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileManager {


	public static List<Host> getHostList(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        List<Host> hostList =  new ArrayList<Host>();
        try {
            System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                
                System.out.println(tempString);
                if(!tempString.matches("^#.*")){  //注释掉的主机过滤掉不链接
                	String[] strs = tempString.split("\\|");
                    
                	Host host = new Host();
                	host.setBuss(strs[0]);
                	host.setIp(strs[1]);
                	host.setJkUser(strs[2]);
                	host.setJkUserPassword(strs[3]);
                	host.setRootUser(strs[4]);
                	host.setRootUserPassword(strs[5]);
                
                	hostList.add(host);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
		return hostList;
    }
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		System.out.println(System.getProperty("user.dir"));
		String userDir = System.getProperty("user.dir");
		List<Host> list = FileManager.getHostList(userDir+"/WebRoot/WEB-INF/classes/config.txt");
		
		for(Host h:list){
			System.out.println(h);
		}
	}

}
