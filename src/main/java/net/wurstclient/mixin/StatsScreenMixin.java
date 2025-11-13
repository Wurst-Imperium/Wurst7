/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.Component;
import net.wurstclient.WurstClient;

@Mixin(StatsScreen.class)
public class StatsScreenMixin
{
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
		ordinal = 0), method = "init()V")
	private <T extends LayoutElement> T onAddFooter(
		HeaderAndFooterLayout layout, T doneWidget, Operation<T> original)
	{
		if(!(doneWidget instanceof Button doneButton))
			throw new IllegalStateException(
				"The done button in the statistics screen somehow isn't a button");
		
		if(WurstClient.INSTANCE.getOtfs().disableOtf.shouldHideEnableButton())
			return original.call(layout, doneButton);
		
		doneButton.setWidth(150);
		
		LinearLayout subLayout = LinearLayout.horizontal().spacing(5);
		subLayout.addChild(Button.builder(getButtonText(), this::toggleWurst)
			.width(150).build());
		subLayout.addChild(doneButton);
		return original.call(layout, subLayout);
	}
	
	@Unique
	private void toggleWurst(Button button)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		wurst.setEnabled(!wurst.isEnabled());
		button.setMessage(getButtonText());
	}
	
	@Unique
	private Component getButtonText()
	{
		WurstClient wurst = WurstClient.INSTANCE;
		String text = (wurst.isEnabled() ? "Disable" : "Enable") + " Wurst";
		return Component.literal(text);
	}
}
