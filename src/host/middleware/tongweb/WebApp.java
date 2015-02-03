package host.middleware.tongweb;

import java.util.HashSet;
import java.util.Set;

public class WebApp {

	private String name;
	private String contextRoot;
	private Set<String> vsNameSet = new HashSet();
	
	private String sourcePath;

	public String getContextRoot() {
		return contextRoot;
	}

	public void setContextRoot(String contextRoot) {
		if(contextRoot == null || "".equals(contextRoot)){
			contextRoot = this.name;
		}
		
		this.contextRoot = contextRoot.replaceAll("^/", "");
	}

	public Set<String> getVsNameSet() {
		return vsNameSet;
	}

	public void addVsName(String vsName ) {
		this.vsNameSet.add(vsName);
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "WebApp [name=" + name + ", contextRoot=" + contextRoot
				+ ", vsNameSet=" + vsNameSet + ", sourcePath=" + sourcePath
				+ "]";
	}

	
	
}
