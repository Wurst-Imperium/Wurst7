/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.wurstclient.WurstClient;
import net.wurstclient.other_features.ZoomOtf;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

public class ZoomManagerScreen extends Screen implements PressAKeyCallback
{
	private Screen prevScreen;
	private Button scrollButton;
	
	public ZoomManagerScreen(Screen par1GuiScreen)
	{
		super(Component.literal(""));
		prevScreen = par1GuiScreen;
	}
	
	@Override
	public void init()
	{
		WurstClient wurst = WurstClient.INSTANCE;
		ZoomOtf zoom = wurst.getOtfs().zoomOtf;
		SliderSetting level = zoom.getLevelSetting();
		CheckboxSetting scroll = zoom.getScrollSetting();
		
		addRenderableWidget(Button
			.builder(Component.literal("Back"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 - 100, height / 4 + 144 - 16, 200, 20).build());
		
		addRenderableWidget(Button
			.builder(
				Component.literal("Zoom Key: ")
					.append(zoom.getTranslatedKeybindName()),
				b -> minecraft.setScreen(new PressAKeyScreen(this)))
			.bounds(width / 2 - 79, height / 4 + 24 - 16, 158, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("More"), b -> level.increaseValue())
			.bounds(width / 2 - 79, height / 4 + 72 - 16, 50, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Less"), b -> level.decreaseValue())
			.bounds(width / 2 - 25, height / 4 + 72 - 16, 50, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Default"),
				b -> level.setValue(level.getDefaultValue()))
			.bounds(width / 2 + 29, height / 4 + 72 - 16, 50, 20).build());
		
		addRenderableWidget(scrollButton =
			Button
				.builder(
					Component.literal(
						"Use Mouse Wheel: " + onOrOff(scroll.isChecked())),
					b -> toggleScroll())
				.bounds(width / 2 - 79, height / 4 + 96 - 16, 158, 20).build());
	}
	
	private void toggleScroll()
	{
		ZoomOtf zoom = WurstClient.INSTANCE.getOtfs().zoomOtf;
		CheckboxSetting scroll = zoom.getScrollSetting();
		
		scroll.setChecked(!scroll.isChecked());
		scrollButton.setMessage(Component
			.literal("Use Mouse Wheel: " + onOrOff(scroll.isChecked())));
	}
	
	private String onOrOff(boolean on)
	{
		return on ? "ON" : "OFF";
	}
	
	@Override
	public void onClose()
	{
		minecraft.setScreen(prevScreen);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		ZoomOtf zoom = WurstClient.INSTANCE.getOtfs().zoomOtf;
		SliderSetting level = zoom.getLevelSetting();
		
		renderBackground(context, mouseX, mouseY, partialTicks);
		context.drawCenteredString(font, "Zoom Manager", width / 2, 40,
			0xffffff);
		context.drawString(font, "Zoom Level: " + level.getValueString(),
			width / 2 - 75, height / 4 + 44, 0xcccccc);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void setKey(String key)
	{
		WurstClient.INSTANCE.getOtfs().zoomOtf.setBoundKey(key);
		// Button text updates automatically because going back to this screen
		// calls init(). Might be different in older MC versions.
	}
}
