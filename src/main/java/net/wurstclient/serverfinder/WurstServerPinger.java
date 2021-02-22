/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.serverfinder;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.wurstclient.serverfinder.ServerFinderScreen.ServerFinderState;

public class WurstServerPinger implements IServerFinderDisconnectListener
{
	private static final AtomicInteger threadNumber = new AtomicInteger(0);
	private WurstServerInfo server;
	private boolean done = false;
	private boolean failed = false;
	private Thread thread;
	private int pingPort;
	private WurstServerListPinger pinger;
	private IServerFinderDoneListener doneListener;
	private boolean notifiedDoneListener = false;
	private boolean scanPorts;
	private int searchNumber;
	
	public WurstServerPinger(IServerFinderDoneListener doneListener, boolean scanPorts, int searchNumber) {
		pinger = new WurstServerListPinger();
		pinger.addServerFinderDisconnectListener(this);
		this.doneListener = doneListener;
		this.scanPorts = scanPorts;
		this.searchNumber = searchNumber;
	}
	
	public void ping(String ip)
	{
		ping(ip, 25565);
	}
	
	public int getSearchNumber() {
		return searchNumber;
	}
	
	public Thread getThread() {
		return thread;
	}
	
	public int getPingPort() {
		return pingPort;
	}
	
	public WurstServerInfo getServerInfo() {
		return server;
	}
	
	public void ping(String ip, int port)
	{
		if (isOldSearch())
			return;
		
		pingPort = port;
		server = new WurstServerInfo("", ip + ":" + port, false);
		server.version = null;
		
		if (scanPorts) {
			thread = new Thread(() -> pingInCurrentThread(ip, port),
					"Wurst Server Pinger #" + threadNumber.incrementAndGet());
		}
		else {
			thread = new Thread(() -> pingInCurrentThread(ip, port),
					"Wurst Server Pinger #" + threadNumber + ", " + port);
		}
		thread.start();
	}
	
	public WurstServerListPinger getServerListPinger() {
		return pinger;
	}
	
	private boolean isOldSearch() {
		return ServerFinderScreen.instance == null || ServerFinderScreen.instance.getState() == ServerFinderState.CANCELLED || ServerFinderScreen.getSearchNumber() != searchNumber;
	}
	
	private void pingInCurrentThread(String ip, int port)
	{
		if (isOldSearch())
			return;
		
		//System.out.println("Pinging " + ip + ":" + port + "...");
		
		try
		{
			pinger.add(server, () -> {});
			//System.out.println("Ping successful: " + ip + ":" + port);
			if (scanPorts) {
				for (int i = 1; i <= 100; i++) {
					if (isOldSearch())
						return;
					WurstServerPinger pp1 = new WurstServerPinger(doneListener, false, searchNumber);
					WurstServerPinger pp2 = new WurstServerPinger(doneListener, false, searchNumber);
					if (ServerFinderScreen.instance != null && !isOldSearch()) {
						ServerFinderScreen.instance.incrementTargetChecked(2);
					}
					pp1.ping(ip, port - i);
					pp2.ping(ip, port + i);
				}
			}
			
		}catch(UnknownHostException e)
		{
			//System.out.println("Unknown host: " + ip + ":" + port);
			failed = true;
			
		}catch(Exception e2)
		{
			//System.out.println("Ping failed: " + ip + ":" + port);
			failed = true;
		}
		
		if (failed) {
			pinger.cancel();
			done = true;
			synchronized(this) {
				if (doneListener != null && !notifiedDoneListener) {
					doneListener.onServerDone(this);
					notifiedDoneListener = true;
				}
			}
		}
	}
	
	public boolean isStillPinging()
	{
		return !done;
	}
	
	public boolean isWorking()
	{
		return !failed;
	}
	
	public boolean isOtherVersion()
	{
		return server.protocolVersion != 47;
	}
	
	public String getServerIP()
	{
		return server.address;
	}

	@Override
	public void onServerDisconnect() {
		if (isOldSearch())
			return;
		
		pinger.cancel();
		done = true;
		synchronized(this) {
			if (doneListener != null && !notifiedDoneListener) {
				doneListener.onServerDone(this);
				notifiedDoneListener = true;
			}
		}
	}
	
	@Override
	public void onServerFailed() {
		if (isOldSearch())
			return;
		
		pinger.cancel();
		done = true;
		synchronized(this) {
			if (doneListener != null && !notifiedDoneListener) {
				doneListener.onServerFailed(this);
				notifiedDoneListener = true;
			}
		}
	}
}
