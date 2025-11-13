/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.serverfinder;

import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.wurstclient.WurstClient;

public class WurstServerPinger
{
	private static final AtomicInteger threadNumber = new AtomicInteger(0);
	private ServerData server;
	private boolean done = false;
	private boolean failed = false;
	
	public void ping(String ip)
	{
		ping(ip, 25565);
	}
	
	public void ping(String ip, int port)
	{
		server = new ServerData("", ip + ":" + port, Type.OTHER);
		
		new Thread(() -> pingInCurrentThread(ip, port),
			"Wurst Server Pinger #" + threadNumber.incrementAndGet()).start();
	}
	
	private void pingInCurrentThread(String ip, int port)
	{
		ServerStatusPinger pinger = new ServerStatusPinger();
		System.out.println("Pinging " + ip + ":" + port + "...");
		
		try
		{
			pinger.pingServer(server, () -> {}, () -> {}, EventLoopGroupHolder
				.remote(WurstClient.MC.options.useNativeTransport()));
			System.out.println("Ping successful: " + ip + ":" + port);
			
		}catch(UnknownHostException e)
		{
			System.out.println("Unknown host: " + ip + ":" + port);
			failed = true;
			
		}catch(Exception e2)
		{
			System.out.println("Ping failed: " + ip + ":" + port);
			failed = true;
		}
		
		pinger.removeAll();
		done = true;
	}
	
	public boolean isStillPinging()
	{
		return !done;
	}
	
	public boolean isWorking()
	{
		return !failed;
	}
	
	public String getServerIP()
	{
		return server.ip;
	}
}
