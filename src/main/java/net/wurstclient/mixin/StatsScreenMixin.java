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

import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;

@Mixin(StatsScreen.class)
public class StatsScreenMixin
{
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;add(Lnet/minecraft/client/gui/widget/Widget;)Lnet/minecraft/client/gui/widget/Widget;",
		ordinal = 4), method = "createButtons()V")
	private <T extends Widget> T onCreateDoneButton(
		DirectionalLayoutWidget layout, T doneWidget, Operation<T> original)
	{
		if(!(doneWidget instanceof ButtonWidget doneButton))
			throw new IllegalStateException(
				"The done button in the statistics screen somehow isn't a button");
		
		if(WurstClient.INSTANCE.getOtfs().disableOtf.shouldHideEnableButton())
			return original.call(layout, doneButton);
		
		doneButton.setWidth(150);
		
		DirectionalLayoutWidget subLayout =
			layout.add(DirectionalLayoutWidget.horizontal()).spacing(5);
		subLayout.add(ButtonWidget.builder(getButtonText(), this::toggleWurst)
			.width(150).build());
		return original.call(subLayout, doneButton);
	}
	
	@Unique
	private void toggleWurst(ButtonWidget button)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		wurst.setEnabled(!wurst.isEnabled());
		button.setMessage(getButtonText());
	}
	
	@Unique
	private Text getButtonText()
	{
		WurstClient wurst = WurstClient.INSTANCE;
		String text = (wurst.isEnabled() ? "Disable" : "Enable") + " Wurst";
		return Text.literal(text);
	}
}
