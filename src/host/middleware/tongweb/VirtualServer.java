package host.middleware.tongweb;

import java.util.HashSet;
import java.util.Set;

public class VirtualServer {

	private String id;
	private Set<String> httpListenerSet = new HashSet();
	
	public VirtualServer() {
		super();
	}
	public VirtualServer(String id) {
		super();
		this.id = id;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void addHttpListener(String httpListener){
		httpListenerSet.add(httpListener);
	}
	public Set<String> getHttpListenerSet() {
		return httpListenerSet;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VirtualServer other = (VirtualServer) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "VirtualServer [id=" + id + ", httpListenerSet="
				+ httpListenerSet + "]";
	}
	
}
