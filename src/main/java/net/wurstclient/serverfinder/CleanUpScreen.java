/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.serverfinder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class CleanUpScreen extends Screen
{
	private JoinMultiplayerScreen prevScreen;
	private Button cleanUpButton;
	
	private boolean removeAll;
	private boolean cleanupFailed = true;
	private boolean cleanupOutdated = true;
	private boolean cleanupRename = true;
	private boolean cleanupUnknown = true;
	private boolean cleanupGriefMe;
	
	public CleanUpScreen(JoinMultiplayerScreen prevScreen)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addRenderableWidget(new CleanUpButton(width / 2 - 100,
			height / 4 + 168 + 12, () -> "Cancel", "", b -> onClose()));
		
		addRenderableWidget(cleanUpButton = new CleanUpButton(width / 2 - 100,
			height / 4 + 144 + 12, () -> "Clean Up",
			"Start the Clean Up with the settings\n" + "you specified above.\n"
				+ "It might look like the game is not\n"
				+ "responding for a couple of seconds.",
			b -> cleanUp()));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 - 24 + 12,
				() -> "Unknown Hosts: " + removeOrKeep(cleanupUnknown),
				"Servers that clearly don't exist.",
				b -> cleanupUnknown = !cleanupUnknown));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 0 + 12,
				() -> "Outdated Servers: " + removeOrKeep(cleanupOutdated),
				"Servers that run a different Minecraft\n"
					+ "version than you.",
				b -> cleanupOutdated = !cleanupOutdated));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 24 + 12,
				() -> "Failed Ping: " + removeOrKeep(cleanupFailed),
				"All servers that failed the last ping.\n"
					+ "Make sure that the last ping is complete\n"
					+ "before you do this. That means: Go back,\n"
					+ "press the refresh button and wait until\n"
					+ "all servers are done refreshing.",
				b -> cleanupFailed = !cleanupFailed));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 48 + 12,
				() -> "\"Grief me\" Servers: " + removeOrKeep(cleanupGriefMe),
				"All servers where the name starts with \"Grief me\"\n"
					+ "Useful for removing servers found by ServerFinder.",
				b -> cleanupGriefMe = !cleanupGriefMe));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 72 + 12,
				() -> "\u00a7cRemove all Servers: " + yesOrNo(removeAll),
				"This will completely clear your server\n"
					+ "list. \u00a7cUse with caution!\u00a7r",
				b -> removeAll = !removeAll));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 96 + 12,
				() -> "Rename all Servers: " + yesOrNo(cleanupRename),
				"Renames your servers to \"Grief me #1\",\n"
					+ "\"Grief me #2\", etc.",
				b -> cleanupRename = !cleanupRename));
	}
	
	private String yesOrNo(boolean b)
	{
		return b ? "Yes" : "No";
	}
	
	private String removeOrKeep(boolean b)
	{
		return b ? "Remove" : "Keep";
	}
	
	private void cleanUp()
	{
		for(int i = prevScreen.getServers().size() - 1; i >= 0; i--)
		{
			ServerData server = prevScreen.getServers().get(i);
			
			if(removeAll || shouldRemove(server))
				prevScreen.getServers().remove(server);
		}
		
		if(cleanupRename)
			for(int i = 0; i < prevScreen.getServers().size(); i++)
			{
				ServerData server = prevScreen.getServers().get(i);
				server.name = "Grief me #" + (i + 1);
			}
		
		saveServerList();
		minecraft.setScreen(prevScreen);
	}
	
	private boolean shouldRemove(ServerData server)
	{
		if(server == null)
			return false;
		
		if(cleanupUnknown && isUnknownHost(server))
			return true;
		
		if(cleanupOutdated && !isSameProtocol(server))
			return true;
		
		if(cleanupFailed && isFailedPing(server))
			return true;
		
		if(cleanupGriefMe && isGriefMeServer(server))
			return true;
		
		return false;
	}
	
	private boolean isUnknownHost(ServerData server)
	{
		if(server.motd == null)
			return false;
		
		if(server.motd.getString() == null)
			return false;
		
		return server.motd.getString().equals("\u00a74Can\'t resolve hostname");
	}
	
	private boolean isSameProtocol(ServerData server)
	{
		return server.protocol == SharedConstants.getCurrentVersion()
			.getProtocolVersion();
	}
	
	private boolean isFailedPing(ServerData server)
	{
		return server.ping != -2L && server.ping < 0L;
	}
	
	private boolean isGriefMeServer(ServerData server)
	{
		return server.name != null && server.name.startsWith("Grief me");
	}
	
	private void saveServerList()
	{
		prevScreen.getServers().save();
		
		ServerSelectionList listWidget = prevScreen.serverSelectionList;
		listWidget.setSelected(null);
		listWidget.updateOnlineServers(prevScreen.getServers());
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			cleanUpButton.onPress();
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			onClose();
			return true;
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(context, mouseX, mouseY, partialTicks);
		context.drawCenteredString(font, "Clean Up", width / 2, 20,
			CommonColors.WHITE);
		context.drawCenteredString(font,
			"Please select the servers you want to remove:", width / 2, 36,
			CommonColors.LIGHT_GRAY);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		renderButtonTooltip(context, mouseX, mouseY);
	}
	
	private void renderButtonTooltip(GuiGraphics context, int mouseX,
		int mouseY)
	{
		for(AbstractWidget button : Screens.getButtons(this))
		{
			if(!button.isHoveredOrFocused()
				|| !(button instanceof CleanUpButton))
				continue;
			
			CleanUpButton cuButton = (CleanUpButton)button;
			
			if(cuButton.tooltip.isEmpty())
				continue;
			
			context.renderComponentTooltip(font, cuButton.tooltip, mouseX,
				mouseY);
			break;
		}
	}
	
	@Override
	public void onClose()
	{
		minecraft.setScreen(prevScreen);
	}
	
	private final class CleanUpButton extends Button
	{
		private final Supplier<String> messageSupplier;
		private final List<Component> tooltip;
		
		public CleanUpButton(int x, int y, Supplier<String> messageSupplier,
			String tooltip, OnPress pressAction)
		{
			super(x, y, 200, 20, Component.literal(messageSupplier.get()),
				pressAction, Button.DEFAULT_NARRATION);
			this.messageSupplier = messageSupplier;
			
			if(tooltip.isEmpty())
				this.tooltip = Arrays.asList();
			else
			{
				String[] lines = tooltip.split("\n");
				
				Component[] lines2 = new Component[lines.length];
				for(int i = 0; i < lines.length; i++)
					lines2[i] = Component.literal(lines[i]);
				
				this.tooltip = Arrays.asList(lines2);
			}
		}
		
		@Override
		public void onPress()
		{
			super.onPress();
			setMessage(Component.literal(messageSupplier.get()));
		}
	}
}
