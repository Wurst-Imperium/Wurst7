package net.wurstclient.serverfinder;

public interface IServerFinderDoneListener {
	
	public void onServerDone(WurstServerPinger pinger);
	
	public void onServerFailed(WurstServerPinger pinger);
	
}
