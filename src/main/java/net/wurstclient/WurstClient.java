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
import net.wurstclient.event.EventManager;

public final class WurstClient
{
	public static final String VERSION = "7.0";
	
	private final Path wurstFolder;
	private final WurstAnalytics analytics;
	private final EventManager eventManager;
	
	private boolean enabled = true;
	
	public WurstClient()
	{
		wurstFolder = createWurstFolder();
		
		analytics = new WurstAnalytics("UA-52838431-5",
			"client.wurstclient.net", wurstFolder.resolve("analytics.json"));
		
		eventManager = new EventManager(this);
		
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
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
}
