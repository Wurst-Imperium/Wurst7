/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.MinecraftClient;
import net.wurstclient.analytics.WurstAnalytics;
import net.wurstclient.command.CmdList;
import net.wurstclient.command.CmdProcessor;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ChatOutputListener;
import net.wurstclient.hack.HackList;

public final class WurstClient
{
	public static final String VERSION = "7.0";
	
	private final WurstAnalytics analytics;
	private final EventManager eventManager;
	private final HackList hax;
	private final CmdList cmds;
	
	private boolean enabled = true;
	
	public WurstClient()
	{
		Path wurstFolder = createWurstFolder();
		
		String trackingID = "UA-52838431-5";
		String hostname = "client.wurstclient.net";
		Path analyticsFile = wurstFolder.resolve("analytics.json");
		analytics = new WurstAnalytics(trackingID, hostname, analyticsFile);
		
		eventManager = new EventManager(this);
		
		Path enabledHacksFile = wurstFolder.resolve("enabled-hacks.json");
		Path settingsFile = wurstFolder.resolve("settings.json");
		hax = new HackList(enabledHacksFile, settingsFile);
		
		cmds = new CmdList();
		
		CmdProcessor cmdProcessor = new CmdProcessor(cmds);
		eventManager.add(ChatOutputListener.class, cmdProcessor);
		
		analytics.trackPageView("/mc1.14.2/v" + VERSION,
			"Wurst " + VERSION + " MC1.14.2");
	}
	
	private Path createWurstFolder()
	{
		MinecraftClient mc = MinecraftClient.getInstance();
		Path dotMinecraftFolder = mc.runDirectory.toPath();
		Path wurstFolder = dotMinecraftFolder.resolve("wurst");
		
		try
		{
			Files.createDirectories(wurstFolder);
			
		}catch(IOException e)
		{
			throw new RuntimeException(
				"Couldn't create .minecraft/wurst folder.", e);
		}
		
		return wurstFolder;
	}
	
	public WurstAnalytics getAnalytics()
	{
		return analytics;
	}
	
	public EventManager getEventManager()
	{
		return eventManager;
	}
	
	public HackList getHax()
	{
		return hax;
	}
	
	public CmdList getCmds()
	{
		return cmds;
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
}
