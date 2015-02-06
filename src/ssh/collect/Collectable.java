package ssh.collect;

import host.Host;
import host.PortLoadConfig;

import java.util.List;

import ssh.SSHClient;
import ssh.Shell;

public interface Collectable {

	/**
	 * 采集aix主机信息
	 * @param shell     建立ssh通道，连续发起采集命令
	 * @param ssh	建立ssh通道，只能发起单个采集命令
	 * @param h		
	 * @param allLoadBalancerFarmAndServerInfo  所有的负载均衡的配置文件信息
	 */
	public abstract void collect(Shell shell, final SSHClient ssh,
			final Host h, final List<PortLoadConfig> portListFromLoad);
	
	abstract void collectHostDetail(Shell shell, SSHClient ssh, Host h,
			List<PortLoadConfig> portListFromLoad);

	abstract void collectOracle(Shell shell, Host h);

	abstract void collectWeblogic(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad);

	abstract void collectDB2(Shell shell, Host h);
	abstract void collectWebSphere(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad);

	abstract void collectTomcat(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad);
	abstract  void collectTongweb(Shell shell, Host h,
			List<PortLoadConfig> portListFromLoad) ;


}