/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;
import net.wurstclient.WurstClient;
import net.wurstclient.analytics.WurstAnalytics;
import net.wurstclient.commands.FriendsCmd;
import net.wurstclient.hacks.XRayHack;
import net.wurstclient.settings.CheckboxSetting;

public class WurstOptionsScreen extends Screen
{
	private Screen prevScreen;
	
	public WurstOptionsScreen(Screen prevScreen)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addButton(new ButtonWidget(width / 2 - 100, height / 4 + 144 - 16, 200,
			20, new LiteralText("Back"), b -> client.openScreen(prevScreen)));
		
		addSettingButtons();
		addManagerButtons();
		addLinkButtons();
	}
	
	private void addSettingButtons()
	{
		FriendsCmd friendsCmd = WurstClient.INSTANCE.getCmds().friendsCmd;
		CheckboxSetting middleClickFriends = friendsCmd.getMiddleClickFriends();
		WurstAnalytics analytics = WurstClient.INSTANCE.getAnalytics();
		
		new WurstOptionsButton(-154, 24,
			() -> "Click Friends: "
				+ (middleClickFriends.isChecked() ? "ON" : "OFF"),
			middleClickFriends.getDescription(), b -> middleClickFriends
				.setChecked(!middleClickFriends.isChecked()));
		
		new WurstOptionsButton(-154, 48,
			() -> "Analytics: " + (analytics.isEnabled() ? "ON" : "OFF"),
			"Allows us to measure the popularity of Wurst\n"
				+ "by sending anonymous usage statistics.",
			b -> analytics.setEnabled(!analytics.isEnabled()));
	}
	
	private void addManagerButtons()
	{
		XRayHack xRayHack = WurstClient.INSTANCE.getHax().xRayHack;
		
		new WurstOptionsButton(-50, 24, () -> "Keybinds",
			"Keybinds allow you to toggle any hack\n"
				+ "or command by simply pressing a\n" + "button.",
			b -> client.openScreen(new KeybindManagerScreen(this)));
		
		new WurstOptionsButton(-50, 48, () -> "X-Ray Blocks",
			"Manager for the blocks\n" + "that X-Ray will show.",
			b -> xRayHack.openBlockListEditor(this));
		
		new WurstOptionsButton(-50, 72, () -> "Zoom",
			"The Zoom Manager allows you to\n"
				+ "change the zoom key, how far it\n"
				+ "will zoom in and more.",
			b -> client.openScreen(new ZoomManagerScreen(this)));
	}
	
	private void addLinkButtons()
	{
		OperatingSystem os = Util.getOperatingSystem();
		
		new WurstOptionsButton(54, 24, () -> "Official Website",
			"WurstClient.net", b -> os.open("https://www.wurstclient.net/"));
		
		new WurstOptionsButton(54, 48, () -> "Twitter", "@Wurst_Imperium",
			b -> os.open("https://twitter.com/Wurst_Imperium"));
		
		new WurstOptionsButton(54, 72, () -> "Subreddit (NEW!)",
			"r/WurstClient",
			b -> os.open("https://www.reddit.com/r/WurstClient/"));
		
		new WurstOptionsButton(54, 96, () -> "Donate",
			"paypal.me/WurstImperium",
			b -> os.open("https://www.wurstclient.net/donate/"));
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(matrixStack);
		renderTitles(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		renderButtonTooltip(matrixStack, mouseX, mouseY);
	}
	
	private void renderTitles(MatrixStack matrixStack)
	{
		TextRenderer tr = client.textRenderer;
		int middleX = width / 2;
		int y1 = 40;
		int y2 = height / 4 + 24 - 28;
		
		drawCenteredString(matrixStack, tr, "Wurst Options", middleX, y1,
			0xffffff);
		
		drawCenteredString(matrixStack, tr, "Settings", middleX - 104, y2,
			0xcccccc);
		drawCenteredString(matrixStack, tr, "Managers", middleX, y2, 0xcccccc);
		drawCenteredString(matrixStack, tr, "Links", middleX + 104, y2,
			0xcccccc);
	}
	
	private void renderButtonTooltip(MatrixStack matrixStack, int mouseX,
		int mouseY)
	{
		for(AbstractButtonWidget button : buttons)
		{
			if(!button.isHovered() || !(button instanceof WurstOptionsButton))
				continue;
			
			WurstOptionsButton woButton = (WurstOptionsButton)button;
			if(woButton.tooltip.isEmpty())
				continue;
			
			renderTooltip(matrixStack, woButton.tooltip, mouseX, mouseY);
			break;
		}
	}
	
	private final class WurstOptionsButton extends ButtonWidget
	{
		private final Supplier<String> messageSupplier;
		private final List<Text> tooltip;
		
		public WurstOptionsButton(int xOffset, int yOffset,
			Supplier<String> messageSupplier, String tooltip,
			PressAction pressAction)
		{
			super(WurstOptionsScreen.this.width / 2 + xOffset,
				WurstOptionsScreen.this.height / 4 - 16 + yOffset, 100, 20,
				new LiteralText(messageSupplier.get()), pressAction);
			
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
