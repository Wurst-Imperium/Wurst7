/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.wurstclient.mixinterface.IMultiplayerScreen;
import net.wurstclient.util.MathUtils;

public class ServerFinderScreen extends Screen
{
	private MultiplayerScreen prevScreen;
	
	private TextFieldWidget ipBox;
	private TextFieldWidget maxThreadsBox;
	private ButtonWidget searchButton;
	
	private ServerFinderState state;
	private int maxThreads;
	private int checked;
	private int working;
	
	public ServerFinderScreen(MultiplayerScreen prevMultiplayerMenu)
	{
		super(Text.literal(""));
		prevScreen = prevMultiplayerMenu;
	}
	
	@Override
	public void init()
	{
		addDrawableChild(searchButton =
			ButtonWidget.builder(Text.literal("Search"), b -> searchOrCancel())
				.dimensions(width / 2 - 100, height / 4 + 96 + 12, 200, 20)
				.build());
		
		addDrawableChild(
			ButtonWidget
				.builder(Text.literal("Tutorial"),
					b -> Util.getOperatingSystem().open(
						"https://www.wurstclient.net/serverfinder-tutorial/"))
				.dimensions(width / 2 - 100, height / 4 + 120 + 12, 200, 20)
				.build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Back"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 - 100, height / 4 + 144 + 12, 200, 20)
			.build());
		
		ipBox = new TextFieldWidget(textRenderer, width / 2 - 100,
			height / 4 + 34, 200, 20, Text.literal(""));
		ipBox.setMaxLength(200);
		ipBox.setFocused(true);
		addSelectableChild(ipBox);
		
		maxThreadsBox = new TextFieldWidget(textRenderer, width / 2 - 32,
			height / 4 + 58, 26, 12, Text.literal(""));
		maxThreadsBox.setMaxLength(3);
		maxThreadsBox.setText("128");
		addSelectableChild(maxThreadsBox);
		
		setFocused(ipBox);
		state = ServerFinderState.NOT_RUNNING;
	}
	
	private void searchOrCancel()
	{
		if(state.isRunning())
		{
			state = ServerFinderState.CANCELLED;
			return;
		}
		
		state = ServerFinderState.RESOLVING;
		maxThreads = Integer.parseInt(maxThreadsBox.getText());
		checked = 0;
		working = 0;
		
		new Thread(this::findServers, "Server Finder").start();
	}
	
	private void findServers()
	{
		try
		{
			InetAddress addr =
				InetAddress.getByName(ipBox.getText().split(":")[0].trim());
			
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
	
	@Override
	public void tick()
	{
		ipBox.tick();
		
		searchButton
			.setMessage(Text.literal(state.isRunning() ? "Cancel" : "Search"));
		ipBox.active = !state.isRunning();
		maxThreadsBox.active = !state.isRunning();
		
		searchButton.active = MathUtils.isInteger(maxThreadsBox.getText())
			&& !ipBox.getText().isEmpty();
	}
	
	private boolean isServerInList(String ip)
	{
		for(int i = 0; i < prevScreen.getServerList().size(); i++)
			if(prevScreen.getServerList().get(i).address.equals(ip))
				return true;
			
		return false;
	}
	
	private void updatePingers(ArrayList<WurstServerPinger> pingers)
	{
		for(int i = 0; i < pingers.size(); i++)
			if(!pingers.get(i).isStillPinging())
			{
				checked++;
				if(pingers.get(i).isWorking())
				{
					working++;
					
					if(!isServerInList(pingers.get(i).getServerIP()))
					{
						prevScreen.getServerList()
							.add(
								new ServerInfo("Grief me #" + working,
									pingers.get(i).getServerIP(), false),
								false);
						prevScreen.getServerList().saveFile();
						((IMultiplayerScreen)prevScreen).getServerListSelector()
							.setSelected(null);
						((IMultiplayerScreen)prevScreen).getServerListSelector()
							.setServers(prevScreen.getServerList());
					}
				}
				pingers.remove(i);
			}
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			searchButton.onPress();
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(context);
		
		context.drawCenteredTextWithShadow(textRenderer, "Server Finder",
			width / 2, 20, 16777215);
		context.drawCenteredTextWithShadow(textRenderer,
			"This will search for servers with similar IPs", width / 2, 40,
			10526880);
		context.drawCenteredTextWithShadow(textRenderer,
			"to the IP you type into the field below.", width / 2, 50,
			10526880);
		context.drawCenteredTextWithShadow(textRenderer,
			"The servers it finds will be added to your server list.",
			width / 2, 60, 10526880);
		
		context.drawTextWithShadow(textRenderer, "Server address:",
			width / 2 - 100, height / 4 + 24, 10526880);
		ipBox.render(context, mouseX, mouseY, partialTicks);
		
		context.drawTextWithShadow(textRenderer, "Max. threads:",
			width / 2 - 100, height / 4 + 60, 10526880);
		maxThreadsBox.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredTextWithShadow(textRenderer, state.toString(),
			width / 2, height / 4 + 73, 10526880);
		
		context.drawTextWithShadow(textRenderer,
			"Checked: " + checked + " / 1792", width / 2 - 100, height / 4 + 84,
			10526880);
		context.drawTextWithShadow(textRenderer, "Working: " + working,
			width / 2 - 100, height / 4 + 94, 10526880);
		
		super.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void close()
	{
		state = ServerFinderState.CANCELLED;
		super.close();
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
