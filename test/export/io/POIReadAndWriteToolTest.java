package export.io;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import host.Host;

import org.junit.Before;
import org.junit.Test;

public class POIReadAndWriteToolTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testWrite() {
		List<Host> hostList = new  ArrayList();
	       
        Host host = new Host();
        hostList.add(host);
        host.setBuss("测试服务器2数据库");
        host.setIp("10.204.16.150");
        
        host.setOs("AIX");
       
       /* Host.HostDetail hostDetail = new Host.HostDetail();
        host.setDetail(hostDetail);
        hostDetail.setOs("AIX");
        hostDetail.setHostName("IBM测试机");
        hostDetail.setHostType("IBM");
        hostDetail.setOsVersion("6.1");*/
 
        
        String path = "g:/";  
        String fileName = "11";  
        String fileType = "xlsx"; 
        POIReadAndWriteTool writer = POIReadAndWriteTool.getInstance();
        File file = new File(path, fileName, fileType);
       
       // read(path, fileName, fileType);  
        try {
        	 writer.write(hostList,Host.class,file);  
			//(new POIReadAndWrite()).read(path,fileName,fileType);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
