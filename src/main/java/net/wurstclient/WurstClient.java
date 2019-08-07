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
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.command.CmdList;
import net.wurstclient.command.CmdProcessor;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ChatOutputListener;
import net.wurstclient.events.KeyPressListener;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.HackList;
import net.wurstclient.keybinds.KeybindList;
import net.wurstclient.keybinds.KeybindProcessor;
import net.wurstclient.mixinterface.IMinecraftClient;
import net.wurstclient.settings.SettingsFile;
import net.wurstclient.update.WurstUpdater;

public enum WurstClient
{
	INSTANCE;
	public static final MinecraftClient MC = MinecraftClient.getInstance();
	public static final IMinecraftClient IMC = (IMinecraftClient)MC;
	
	public static final String VERSION = "7.0pre2";
	public static final String MC_VERSION = "1.14.4";
	
	private WurstAnalytics analytics;
	private EventManager eventManager;
	private HackList hax;
	private CmdList cmds;
	private SettingsFile settingsFile;
	private KeybindList keybinds;
	private ClickGui gui;
	private RotationFaker rotationFaker;
	
	private boolean enabled = true;
	private static boolean guiInitialized;
	
	public void initialize()
	{
		System.out.println("Starting Wurst Client...");
		
		Path wurstFolder = createWurstFolder();
		
		String trackingID = "UA-52838431-5";
		String hostname = "client.wurstclient.net";
		Path analyticsFile = wurstFolder.resolve("analytics.json");
		analytics = new WurstAnalytics(trackingID, hostname, analyticsFile);
		
		eventManager = new EventManager(this);
		
		Path enabledHacksFile = wurstFolder.resolve("enabled-hacks.json");
		hax = new HackList(enabledHacksFile);
		
		cmds = new CmdList();
		
		Path settingsFile = wurstFolder.resolve("settings.json");
		this.settingsFile = new SettingsFile(settingsFile, hax, cmds);
		this.settingsFile.load();
		
		Path keybindsFile = wurstFolder.resolve("keybinds.json");
		keybinds = new KeybindList(keybindsFile);
		
		Path guiFile = wurstFolder.resolve("windows.json");
		gui = new ClickGui(guiFile);
		
		CmdProcessor cmdProcessor = new CmdProcessor(cmds);
		eventManager.add(ChatOutputListener.class, cmdProcessor);
		
		KeybindProcessor keybindProcessor =
			new KeybindProcessor(hax, keybinds, cmdProcessor);
		eventManager.add(KeyPressListener.class, keybindProcessor);
		
		rotationFaker = new RotationFaker();
		eventManager.add(PreMotionListener.class, rotationFaker);
		eventManager.add(PostMotionListener.class, rotationFaker);
		
		WurstClient.INSTANCE.getEventManager().add(UpdateListener.class,
			new WurstUpdater());
		
		analytics.trackPageView("/mc" + MC_VERSION + "/v" + VERSION,
			"Wurst " + VERSION + " MC" + MC_VERSION);
	}
	
	private Path createWurstFolder()
	{
		Path dotMinecraftFolder = MC.runDirectory.toPath();
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
	
	public void saveSettings()
	{
		settingsFile.save();
	}
	
	public HackList getHax()
	{
		return hax;
	}
	
	public CmdList getCmds()
	{
		return cmds;
	}
	
	public KeybindList getKeybinds()
	{
		return keybinds;
	}
	
	public ClickGui getGui()
	{
		if(!guiInitialized)
		{
			guiInitialized = true;
			gui.init();
		}
		
		return gui;
	}
	
	public RotationFaker getRotationFaker()
	{
		return rotationFaker;
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
