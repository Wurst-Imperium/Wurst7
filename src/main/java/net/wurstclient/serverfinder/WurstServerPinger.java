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

public class WurstServerPinger implements IServerFinderDisconnectListener, IServerFinderDoneListener
{
	private static final AtomicInteger threadNumber = new AtomicInteger(0);
	private WurstServerInfo server;
	private boolean done = false;
	private boolean failed = false;
	private Thread thread;
	private int pingPort;
	private WurstServerListPinger pinger;
	private boolean notifiedDoneListeners = false;
	private boolean scanPorts;
	private int searchNumber;
	private int currentIncrement = 1;
	private ArrayList<IServerFinderDoneListener> doneListeners = new ArrayList<>();
	private int portPingers = 0;
	private int successfulPortPingers = 0;
	private String pingIP;
	private final Object portPingerLock = new Object();
	
	public WurstServerPinger(boolean scanPorts, int searchNumber) {
		pinger = new WurstServerListPinger();
		pinger.addServerFinderDisconnectListener(this);
		this.scanPorts = scanPorts;
		this.searchNumber = searchNumber;
	}
	
	public void addServerFinderDoneListener(IServerFinderDoneListener listener) {
		doneListeners.add(listener);
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
		
		pingIP = ip;
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
	
	private void runPortIncrement(String ip) {
		synchronized(portPingerLock) {
			portPingers = 0;
			successfulPortPingers = 0;
		}
		for (int i = currentIncrement; i < currentIncrement * 10; i++) {
			if (isOldSearch())
				return;
			WurstServerPinger pp1 = new WurstServerPinger(false, searchNumber);
			WurstServerPinger pp2 = new WurstServerPinger(false, searchNumber);
			for (int j = 0; j < doneListeners.size(); j++) {
				pp1.addServerFinderDoneListener(doneListeners.get(j));
				pp2.addServerFinderDoneListener(doneListeners.get(j));
			}
			pp1.addServerFinderDoneListener(this);
			pp2.addServerFinderDoneListener(this);
			if (ServerFinderScreen.instance != null && !isOldSearch()) {
				ServerFinderScreen.instance.incrementTargetChecked(2);
			}
			pp1.ping(ip, 25565 - i);
			pp2.ping(ip, 25565 + i);
		}
		synchronized(portPingerLock) {
			currentIncrement *= 10;
		}
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
				runPortIncrement(ip);
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
			notifyDoneListeners(false);
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
		notifyDoneListeners(false);
	}
	
	private void notifyDoneListeners(boolean failure) {
		synchronized(this) {
			if (!notifiedDoneListeners) {
				notifiedDoneListeners = true;
				for (IServerFinderDoneListener doneListener : doneListeners) {
					if (doneListener != null) {
						if (failure) {
							doneListener.onServerFailed(this);
						}
						else {
							doneListener.onServerDone(this);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onServerFailed() {
		if (isOldSearch())
			return;
		
		pinger.cancel();
		done = true;
		notifyDoneListeners(true);
	}

	@Override
	public void onServerDone(WurstServerPinger pinger) {
		synchronized(portPingerLock) {
			portPingers += 1;
			if (pinger.isWorking())
				successfulPortPingers += 1;
			if (portPingers == (currentIncrement / 10) * 18 && currentIncrement <= 1000 && successfulPortPingers > 0) {
				new Thread(() -> runPortIncrement(pingIP)).start();
			}
		}
	}

	@Override
	public void onServerFailed(WurstServerPinger pinger) {
		synchronized(portPingerLock) {
			portPingers += 1;
		}
	}
}
