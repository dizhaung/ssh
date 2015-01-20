package host.middleware.tomcat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * 对应server.xml 根元素Server下的DOM树结构
 * 子节点元素类型作为父节点元素类型的内部类 来组织这种结构
 * 类的属性只映射了本程序需要关注的几个关键属性
 * DOM树 各节点关系见      Tomcat docs  Configuration
 * @author HP
 * @date 2015-1-16 下午5:16:46
 *
 */
public class Service {

	private String name;
	
	private Set<Connector> connectorSet = new HashSet();
	private Set<Engine> engineSet = new HashSet();
	
	public boolean hasConnector(){
		return !connectorSet.isEmpty();
	}
	public boolean hasEngine(){
		return !engineSet.isEmpty();
	}
	@Override
	public String toString() {
		return "Service [name=" + name + ", connectorSet=" + connectorSet
				+ ", engineSet=" + engineSet + "]";
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set<Connector> getConnectorSet() {
		return connectorSet;
	}
	public void addConnector(Connector connector ) {
		this.connectorSet.add(connector);
	}
	public Set<Engine> getEngineSet() {
		return engineSet;
	}
	public void addEngine(Engine engine ) {
		this.engineSet.add(engine);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Service other = (Service) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public  class Connector {
		
		private String port;
		private String protocol;
		public String getPort() {
			return port;
		}
		public void setPort(String port) {
			this.port = port;
		}
		public String getProtocol() {
			return protocol;
		}
		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}
		@Override
		public String toString() {
			return "Connector [port=" + port + ", protocol=" + protocol + "]";
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((port == null) ? 0 : port.hashCode());
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
			Connector other = (Connector) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (port == null) {
				if (other.port != null)
					return false;
			} else if (!port.equals(other.port))
				return false;
			return true;
		}
		private Service getOuterType() {
			return Service.this;
		}
		
	}
	public final class HttpConnector extends Connector{
		
	}
	public final class Engine {
		private Set<Host> hostSet = new HashSet();
		
		private String name;
		private String defaultHost;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			Engine other = (Engine) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Engine [hostList=" + hostSet + ", name=" + name
					+ ", defaultHost=" + defaultHost + "]";
		}

		public Set<Host> getHostSet() {
			return hostSet;
		}

		public void addHost( Host host ) {
			this.hostSet.add(host);
		}
		public boolean hasHost(){
			return !this.hostSet.isEmpty();
		}
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDefaultHost() {
			return defaultHost;
		}

		public void setDefaultHost(String defaultHost) {
			this.defaultHost = defaultHost;
		}

		public class Host{
			private	Set<Context> contextSet = new HashSet();
			private String appBase;
			private String name;
			
			@Override
			public String toString() {
				return "Host [contextSet=" + contextSet + ", appBase="
						+ appBase + ", name=" + name + "]";
			}

			public Set<Context> getContextSet() {
				return contextSet;
			}

			public void addContext ( Context context ) {
				this.contextSet.add(context);
			}
			public boolean hasContext(){
				return !this.contextSet.isEmpty();
			}
			public String getAppBase() {
				return appBase;
			}

			public void setAppBase(String appBase) {
				this.appBase = appBase;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public final class Context{
				private String path;
				private String docBase;
				public String getPath() {
					return path;
				}
				
				@Override
				public String toString() {
					return "Context [path=" + path + ", docBase=" + docBase
							+ "]";
				}

				@Override
				public int hashCode() {
					final int prime = 31;
					int result = 1;
					result = prime * result + getOuterType().hashCode();
					result = prime * result
							+ ((path == null) ? 0 : path.hashCode());
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
					Context other = (Context) obj;
					if (!getOuterType().equals(other.getOuterType()))
						return false;
					if (path == null) {
						if (other.path != null)
							return false;
					} else if (!path.equals(other.path))
						return false;
					return true;
				}

				public void setPath(String path) {
					this.path = path;
				}

				public String getDocBase() {
					return docBase;
				}

				public void setDocBase(String docBase) {
					this.docBase = docBase;
				}

				private Host getOuterType() {
					return Host.this;
				}

			}
		}

		private Service getOuterType() {
			return Service.this;
		}
	}
}
