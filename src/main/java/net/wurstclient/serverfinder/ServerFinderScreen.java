/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.serverfinder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Util;
import net.wurstclient.util.MathUtils;

public class ServerFinderScreen extends Screen
{
	private final JoinMultiplayerScreen prevScreen;
	
	private EditBox ipBox;
	private EditBox maxThreadsBox;
	private Button searchButton;
	
	private ServerFinderState state;
	private int maxThreads;
	private int checked;
	private int working;
	
	public ServerFinderScreen(JoinMultiplayerScreen prevScreen)
	{
		super(Component.literal("Server Finder"));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addRenderableWidget(searchButton = Button
			.builder(Component.literal("Search"), b -> searchOrCancel())
			.bounds(width / 2 - 100, height / 4 + 96 + 12, 200, 20).build());
		searchButton.active = false;
		
		addRenderableWidget(Button
			.builder(Component.literal("Tutorial"),
				b -> Util.getPlatform().openUri(
					"https://www.wurstclient.net/serverfinder-tutorial/"))
			.bounds(width / 2 - 100, height / 4 + 120 + 12, 200, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Back"), b -> onClose())
			.bounds(width / 2 - 100, height / 4 + 144 + 12, 200, 20).build());
		
		ipBox = new EditBox(font, width / 2 - 100, height / 4 + 34, 200, 20,
			Component.empty());
		ipBox.setMaxLength(200);
		addWidget(ipBox);
		setFocused(ipBox);
		
		maxThreadsBox = new EditBox(font, width / 2 - 32, height / 4 + 58, 26,
			12, Component.empty());
		maxThreadsBox.setMaxLength(3);
		maxThreadsBox.setValue("128");
		addWidget(maxThreadsBox);
		
		state = ServerFinderState.NOT_RUNNING;
	}
	
	private void searchOrCancel()
	{
		if(state.isRunning())
		{
			state = ServerFinderState.CANCELLED;
			ipBox.active = true;
			maxThreadsBox.active = true;
			searchButton.setMessage(Component.literal("Search"));
			return;
		}
		
		state = ServerFinderState.RESOLVING;
		maxThreads = Integer.parseInt(maxThreadsBox.getValue());
		ipBox.active = false;
		maxThreadsBox.active = false;
		searchButton.setMessage(Component.literal("Cancel"));
		checked = 0;
		working = 0;
		
		new Thread(this::findServers, "Server Finder").start();
	}
	
	private void findServers()
	{
		try
		{
			InetAddress addr =
				InetAddress.getByName(ipBox.getValue().split(":")[0].trim());
			
			int[] ipParts = new int[4];
			for(int i = 0; i < 4; i++)
				ipParts[i] = addr.getAddress()[i] & 0xff;
			
			state = ServerFinderState.SEARCHING;
			ArrayList<WurstServerPinger> pingers = new ArrayList<>();
			int[] changes = {0, 1, -1, 2, -2, 3, -3};
			for(int change : changes)
				for(int i2 = 0; i2 <= 255; i2++)
				{
					if(state == ServerFinderState.CANCELLED)
						return;
					
					int[] ipParts2 = ipParts.clone();
					ipParts2[2] = ipParts[2] + change & 0xff;
					ipParts2[3] = i2;
					String ip = ipParts2[0] + "." + ipParts2[1] + "."
						+ ipParts2[2] + "." + ipParts2[3];
					
					WurstServerPinger pinger = new WurstServerPinger();
					pinger.ping(ip);
					pingers.add(pinger);
					while(pingers.size() >= maxThreads)
					{
						if(state == ServerFinderState.CANCELLED)
							return;
						
						updatePingers(pingers);
					}
				}
			while(pingers.size() > 0)
			{
				if(state == ServerFinderState.CANCELLED)
					return;
				
				updatePingers(pingers);
			}
			state = ServerFinderState.DONE;
			
		}catch(UnknownHostException e)
		{
			state = ServerFinderState.UNKNOWN_HOST;
			
		}catch(Exception e)
		{
			e.printStackTrace();
			state = ServerFinderState.ERROR;
		}
	}
	
	private void updatePingers(ArrayList<WurstServerPinger> pingers)
	{
		for(int i = 0; i < pingers.size(); i++)
		{
			WurstServerPinger pinger = pingers.get(i);
			if(pinger.isStillPinging())
				continue;
			
			checked++;
			if(pinger.isWorking())
			{
				working++;
				String name = "Grief me #" + working;
				String ip = pinger.getServerIP();
				addServerToList(name, ip);
			}
			
			pingers.remove(i);
			i--;
		}
	}
	
	// Basically what MultiplayerScreen.addEntry() does,
	// but without changing the current screen.
	private void addServerToList(String name, String ip)
	{
		ServerList serverList = prevScreen.getServers();
		if(serverList.get(ip) != null)
			return;
		
		serverList.add(new ServerData(name, ip, Type.OTHER), false);
		serverList.save();
		
		ServerSelectionList listWidget = prevScreen.serverSelectionList;
		listWidget.setSelected(null);
		listWidget.updateOnlineServers(serverList);
	}
	
	@Override
	public void tick()
	{
		searchButton.active = MathUtils.isInteger(maxThreadsBox.getValue())
			&& !ipBox.getValue().isEmpty();
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent context, boolean doubleClick)
	{
		if(context.button() == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			onClose();
			return true;
		}
		
		return super.mouseClicked(context, doubleClick);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		context.drawCenteredString(font, "Server Finder", width / 2, 20,
			CommonColors.WHITE);
		context.drawCenteredString(font,
			"This will search for servers with similar IPs", width / 2, 40,
			CommonColors.LIGHT_GRAY);
		context.drawCenteredString(font,
			"to the IP you type into the field below.", width / 2, 50,
			CommonColors.LIGHT_GRAY);
		context.drawCenteredString(font,
			"The servers it finds will be added to your server list.",
			width / 2, 60, CommonColors.LIGHT_GRAY);
		
		context.drawString(font, "Server address:", width / 2 - 100,
			height / 4 + 24, CommonColors.LIGHT_GRAY);
		ipBox.render(context, mouseX, mouseY, partialTicks);
		
		context.drawString(font, "Max. threads:", width / 2 - 100,
			height / 4 + 60, CommonColors.LIGHT_GRAY);
		maxThreadsBox.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredString(font, state.toString(), width / 2,
			height / 4 + 73, CommonColors.LIGHT_GRAY);
		
		context.drawString(font, "Checked: " + checked + " / 1792",
			width / 2 - 100, height / 4 + 84, CommonColors.LIGHT_GRAY);
		context.drawString(font, "Working: " + working, width / 2 - 100,
			height / 4 + 94, CommonColors.LIGHT_GRAY);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void onClose()
	{
		state = ServerFinderState.CANCELLED;
		minecraft.setScreen(prevScreen);
	}
	
	enum ServerFinderState
	{
		NOT_RUNNING(""),
		SEARCHING("\u00a72Searching..."),
		RESOLVING("\u00a72Resolving..."),
		UNKNOWN_HOST("\u00a74Unknown Host!"),
		CANCELLED("\u00a74Cancelled!"),
		DONE("\u00a72Done!"),
		ERROR("\u00a74An error occurred!");
		
		private final String name;
		
		private ServerFinderState(String name)
		{
			this.name = name;
		}
		
		public boolean isRunning()
		{
			return this == SEARCHING || this == RESOLVING;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
