/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.serverfinder;

import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.network.ServerEntryNetworkPart;
import net.minecraft.client.options.ServerEntry;

public class WurstServerPinger
{
	private static final AtomicInteger threadNumber = new AtomicInteger(0);
	private ServerEntry server;
	private boolean done = false;
	private boolean failed = false;
	
	public void ping(String ip)
	{
		ping(ip, 25565);
	}
	
	public void ping(String ip, int port)
	{
		server = new ServerEntry("", ip + ":" + port, false);
		
		new Thread(() -> pingInCurrentThread(ip, port),
			"Wurst Server Pinger #" + threadNumber.incrementAndGet()).start();
	}
	
	private void pingInCurrentThread(String ip, int port)
	{
		ServerEntryNetworkPart pinger = new ServerEntryNetworkPart();
		System.out.println("Pinging " + ip + ":" + port + "...");
		
		try
		{
			pinger.method_3003(server);
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
		
		pinger.method_3004();
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
	
	public boolean isOtherVersion()
	{
		return server.protocolVersion != 47;
	}
	
	public String getServerIP()
	{
		return server.address;
	}
}
