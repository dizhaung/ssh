package host.database.db2;

import java.util.Collections;
import java.util.List;

public final class Tablespace{
	private String id;
	private long pageSize;
	private List<Container> containerList;
	
	
	public List<Container> getContainerList() {
		if(containerList == null){
			return Collections.EMPTY_LIST;
		}
		return containerList;
	}
	public void setContainerList(List<Container> containerList) {
		this.containerList = containerList;
	}
	public class Container{
		private String id;
		private	long totalPages;
		private String name;
		private String type;
		private float	totalSize;
		
		public float getTotalSize() {
			return totalSize;
		}
		public void setTotalSize(float totalSize) {
			this.totalSize = totalSize;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public long getTotalPages() {
			return totalPages;
		}
		public void setTotalPages(long totalPages) {
			this.totalPages = totalPages;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		@Override
		public String toString() {
			return "Container [id=" + id + ", totalPages=" + totalPages
					+ ", name=" + name + ", type=" + type + ", totalSize="
					+ totalSize + "]";
		}
		
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public long getPageSize() {
		return pageSize;
	}
	public void setPageSize(long pageSize) {
		this.pageSize = pageSize;
	}
	@Override
	public String toString() {
		return "Tablespace [id=" + id + ", pageSize=" + pageSize
				+ ", containerList=" + containerList + "]";
	}
	
}
