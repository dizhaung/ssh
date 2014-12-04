package export.io;

public class File {

	private String path;
	private String fileName;
	private String fileType;
	private java.io.File file;
	
	public java.io.File  getFile() {
		return file;
	}
	public void setFile(java.io.File  file) {
		this.file = file;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	/**
	 * 
	 * @param path   必需带/分隔符
	 * @param fileName
	 * @param fileType
	 */
	public File(String path, String fileName, String fileType) {
		this.file = new java.io.File(path+fileName+"."+fileType);
		this.path = path;
		this.fileName = fileName;
		this.fileType = fileType;
	}
	public File() {
		super();
	}
	
	
}
