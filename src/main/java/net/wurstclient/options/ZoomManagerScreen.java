/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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
import net.minecraft.client.util.math.MatrixStack;
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
		String zoomKeyName = WurstClient.INSTANCE.getZoomKey()
			.getBoundKeyTranslationKey().replace("key.keyboard.", "");
		
		addButton(new ButtonWidget(width / 2 - 100, height / 4 + 144 - 16, 200,
			20, new LiteralText("Back"), b -> client.openScreen(prevScreen)));
		
		addButton(
			keyButton = new ButtonWidget(width / 2 - 79, height / 4 + 24 - 16,
				158, 20, new LiteralText("Zoom Key: " + zoomKeyName),
				b -> client.openScreen(new PressAKeyScreen(this))));
		
		addButton(new ButtonWidget(width / 2 - 79, height / 4 + 72 - 16, 50, 20,
			new LiteralText("More"), b -> level.increaseValue()));
		
		addButton(new ButtonWidget(width / 2 - 25, height / 4 + 72 - 16, 50, 20,
			new LiteralText("Less"), b -> level.decreaseValue()));
		
		addButton(new ButtonWidget(width / 2 + 29, height / 4 + 72 - 16, 50, 20,
			new LiteralText("Default"),
			b -> level.setValue(level.getDefaultValue())));
		
		addButton(scrollButton =
			new ButtonWidget(width / 2 - 79, height / 4 + 96 - 16, 158, 20,
				new LiteralText(
					"Use Mouse Wheel: " + onOrOff(scroll.isChecked())),
				b -> toggleScroll()));
	}
	
	private void toggleScroll()
	{
		ZoomOtf zoom = WurstClient.INSTANCE.getOtfs().zoomOtf;
		CheckboxSetting scroll = zoom.getScrollSetting();
		
		scroll.setChecked(!scroll.isChecked());
		scrollButton.setMessage(
			new LiteralText("Use Mouse Wheel: " + onOrOff(scroll.isChecked())));
	}
	
	private String onOrOff(boolean on)
	{
		return on ? "ON" : "OFF";
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		ZoomOtf zoom = WurstClient.INSTANCE.getOtfs().zoomOtf;
		SliderSetting level = zoom.getLevelSetting();
		
		renderBackground(matrixStack);
		drawCenteredString(matrixStack, textRenderer, "Zoom Manager", width / 2,
			40, 0xffffff);
		drawStringWithShadow(matrixStack, textRenderer,
			"Zoom Level: " + level.getValueString(), width / 2 - 75,
			height / 4 + 44, 0xcccccc);
		
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void setKey(String key)
	{
		WurstClient.INSTANCE.getZoomKey()
			.setBoundKey(InputUtil.fromTranslationKey(key));
		client.options.write();
		KeyBinding.updateKeysByCode();
		keyButton.setMessage(new LiteralText("Zoom Key: " + key));
	}
}
