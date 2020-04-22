/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMultiplayerScreen;
import net.wurstclient.mixinterface.IServerList;

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
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addButton(new CleanUpButton(width / 2 - 100, height / 4 + 168 + 12,
			() -> "Cancel", "", b -> client.openScreen(prevScreen)));
		
		addButton(cleanUpButton = new CleanUpButton(width / 2 - 100,
			height / 4 + 144 + 12, () -> "Clean Up",
			"Start the Clean Up with the settings\n" + "you specified above.\n"
				+ "It might look like the game is not\n"
				+ "responding for a couple of seconds.",
			b -> cleanUp()));
		
		addButton(new CleanUpButton(width / 2 - 100, height / 4 - 24 + 12,
			() -> "Unknown Hosts: " + removeOrKeep(cleanupUnknown),
			"Servers that clearly don't exist.",
			b -> cleanupUnknown = !cleanupUnknown));
		
		addButton(new CleanUpButton(width / 2 - 100, height / 4 + 0 + 12,
			() -> "Outdated Servers: " + removeOrKeep(cleanupOutdated),
			"Servers that run a different Minecraft\n" + "version than you.",
			b -> cleanupOutdated = !cleanupOutdated));
		
		addButton(new CleanUpButton(width / 2 - 100, height / 4 + 24 + 12,
			() -> "Failed Ping: " + removeOrKeep(cleanupFailed),
			"All servers that failed the last ping.\n"
				+ "Make sure that the last ping is complete\n"
				+ "before you do this. That means: Go back,\n"
				+ "press the refresh button and wait until\n"
				+ "all servers are done refreshing.",
			b -> cleanupFailed = !cleanupFailed));
		
		addButton(new CleanUpButton(width / 2 - 100, height / 4 + 48 + 12,
			() -> "\"Grief me\" Servers: " + removeOrKeep(cleanupGriefMe),
			"All servers where the name starts with \"Grief me\"\n"
				+ "Useful for removing servers found by ServerFinder.",
			b -> cleanupGriefMe = !cleanupGriefMe));
		
		addButton(new CleanUpButton(width / 2 - 100, height / 4 + 72 + 12,
			() -> "\u00a7cRemove all Servers: " + yesOrNo(removeAll),
			"This will completely clear your server\n"
				+ "list. \u00a7cUse with caution!\u00a7r",
			b -> removeAll = !removeAll));
		
		addButton(new CleanUpButton(width / 2 - 100, height / 4 + 96 + 12,
			() -> "Rename all Servers: " + yesOrNo(cleanupRename),
			"Renames your servers to \"Grief me #1\",\n"
				+ "\"Grief me #2\", etc.",
			b -> cleanupRename = !cleanupRename));
		
		WurstClient.INSTANCE.getAnalytics()
			.trackPageView("/multiplayer/clean-up", "Clean Up");
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
		WurstClient.INSTANCE.getAnalytics().trackEvent("clean up", "start");
		
		if(removeAll)
		{
			((IServerList)prevScreen.getServerList()).clear();
			prevScreen.getServerList().saveFile();
			((IMultiplayerScreen)prevScreen).getServerListSelector()
				.setSelected(null);
			((IMultiplayerScreen)prevScreen).getServerListSelector()
				.setServers(prevScreen.getServerList());
			client.openScreen(prevScreen);
			return;
		}
		
		for(int i = prevScreen.getServerList().size() - 1; i >= 0; i--)
		{
			ServerInfo server = prevScreen.getServerList().get(i);
			if(cleanupUnknown
				&& "\u00a74Can\'t resolve hostname"
					.equals(server.label.getString())
				|| cleanupOutdated && server.protocolVersion != SharedConstants
					.getGameVersion().getProtocolVersion()
				|| cleanupFailed && server.ping != -2L && server.ping < 0L
				|| cleanupGriefMe && server.name.startsWith("Grief me"))
			{
				prevScreen.getServerList().remove(server);
				prevScreen.getServerList().saveFile();
				((IMultiplayerScreen)prevScreen).getServerListSelector()
					.setSelected(null);
				((IMultiplayerScreen)prevScreen).getServerListSelector()
					.setServers(prevScreen.getServerList());
			}
		}
		
		if(cleanupRename)
			for(int i = 0; i < prevScreen.getServerList().size(); i++)
			{
				ServerInfo server = prevScreen.getServerList().get(i);
				server.name = "Grief me #" + (i + 1);
				prevScreen.getServerList().saveFile();
				((IMultiplayerScreen)prevScreen).getServerListSelector()
					.setSelected(null);
				((IMultiplayerScreen)prevScreen).getServerListSelector()
					.setServers(prevScreen.getServerList());
			}
		
		client.openScreen(prevScreen);
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
		drawCenteredString(matrixStack, textRenderer, "Clean Up", width / 2, 20,
			16777215);
		drawCenteredString(matrixStack, textRenderer,
			"Please select the servers you want to remove:", width / 2, 36,
			10526880);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		renderButtonTooltip(matrixStack, mouseX, mouseY);
	}
	
	private void renderButtonTooltip(MatrixStack matrixStack, int mouseX,
		int mouseY)
	{
		for(AbstractButtonWidget button : buttons)
		{
			if(!button.isHovered() || !(button instanceof CleanUpButton))
				continue;
			
			CleanUpButton woButton = (CleanUpButton)button;
			if(woButton.tooltip.isEmpty())
				continue;
			
			renderTooltip(matrixStack, woButton.tooltip, mouseX, mouseY);
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
			super(x, y, 200, 20, new LiteralText(messageSupplier.get()),
				pressAction);
			this.messageSupplier = messageSupplier;
			
			if(tooltip.isEmpty())
				this.tooltip = Arrays.asList(new LiteralText[0]);
			else
			{
				String[] lines = tooltip.split("\n");
				
				LiteralText[] lines2 = new LiteralText[lines.length];
				for(int i = 0; i < lines.length; i++)
					lines2[i] = new LiteralText(lines[i]);
				
				this.tooltip = Arrays.asList(lines2);
			}
			
			addButton(this);
		}
		
		@Override
		public void onPress()
		{
			super.onPress();
			setMessage(new LiteralText(messageSupplier.get()));
		}
	}
}
