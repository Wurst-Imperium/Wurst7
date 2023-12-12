/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;
import net.wurstclient.WurstClient;
import net.wurstclient.analytics.WurstAnalytics;
import net.wurstclient.commands.FriendsCmd;
import net.wurstclient.hacks.XRayHack;
import net.wurstclient.other_features.VanillaSpoofOtf;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;

public class WurstOptionsScreen extends Screen
{
	private Screen prevScreen;
	
	public WurstOptionsScreen(Screen prevScreen)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Back"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 - 100, height / 4 + 144 - 16, 200, 20)
			.build());
		
		addSettingButtons();
		addManagerButtons();
		addLinkButtons();
	}
	
	private void addSettingButtons()
	{
		WurstClient wurst = WurstClient.INSTANCE;
		FriendsCmd friendsCmd = wurst.getCmds().friendsCmd;
		CheckboxSetting middleClickFriends = friendsCmd.getMiddleClickFriends();
		WurstAnalytics analytics = wurst.getAnalytics();
		VanillaSpoofOtf vanillaSpoofOtf = wurst.getOtfs().vanillaSpoofOtf;
		CheckboxSetting forceEnglish =
			wurst.getOtfs().translationsOtf.getForceEnglish();
		
		new WurstOptionsButton(-154, 24,
			() -> "Click Friends: "
				+ (middleClickFriends.isChecked() ? "ON" : "OFF"),
			middleClickFriends.getWrappedDescription(200),
			b -> middleClickFriends
				.setChecked(!middleClickFriends.isChecked()));
		
		new WurstOptionsButton(-154, 48,
			() -> "Count Users: " + (analytics.isEnabled() ? "ON" : "OFF"),
			"Counts how many people are using Wurst and which versions are the"
				+ " most popular. We use this data to decide when to stop"
				+ " supporting old Minecraft versions.\n\n"
				+ "We use a random ID to tell users apart so that this data can"
				+ " never be linked to your Minecraft account. The random ID is"
				+ " changed every 3 days to make extra sure that you remain"
				+ " anonymous.",
			b -> analytics.setEnabled(!analytics.isEnabled()));
		
		new WurstOptionsButton(-154, 72,
			() -> "Spoof Vanilla: "
				+ (vanillaSpoofOtf.isEnabled() ? "ON" : "OFF"),
			vanillaSpoofOtf.getDescription(),
			b -> vanillaSpoofOtf.doPrimaryAction());
		
		new WurstOptionsButton(-154, 96,
			() -> "Translations: " + (!forceEnglish.isChecked() ? "ON" : "OFF"),
			"Allows text in Wurst to be displayed in other languages than"
				+ " English. It will use the same language that Minecraft is"
				+ " set to.\n\n" + "This is an experimental feature!",
			b -> forceEnglish.setChecked(!forceEnglish.isChecked()));
	}
	
	private void addManagerButtons()
	{
		XRayHack xRayHack = WurstClient.INSTANCE.getHax().xRayHack;
		
		new WurstOptionsButton(-50, 24, () -> "Keybinds",
			"Keybinds allow you to toggle any hack or command by simply"
				+ " pressing a button.",
			b -> client.setScreen(new KeybindManagerScreen(this)));
		
		new WurstOptionsButton(-50, 48, () -> "X-Ray Blocks",
			"Manager for the blocks that X-Ray will show.",
			b -> xRayHack.openBlockListEditor(this));
		
		new WurstOptionsButton(-50, 72, () -> "Zoom",
			"The Zoom Manager allows you to change the zoom key and how far it"
				+ " will zoom in.",
			b -> client.setScreen(new ZoomManagerScreen(this)));
	}
	
	private void addLinkButtons()
	{
		OperatingSystem os = Util.getOperatingSystem();
		
		new WurstOptionsButton(54, 24, () -> "Official Website",
			"§n§lWurstClient.net", b -> os.open(
				"https://www.wurstclient.net/?utm_source=Wurst+Client&utm_medium=Wurst+Options&utm_content=Official+Website"));
		
		new WurstOptionsButton(54, 48, () -> "Wurst Wiki", "§n§lWurst.Wiki\n"
			+ "We are looking for volunteers to help us expand"
			+ " the wiki and keep it up to date with the latest Wurst updates.",
			b -> os.open(
				"https://wurst.wiki/?utm_source=Wurst+Client&utm_medium=Wurst+Options&utm_content=Wurst+Wiki"));
		
		new WurstOptionsButton(54, 72, () -> "WurstForum", "§n§lWurstForum.net",
			b -> os.open(
				"https://wurstforum.net/?utm_source=Wurst+Client&utm_medium=Wurst+Options&utm_content=WurstForum"));
		
		new WurstOptionsButton(54, 96, () -> "Twitter", "@Wurst_Imperium",
			b -> os.open("https://www.wurstclient.net/twitter/"));
		
		new WurstOptionsButton(54, 120, () -> "Donate",
			"§n§lWurstClient.net/donate\n"
				+ "Donate now to help me keep the Wurst Client alive and free"
				+ " to use for everyone.\n\n"
				+ "Every bit helps and is much appreciated! You can also get a"
				+ " few cool perks in return.",
			b -> os.open(
				"https://www.wurstclient.net/donate/?utm_source=Wurst+Client&utm_medium=Wurst+Options&utm_content=Donate"));
	}
	
	@Override
	public void close()
	{
		client.setScreen(prevScreen);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(context, mouseX, mouseY, partialTicks);
		renderTitles(context);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		renderButtonTooltip(context, mouseX, mouseY);
	}
	
	private void renderTitles(DrawContext context)
	{
		TextRenderer tr = client.textRenderer;
		int middleX = width / 2;
		int y1 = 40;
		int y2 = height / 4 + 24 - 28;
		
		context.drawCenteredTextWithShadow(tr, "Wurst Options", middleX, y1,
			0xffffff);
		
		context.drawCenteredTextWithShadow(tr, "Settings", middleX - 104, y2,
			0xcccccc);
		context.drawCenteredTextWithShadow(tr, "Managers", middleX, y2,
			0xcccccc);
		context.drawCenteredTextWithShadow(tr, "Links", middleX + 104, y2,
			0xcccccc);
	}
	
	private void renderButtonTooltip(DrawContext context, int mouseX,
		int mouseY)
	{
		for(ClickableWidget button : Screens.getButtons(this))
		{
			if(!button.isSelected() || !(button instanceof WurstOptionsButton))
				continue;
			
			WurstOptionsButton woButton = (WurstOptionsButton)button;
			
			if(woButton.tooltip.isEmpty())
				continue;
			
			context.drawTooltip(textRenderer, woButton.tooltip, mouseX, mouseY);
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
				Text.literal(messageSupplier.get()), pressAction,
				ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
			
			this.messageSupplier = messageSupplier;
			
			if(tooltip.isEmpty())
				this.tooltip = Arrays.asList();
			else
			{
				String[] lines = ChatUtils.wrapText(tooltip, 200).split("\n");
				
				Text[] lines2 = new Text[lines.length];
				for(int i = 0; i < lines.length; i++)
					lines2[i] = Text.literal(lines[i]);
				
				this.tooltip = Arrays.asList(lines2);
			}
			
			addDrawableChild(this);
		}
		
		@Override
		public void onPress()
		{
			super.onPress();
			setMessage(Text.literal(messageSupplier.get()));
		}
	}
}
