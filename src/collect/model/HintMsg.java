package collect.model;

public class HintMsg {

	private int nowNum;//当前已完成
	private int maxNum;//最大主机数量
	private String ip;
	private String msg;
	
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
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
				+ ip + ", msg=" + msg + "]";
	}
	public HintMsg(int nowNum, int maxNum, String ip, String msg) {
		super();
		this.nowNum = nowNum;
		this.maxNum = maxNum;
		this.ip = ip;
		this.msg = msg;
	}
	
	
}
