/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.wurstclient.mixinterface.IMultiplayerScreen;
import net.wurstclient.mixinterface.IScreen;

public class CleanUpScreen extends Screen
{
	private MultiplayerScreen prevScreen;
	private ButtonWidget cleanUpButton;
	
	private boolean removeAll;
	private boolean cleanupFailed = true;
	private boolean cleanupOutdated = true;
	private boolean cleanupRename = true;
	private boolean cleanupUnknown = true;
	private boolean cleanupGriefMe;
	
	public CleanUpScreen(MultiplayerScreen prevScreen)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 + 168 + 12,
				() -> "Cancel", "", b -> client.setScreen(prevScreen)));
		
		addDrawableChild(cleanUpButton = new CleanUpButton(width / 2 - 100,
			height / 4 + 144 + 12, () -> "Clean Up",
			"Start the Clean Up with the settings\n" + "you specified above.\n"
				+ "It might look like the game is not\n"
				+ "responding for a couple of seconds.",
			b -> cleanUp()));
		
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 - 24 + 12,
				() -> "Unknown Hosts: " + removeOrKeep(cleanupUnknown),
				"Servers that clearly don't exist.",
				b -> cleanupUnknown = !cleanupUnknown));
		
		addDrawableChild(new CleanUpButton(width / 2 - 100, height / 4 + 0 + 12,
			() -> "Outdated Servers: " + removeOrKeep(cleanupOutdated),
			"Servers that run a different Minecraft\n" + "version than you.",
			b -> cleanupOutdated = !cleanupOutdated));
		
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 + 24 + 12,
				() -> "Failed Ping: " + removeOrKeep(cleanupFailed),
				"All servers that failed the last ping.\n"
					+ "Make sure that the last ping is complete\n"
					+ "before you do this. That means: Go back,\n"
					+ "press the refresh button and wait until\n"
					+ "all servers are done refreshing.",
				b -> cleanupFailed = !cleanupFailed));
		
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 + 48 + 12,
				() -> "\"Grief me\" Servers: " + removeOrKeep(cleanupGriefMe),
				"All servers where the name starts with \"Grief me\"\n"
					+ "Useful for removing servers found by ServerFinder.",
				b -> cleanupGriefMe = !cleanupGriefMe));
		
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 + 72 + 12,
				() -> "\u00a7cRemove all Servers: " + yesOrNo(removeAll),
				"This will completely clear your server\n"
					+ "list. \u00a7cUse with caution!\u00a7r",
				b -> removeAll = !removeAll));
		
		addDrawableChild(
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
		for(int i = prevScreen.getServerList().size() - 1; i >= 0; i--)
		{
			ServerInfo server = prevScreen.getServerList().get(i);
			
			if(removeAll || shouldRemove(server))
				prevScreen.getServerList().remove(server);
		}
		
		if(cleanupRename)
			for(int i = 0; i < prevScreen.getServerList().size(); i++)
			{
				ServerInfo server = prevScreen.getServerList().get(i);
				server.name = "Grief me #" + (i + 1);
			}
		
		saveServerList();
		client.setScreen(prevScreen);
	}
	
	private boolean shouldRemove(ServerInfo server)
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
	
	private boolean isUnknownHost(ServerInfo server)
	{
		if(server.label == null)
			return false;
		
		if(server.label.getString() == null)
			return false;
		
		return server.label.getString()
			.equals("\u00a74Can\'t resolve hostname");
	}
	
	private boolean isSameProtocol(ServerInfo server)
	{
		return server.protocolVersion == SharedConstants.getGameVersion()
			.getProtocolVersion();
	}
	
	private boolean isFailedPing(ServerInfo server)
	{
		return server.ping != -2L && server.ping < 0L;
	}
	
	private boolean isGriefMeServer(ServerInfo server)
	{
		return server.name != null && server.name.startsWith("Grief me");
	}
	
	private void saveServerList()
	{
		prevScreen.getServerList().saveFile();
		
		MultiplayerServerListWidget serverListSelector =
			((IMultiplayerScreen)prevScreen).getServerListSelector();
		
		serverListSelector.setSelected(null);
		serverListSelector.setServers(prevScreen.getServerList());
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			cleanUpButton.onPress();
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(matrixStack);
		drawCenteredText(matrixStack, textRenderer, "Clean Up", width / 2, 20,
			16777215);
		drawCenteredText(matrixStack, textRenderer,
			"Please select the servers you want to remove:", width / 2, 36,
			10526880);
		
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		renderButtonTooltip(matrixStack, mouseX, mouseY);
	}
	
	private void renderButtonTooltip(MatrixStack matrixStack, int mouseX,
		int mouseY)
	{
		for(Drawable d : ((IScreen)this).getButtons())
		{
			if(!(d instanceof ClickableWidget))
				continue;
			
			ClickableWidget button = (ClickableWidget)d;
			
			if(!button.isHovered() || !(button instanceof CleanUpButton))
				continue;
			
			CleanUpButton cuButton = (CleanUpButton)button;
			
			if(cuButton.tooltip.isEmpty())
				continue;
			
			renderTooltip(matrixStack, cuButton.tooltip, mouseX, mouseY);
			break;
		}
	}
	
	private final class CleanUpButton extends ButtonWidget
	{
		private final Supplier<String> messageSupplier;
		private final List<Text> tooltip;
		
		public CleanUpButton(int x, int y, Supplier<String> messageSupplier,
			String tooltip, PressAction pressAction)
		{
			super(x, y, 200, 20, Text.literal(messageSupplier.get()),
				pressAction);
			this.messageSupplier = messageSupplier;
			
			if(tooltip.isEmpty())
				this.tooltip = Arrays.asList();
			else
			{
				String[] lines = tooltip.split("\n");
				
				Text[] lines2 = new Text[lines.length];
				for(int i = 0; i < lines.length; i++)
					lines2[i] = Text.literal(lines[i]);
				
				this.tooltip = Arrays.asList(lines2);
			}
		}
		
		@Override
		public void onPress()
		{
			super.onPress();
			setMessage(Text.literal(messageSupplier.get()));
		}
	}
}
