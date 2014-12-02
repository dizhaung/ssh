package export;

import host.Host;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.sun.xml.internal.ws.util.StringUtils;

public class POIReadAndWrite {
	
	
	/**
	 * 将主机基本信息和详细信息写入excel文件
	 * @param path
	 * @param fileName
	 * @param fileType
	 * @throws IOException
	 */
    public static void writer(String path, String fileName,String fileType) throws IOException {  
    	System.out.println(path);
        //创建工作文档对象  
        Workbook wb = null;  
        if (fileType.equals("xls")) {  
            wb = new HSSFWorkbook();  
        }  
        else if(fileType.equals("xlsx"))  
        {  
            wb = new XSSFWorkbook();  
        }  
        else  
        {  
            System.out.println("您的文档格式不正确！");  
        }  
        //创建sheet对象  
        Sheet sheet1 = (Sheet) wb.createSheet("服务器总表");
        //创建主机列表     添加测试主机
        
        List<Host> hostList = new  ArrayList();
       
        Host host = new Host();
        hostList.add(host);
        host.setBuss("测试服务器2数据库");
        host.setIp("10.204.16.150");
        host.setHostType("数据库服务器   应用服务器");
        host.setOs("AIX");
       
        Host.HostDetail hostDetail = new Host.HostDetail();
        host.setDetail(hostDetail);
        hostDetail.setOs("AIX");
        hostDetail.setHostName("IBM测试机");
        hostDetail.setHostType("IBM");
        hostDetail.setOsVersion("6.1");
       /* host = new Host();
        host.setBuss("测试服务器1 应用服务器");
        host.setIp("10.204.16.151");
        host.setHostType("应用服务器");
        host.setOs("AIX");
        hostList.add(host);*/
        
        //循环写入行数据  
        int i = 0;
        for (Iterator<Host> it = hostList.iterator(); it.hasNext();) { 
        	host = it.next();
            Row row = (Row) sheet1.createRow(i++);  
            //循环写入列数据  
            Cell cell = row.createCell(0);  
            cell.setCellValue(i);
            cell = row.createCell(1);  
            cell.setCellValue(host.getBuss());  
            cell = row.createCell(2);
            cell.setCellValue(host.getIp());
            cell = row.createCell(3);
            cell.setCellValue(host.getOs());
            cell = row.createCell(4);
            cell.setCellValue(host.getHostType());
            
        } 
        //将主机详细信息写入文件
        
        for (Iterator<Host> it = hostList.iterator(); it.hasNext();) { 
        	host = it.next();
            Sheet bussSheet = wb.createSheet(host.getBuss()+host.getIp());
            writeHostToSheet(host,bussSheet);
            
        } 
        //创建文件流  
        OutputStream stream = new FileOutputStream(path+fileName+"."+fileType);  
        //写入数据  
        wb.write(stream);  
        //关闭文件流  
        stream.close();  
    }
    /**
    * 将主机的详细信息写入Excel文件的工作表
    * @param host
    * @param sheet
    */
    private static void writeHostToSheet(final Host host,Sheet sheet){
    	Row row = (Row) sheet.createRow(0);  
    	//和页面的看到的格式基本一致
        //打印页面标题
        Cell cell = row.createCell(0);  
        cell.setCellValue(host.getBuss()+"的基本信息	IP:"+host.getIp());
        
        //打印服务器标题
        row = (Row) sheet.createRow(1);
         cell = row.createCell(0);  
        cell.setCellValue("服务器的基本信息");
        
        //打印服务器的基本信息
        printServerBaseInfo(host,sheet);
    }
    /**
     * 打印服务器的基本信息     
     * @param host
     * @param sheet 服务器详细信息Excel    工作表
     */
    public static void printServerBaseInfo(final Host host,Sheet sheet){
    	Host.HostDetail detail = host.getDetail();
    	List<String> itemList = new LinkedList();
    	 
    	itemList.add("主机类型");
    	itemList.add( detail.getHostType() );
    	itemList.add( "操作系统" );
    	itemList.add( detail.getOs() );
    	itemList.add( "IP地址" );
    	itemList.add( detail.getOsVersion() );
    	printServerBaseInfoRowToSheet(3,itemList,sheet);
    	itemList = new LinkedList();
    	itemList.add( "主机名" );
    	itemList.add( detail.getHostName() );
    	itemList.add( "主机操作系统版本" );
    	itemList.add( detail.getOsVersion() );
    	printServerBaseInfoRowToSheet(4,itemList,sheet);
    	//按列表顺序打印到行
    	itemList = new LinkedList();
    	itemList.add("是否双机");
    	itemList.add(detail.getIsCluster());
    	itemList.add("双机虚地址");
    	itemList.add(detail.getClusterServiceIP());
    	
    	printServerBaseInfoRowToSheet(5,itemList,sheet);
    	
    	//按列表顺序打印到行
    	itemList = new LinkedList();
    	itemList.add("是否负载均衡");
    	itemList.add(detail.getIsLoadBalanced());
    	itemList.add("负载均衡虚地址");
    	itemList.add(detail.getLoadBalancedVirtualIP());
    	printServerBaseInfoRowToSheet(6,itemList,sheet);
    	
    	
    	itemList = new LinkedList();
    	itemList.add("内存大小");
    	itemList.add(detail.getMemSize());
    	itemList.add("CPU个数");
    	itemList.add(detail.getCPUNumber());
    	 
    	itemList.add("CPU主频");
    	itemList.add(detail.getCPUClockSpeed());
    	itemList.add("CPU核数");
    	itemList.add(detail.getLogicalCPUNumber());
    	printServerBaseInfoRowToSheet(7,itemList,sheet);
    	
      
    	//打印网口类型表格
    	///打印表格头  
    	int endRowNum = printTableToSheet(9,detail.reverseCardListToTable(),sheet);
    	
    	//打印磁盘类型表格
    	
    	endRowNum = printTableToSheet(endRowNum,detail.reverseFsListToTable(),sheet);
    }
    /**
     * 打印表格类型的数据到工作表  例如：网口表     磁盘表
     * @param startRowNum   表格起始的行号
     * @param table   
     * @param sheet
     * @return 表格结束行号
     */
    public static int printTableToSheet(final int startRowNum,final List<List<String>> table,final Sheet sheet){
    	int i = 0;
    	for(List<String> tr:table){
    		printServerBaseInfoRowToSheet(startRowNum+i++,tr,sheet);
    	}
    	return startRowNum+i;
    }
    /**
     * 
     * @param rowNum    例如： 1代表第一行
     * @param itemList
     * @param sheet
     */
    public static void printServerBaseInfoRowToSheet(final int rowNum,final List<String> itemList,final Sheet sheet){
    	//按列表顺序打印到行
    	Row row = (Row) sheet.createRow(rowNum-1);	
    	 for(int i = 0,length = itemList.size();i<length;i++){
    		printServerBaseInfoItemToRow(i+1,itemList.get(i),row);
    		
    	}
    }
    /**
     * 打印服务器基本信息项目       每一项一个小指标 例如：主机IP：10.10.10.10
     * @param cellNum  单元格位置   1代表第一个单元格
     * @param cellValue
     * @param row
     */
    public static void printServerBaseInfoItemToRow(final int cellNum,final String cellValue,final Row row){
    	
        Cell cell = row.createCell(cellNum-1);  
        cell.setCellValue(cellValue);
    }
    public static void read(String path,String fileName,String fileType) throws IOException  
    {  
        InputStream stream = new FileInputStream(path+fileName+"."+fileType);  
        Workbook wb = null;  
        if (fileType.equals("xls")) {  
            wb = new HSSFWorkbook(stream);  
        }  
        else if (fileType.equals("xlsx")) {  
            wb = new XSSFWorkbook(stream);  
        }  
        else {  
            System.out.println("您输入的excel格式不正确");  
        }  
        Sheet sheet1 = wb.getSheetAt(0);
       
        for (Row row : sheet1) {  
            for (Cell cell : row) {  
                System.out.print(cell.getStringCellValue()+"  ");  
            }  
            System.out.println();  
        }  
    }  
    public static void main(String[] args) throws IOException {  
        String path = "g:/";  
        String fileName = "11";  
        String fileType = "xlsx";  
        writer(path, fileName, fileType);  
       // read(path, fileName, fileType);  
        try {
			//(new POIReadAndWrite()).read(path,fileName,fileType);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }   
 /**
  * 取得文件格式。
  * <p>
  * 该方法取得文件名最后一个"."所在的位置，然后返回该位置后的字符串。
  * </p>
  * 
  * @param fileName 文件名
  * @return String 文件格式
  */
 private String getFileFormat(String fileName) {
     int dotIndex = fileName.lastIndexOf('.');
     return dotIndex == -1 ? null : fileName.substring(dotIndex + 1);
 }
}
 