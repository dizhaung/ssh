package collect.model;

public class HintMsg {

	private int nowNum;//当前已完成
	private int maxNum;//最大主机数量
	private String ip;
	public int getNowNum() {
		return nowNum;
	}
	public void setNowNum(int nowNum) {
		this.nowNum = nowNum;
	}
	public int getMaxNum() {
		return maxNum;
	}
	public void setMaxNum(int maxNum) {
		this.maxNum = maxNum;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	@Override
	public String toString() {
		return "HintMsg [nowNum=" + nowNum + ", maxNum=" + maxNum + ", ip="
				+ ip + "]";
	}
	public HintMsg(int nowNum, int maxNum, String ip) {
		super();
		this.nowNum = nowNum;
		this.maxNum = maxNum;
		this.ip = ip;
	}
	
}
