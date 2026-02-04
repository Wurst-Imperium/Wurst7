/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.Component;
import net.wurstclient.WurstClient;
import net.wurstclient.options.WurstOptionsScreen;

@Mixin(StatsScreen.class)
public abstract class StatsScreenMixin extends Screen
{
	@Unique
	private Button wurstOptionsButton;
	
	public StatsScreenMixin(WurstClient wurst, Component title)
	{
		super(title);
	}
	
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
		ordinal = 4), method = "initButtons()V")
	private <T extends LayoutElement> T onAddDoneButton(
		LinearLayout footerLayout, T doneWidget, Operation<T> original,
		@Local(ordinal = 0) HeaderAndFooterLayout headerAndFooterLayout)
	{
		if(!(doneWidget instanceof Button doneButton))
			throw new IllegalStateException(
				"The done button in the statistics screen somehow isn't a button");
		
		WurstClient wurst = WurstClient.INSTANCE;
		if(wurst.getOtfs().disableOtf.shouldHideEnableButton())
			return original.call(footerLayout, doneButton);
		
		LinearLayout vLayout = LinearLayout.vertical().spacing(5);
		LinearLayout hLayout = LinearLayout.horizontal().spacing(5);
		
		Button toggleButton =
			Button.builder(getToggleButtonText(), this::toggleWurst).width(100)
				.build();
		hLayout.addChild(toggleButton);
		
		doneButton.setWidth(100);
		hLayout.addChild(doneButton);
		
		if(wurst.getOtfs().wurstOptionsOtf.isVisibleInStatistics())
		{
			headerAndFooterLayout.setFooterHeight(78);
			wurstOptionsButton = WurstClient.INSTANCE.getOtfs().wurstOptionsOtf
				.buttonBuilder(this::openWurstOptions).width(205).build();
			vLayout.addChild(wurstOptionsButton);
		}
		
		vLayout.addChild(hLayout);
		return original.call(footerLayout, vLayout);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		super.render(context, mouseX, mouseY, partialTicks);
		WurstClient.INSTANCE.getOtfs().wurstOptionsOtf
			.drawWurstLogoOnButton(context, wurstOptionsButton);
	}
	
	@Unique
	private void openWurstOptions(Button button)
	{
		minecraft.setScreen(new WurstOptionsScreen(this));
	}
	
	@Unique
	private void toggleWurst(Button toggleButton)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		wurst.setEnabled(!wurst.isEnabled());
		toggleButton.setMessage(getToggleButtonText());
		if(wurstOptionsButton != null)
			wurstOptionsButton.active = wurst.isEnabled();
	}
	
	@Unique
	private Component getToggleButtonText()
	{
		WurstClient wurst = WurstClient.INSTANCE;
		String text = (wurst.isEnabled() ? "Disable" : "Enable") + " Wurst";
		return Component.literal(text);
	}
}
