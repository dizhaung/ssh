package export.io;

import host.Host;
import host.Host.Database;
import host.Host.HostDetail;
import host.Host.Middleware;
import host.HostBase;
import host.TinyHost;

 
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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.sun.xml.internal.ws.util.StringUtils;

public class POIReadAndWriteTool {
	private static POIReadAndWriteTool instance = null;
	public static POIReadAndWriteTool getInstance(){
		 
		
	 
		return  new POIReadAndWriteTool();
	}
	Workbook wb = null;  
	public void init(){
		
	}
	 
	/**
	 * 将主机基本信息和详细信息写入excel文件
	 * @param path
	 * @param fileName
	 * @param fileType
	 * @throws IOException
	 */
    public  void write(final List<? extends HostBase> hostList,Class<?> clazz,File file) throws IOException {  
    	 
        //创建工作文档对象  
       
        if ("xls".equals(file.getFileType())) {  
            wb = new HSSFWorkbook();  
        }  
        else if("xlsx".equals(file.getFileType()))  
        {  
            wb = new XSSFWorkbook();  
        }  
        else  
        {  
            System.out.println("您的文档格式不正确！");  
        }  
        //创建sheet对象  
        Sheet sheet1 = (Sheet) wb.createSheet("服务器总表");
       
        if( clazz == Host.class){
        	 writeHostListToSheet( (List<Host>)hostList,  sheet1);
             //将主机详细信息写入文件
             writeHostListToWorkbook( (List<Host>)hostList, wb );
        }
        if(clazz == TinyHost.class){
        	writeTinyHostListToSheet( (List<TinyHost>)hostList,  sheet1);
        }
       
       
        //创建文件流  
        OutputStream stream = new FileOutputStream(file.getFile());  
        //写入数据  
        wb.write(stream);  
        //关闭文件流  
        stream.close();
        
    }
    /** 
     * 设置字体 
     * @param wb 
     * @return 
     */  
    public Font createFonts(){  
        //创建Font对象  
        Font font = wb.createFont();  
        //设置字体  
        font.setFontName("黑体");  
        //着色  
       // font.setColor(HSSFColor.BLUE.index);  
        //斜体  
        //font.setItalic(true);  
        //字体大小  
        font.setFontHeight((short)300);  
        return font;  
    }  
    /**
     * 每一个主机的信息在工作簿中占一个工作表
     * @author HP
     * @param hostList
     * @param wb
     */
    public  void writeHostListToWorkbook(final List<Host> hostList,final Workbook wb ){
    	 for (Iterator<Host> it = hostList.iterator(); it.hasNext();) { 
         	Host host = it.next();
             Sheet bussSheet = wb.createSheet(host.getBuss()+host.getIp());
             writeHostToSheet(host,bussSheet);
             
         } 
    }
    public  void writeHostListToSheet(final List<Host> hostList,final Sheet sheet){
    	 //循环写入行数据  
        int i = 1;
        List<List<String>> table = new LinkedList();
        List<String> tr = new LinkedList();
        tr.add("序 号");tr.add("业务名称");tr.add("主机名");tr.add("服务器类型");tr.add("IP地址");tr.add("操作系统");tr.add("是否负载均衡");tr.add("是否双机");tr.add("应用系统个数");tr.add("数据库");
        table.add(tr);
        for (Iterator<Host> it = hostList.iterator(); it.hasNext();) { 
        	Host host = it.next();
          
            tr = new LinkedList();
            HostDetail detail = host.getDetail();
            tr.add(""+i++);
            tr.add(host.getBuss());
            tr.add(detail == null?"未知":detail.getHostName());
            tr.add(detail == null?"未知":detail.getHostType());
            tr.add(host.getIp());
            tr.add(detail == null?"未知":detail.getOs());
            tr.add(detail == null?"未知":detail.getIsLoadBalanced());
            tr.add(detail == null?"未知":detail.getIsCluster());
            tr.add(appCountOf(host.getmList())+"");
            tr.add(allDbTypeAndVersionOf(host.getdList()));
            table.add(tr);
        }
        printTableToSheet(table, sheet);
    }
    public  void writeTinyHostListToSheet(final List<TinyHost> hostList,final Sheet sheet){
    	 //循环写入行数据  
        int i = 1;
        List<List<String>> table = new LinkedList();
        List<String> tr = new LinkedList();
        tr.add("序 号");tr.add("业务名称");tr.add("主机名");tr.add("服务器类型");tr.add("IP地址");tr.add("返回结果");
        table.add(tr);
        for (Iterator<TinyHost> it = hostList.iterator(); it.hasNext();) { 
        	TinyHost host = it.next();
          
            tr = new LinkedList();
           
            tr.add(""+i++);
            tr.add(host.getBuss());
            tr.add(host.getHostName());
            tr.add(host.getHostType());
            tr.add(host.getIp());
            tr.add(host.getCommandResult());
             
            table.add(tr);
        }
        printTableToSheet(table, sheet);
    }
    private int appCountOf(List<Middleware> mList){
    	int count = 0;
    	for(Middleware mw:mList){
    		count += mw.getAppList().size();
    	}
    	return count;
    }
    private String allDbTypeAndVersionOf(List<Database> dbList){
    	
    	StringBuffer typeAndVersion = new StringBuffer("");
    	
    	for(int i = 0,size = dbList.size();i < size;i++){
    		Database db = dbList.get(i);
    		typeAndVersion.append(db.getType()).append(",").append(db.getVersion());
    		if((i+1)<size)	typeAndVersion.append(",");
    	}
    	return typeAndVersion.toString();
    }
    /**
    * 将主机的详细信息写入Excel文件的工作表
    * @param host
    * @param sheet
    */
    private  void writeHostToSheet(final Host host,Sheet sheet){
    	resetRowNum();  
    	//和页面的看到的格式基本一致
        //打印页面标题
        
        printTitleToSheet(host.getBuss()+"的基本信息	IP:"+host.getIp(),sheet);
        //打印服务器标题
       
        printTitleToSheet("服务器的基本信息",sheet);
        //打印服务器的基本信息
        printServerBaseInfo(host,sheet);
        
        //打印数据库信息
        ///打印数据库标题
         
        printTitleToSheet("数据库的基本信息",sheet);
        ///打印各种数据库
        printDatabasesInfo(host,sheet);
        //打印中间件信息
        printTitleToSheet("中间件的基本信息",sheet);
        printMiddlewaresInfo( host, sheet);
    }
    /**
     * 打印数据库信息
     * @author HP
     * @param host
     * @param sheet
     */
    private  void printMiddlewaresInfo(final Host host,final Sheet sheet){
    	List<List<List<String>>> tables = host.reverseMiddlewareList();
    	for(List<List<String>> table:tables){
    		printTableToSheet(table,sheet);
    	}
    }
    /**
     * 打印数据库信息
     * @author HP
     * @param host
     * @param sheet
     */
    private  void printDatabasesInfo(final Host host,final Sheet sheet){
    	List<List<List<String>>> tables = host.reverseDatabaseList();
    	for(List<List<String>> table:tables){
    		printTableToSheet(table,sheet);
    	}
    }
    /**
     * 打印标题行
     * @author HP
     * @param title
     * @param sheet
     */
    private  void printTitleToSheet(final String title,final Sheet sheet){
    	Row row = (Row) sheet.createRow(nextRowNum());  
    	//和页面的看到的格式基本一致
        //打印页面标题
        Cell cell = row.createCell(0);  
        cell.setCellValue(title);
      
    }
    
    /**
     * 打印服务器的基本信息     
     * @param host
     * @param sheet 服务器详细信息Excel    工作表
     */
    public  void printServerBaseInfo(final Host host,Sheet sheet){
    	Host.HostDetail detail = host.getDetail();
    	//打印主机最基本的信息
    	printTableToSheet(host.revserseServerBaseInfo(),sheet);
    	
    	//打印网口类型表格
    	 if(detail != null)
    		 printTableToSheet(detail.reverseCardListToTable(),sheet);
    	 
    	//打印磁盘类型表格
    	 if(detail != null)
    		 printTableToSheet( detail.reverseFsListToTable(),sheet);
    }
    
    private  int nextRowNum = 0;
    private  int nextRowNum(){
    	return nextRowNum++;
    }
    private  void resetRowNum(){
    	 nextRowNum = 0;
    }
    /**
     * 打印表格类型的数据到工作表  例如：网口表     磁盘表
     * @param startRowNum   表格起始的行号
     * @param table   
     * @param sheet
     * @return 表格结束行号
     */
    public  void printTableToSheet(final List<List<String>> table,final Sheet sheet){
    	int i = 0;
    	for(List<String> tr:table){
    		printServerBaseInfoRowToSheet(nextRowNum(),tr,sheet);
    	}
    	 
    }
    /**
     * 
     * @param rowNum    例如： 1代表第一行
     * @param itemList
     * @param sheet
     */
    public  void printServerBaseInfoRowToSheet(final int rowNum,final List<String> itemList,final Sheet sheet){
    	//按列表顺序打印到行
    	Row row = (Row) sheet.createRow(rowNum);	
    	 for(int i = 0,length = itemList.size();i<length;i++){
    		printServerBaseInfoItemToRow(i,itemList.get(i),row);
    		
    	}
    }
    /**
     * 打印服务器基本信息项目       每一项一个小指标 例如：主机IP：10.10.10.10
     * @param cellNum  单元格位置   1代表第一个单元格
     * @param cellValue
     * @param row
     */
    public  void printServerBaseInfoItemToRow(final int cellNum,final String cellValue,final Row row){
    	
        Cell cell = row.createCell(cellNum);  
        cell.setCellValue(cellValue);
        /*CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setFont(createFonts());
         
        cell.setCellStyle(cellStyle);*/
        
    }
    public  void read(String path,String fileName,String fileType) throws IOException  
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
//创建主机列表     添加测试主机
        
        List<Host> hostList = new  ArrayList();
       
        Host host = new Host();
        hostList.add(host);
        host.setBuss("测试服务器2数据库");
        host.setIp("10.204.16.150");
        
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
        
        
        
        String path = "g:/";  
        String fileName = "11";  
        String fileType = "xlsx"; 
        POIReadAndWriteTool writer = POIReadAndWriteTool.getInstance();
        File file = new File(path, fileName, fileType);
        //writer.write(hostList,file);  
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
 