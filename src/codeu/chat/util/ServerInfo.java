package codeu.chat.util;

public final class ServerInfo {
	public final Time startTime;
	public ServerInfo() {
		this.startTime = Time.now();
	}
	public ServerInfo(Time startTime) {
		this.startTime = startTime;
	}
	public String toString(){
		return startTime.toString();
	}
}