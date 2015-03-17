package export.io.spreadsheet.excel;

import java.awt.Color;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

public abstract class TableTheme {
	public final static  class GrayWhite extends TableTheme	{

		public GrayWhite(Workbook wb) {
			super(wb);
			// TODO Auto-generated constructor stub
		}

		@Override
		public CellStyle getCellStyle(final int i){
	    	CellStyle style = null;
	    	if(i%2 == 0){
				style = createCellStyleOfTableEvenRow();
			}else{
				style = createCellStyleOfTableOddRow();
			}
	    	return style;
	    }
		 private CellStyle createCellStyleOfTableOddRow(){ 
	     	 CellStyle style = wb.createCellStyle();
	     	 style.setFont(createTableFont());
	     	 XSSFColor color = new XSSFColor(Color.decode("#ffffff"));
	     	 ((XSSFCellStyle)style).setFillForegroundColor(color);
	     	 ((XSSFCellStyle)style).setFillPattern(FillPatternType.SOLID_FOREGROUND);
	     	 style.setAlignment(CellStyle.ALIGN_CENTER);
	         return style;
	    }
	    private CellStyle createCellStyleOfTableEvenRow(){
	      	 CellStyle style = wb.createCellStyle();
	      	 style.setFont(createTableFont());
	      	 
	      	 XSSFColor color = new XSSFColor(Color.decode("#cccccc"));
	      	 ((XSSFCellStyle)style).setFillForegroundColor(color);
	      	 ((XSSFCellStyle)style).setFillPattern(FillPatternType.SOLID_FOREGROUND);
	      	 style.setAlignment(CellStyle.ALIGN_CENTER);
	          return style;
	      }
	    /** 
	     * 创建标题单元格的字体 
	     * @param wb 
	     * @return 
	     */  
	    private Font createTableFont(){  
	        //创建Font对象  
	        Font font = wb.createFont();  
	        //设置字体  
	        font.setFontName("宋体");  
	        //着色  
	        font.setColor(HSSFColor.BLACK.index);  
	        //字体大小  
	        font.setFontHeightInPoints((short)12);  
	        return font;  
	    } 
		
	}
	public final static  class GrayHeaderWhiteBody extends TableTheme	{

		public GrayHeaderWhiteBody(Workbook wb) {
			super(wb);
			// TODO Auto-generated constructor stub
		}
		@Override
		public CellStyle getCellStyle(int i) {
			// TODO Auto-generated method stub
			CellStyle style = null;
	    	if(i == 0){
				style = createCellStyleOfTableHeaderRow();
			}else{
				style = createCellStyleOfTableBodyRow();
			}
	    	return style;
		}
		/** 
	     * 创建表格头单元格的字体 
	     * @param wb 
	     * @return 
	     */  
	    private Font createTableHeaderFont(){  
	        //创建Font对象  
	        Font font = wb.createFont();  
	        //设置字体  
	        font.setFontName("宋体");  
	        //着色  
	        font.setColor(HSSFColor.BLACK.index);  
	        //字体大小  
	        font.setFontHeightInPoints((short)16);  
	        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
	        return font;  
	    }  
	    
	    private Font createTableBodyFont(){  
	        //创建Font对象  
	        Font font = wb.createFont();  
	        //设置字体  
	        font.setFontName("宋体");  
	        //着色  
	        font.setColor(HSSFColor.BLACK.index);  
	        //字体大小  
	        font.setFontHeightInPoints((short)12);  
	        
	        return font;  
	    }  
		private CellStyle createCellStyleOfTableHeaderRow(){
			CellStyle style = wb.createCellStyle();
	     	 style.setFont(createTableHeaderFont());
	     	 XSSFColor color = new XSSFColor(Color.decode("#cccccc"));
	     	 ((XSSFCellStyle)style).setFillForegroundColor(color);
	     	 ((XSSFCellStyle)style).setFillPattern(FillPatternType.SOLID_FOREGROUND);
	     	 style.setAlignment(CellStyle.ALIGN_CENTER);
	         return style;
		}
		private CellStyle createCellStyleOfTableBodyRow(){
			CellStyle style = wb.createCellStyle();
	     	 style.setFont(createTableBodyFont());
	     	 XSSFColor color = new XSSFColor(Color.decode("#ffffff"));
	     	 ((XSSFCellStyle)style).setFillForegroundColor(color);
	     	 ((XSSFCellStyle)style).setFillPattern(FillPatternType.SOLID_FOREGROUND);
	     	 style.setAlignment(CellStyle.ALIGN_CENTER);
	         return style;
		}
	}
	protected final Workbook wb;
	
	public TableTheme(Workbook wb) {
		super();
		this.wb = wb;
	}
	public abstract CellStyle getCellStyle(final int i);
    	
}
