/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import net.wurstclient.WurstClient;
import net.wurstclient.other_features.ZoomOtf;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

public class ZoomManagerScreen extends Screen implements PressAKeyCallback
{
	private Screen prevScreen;
	private ButtonWidget keyButton;
	private ButtonWidget scrollButton;
	
	public ZoomManagerScreen(Screen par1GuiScreen)
	{
		super(new LiteralText(""));
		prevScreen = par1GuiScreen;
	}
	
	@Override
	public void init()
	{
		ZoomOtf zoom = WurstClient.INSTANCE.getOtfs().zoomOtf;
		SliderSetting level = zoom.getLevelSetting();
		CheckboxSetting scroll = zoom.getScrollSetting();
		String zoomKeyName = WurstClient.INSTANCE.getZoomKey().getBoundKey()
			.getName().replace("key.keyboard.", "");
		
		addButton(new ButtonWidget(width / 2 - 100, height / 4 + 144 - 16, 200,
			20, "Back", b -> minecraft.openScreen(prevScreen)));
		
		addButton(keyButton = new ButtonWidget(width / 2 - 79,
			height / 4 + 24 - 16, 158, 20, "Zoom Key: " + zoomKeyName,
			b -> minecraft.openScreen(new PressAKeyScreen(this))));
		
		addButton(new ButtonWidget(width / 2 - 79, height / 4 + 72 - 16, 50, 20,
			"More", b -> level.increaseValue()));
		
		addButton(new ButtonWidget(width / 2 - 25, height / 4 + 72 - 16, 50, 20,
			"Less", b -> level.decreaseValue()));
		
		addButton(new ButtonWidget(width / 2 + 29, height / 4 + 72 - 16, 50, 20,
			"Default", b -> level.setValue(level.getDefaultValue())));
		
		addButton(scrollButton =
			new ButtonWidget(width / 2 - 79, height / 4 + 96 - 16, 158, 20,
				"Use Mouse Wheel: " + onOrOff(scroll.isChecked()),
				b -> toggleScroll()));
	}
	
	private void toggleScroll()
	{
		ZoomOtf zoom = WurstClient.INSTANCE.getOtfs().zoomOtf;
		CheckboxSetting scroll = zoom.getScrollSetting();
		
		scroll.setChecked(!scroll.isChecked());
		scrollButton
			.setMessage("Use Mouse Wheel: " + onOrOff(scroll.isChecked()));
	}
	
	private String onOrOff(boolean on)
	{
		return on ? "ON" : "OFF";
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		ZoomOtf zoom = WurstClient.INSTANCE.getOtfs().zoomOtf;
		SliderSetting level = zoom.getLevelSetting();
		
		renderBackground();
		drawCenteredString(font, "Zoom Manager", width / 2, 40, 0xffffff);
		drawString(font, "Zoom Level: " + level.getValueString(),
			width / 2 - 75, height / 4 + 44, 0xcccccc);
		
		super.render(mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void setKey(String key)
	{
		WurstClient.INSTANCE.getZoomKey().setKeyCode(InputUtil.fromName(key));
		minecraft.options.write();
		KeyBinding.updateKeysByCode();
		keyButton.setMessage("Zoom Key: " + key);
	}
}
