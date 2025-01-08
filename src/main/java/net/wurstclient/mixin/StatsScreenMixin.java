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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;

@Mixin(StatsScreen.class)
public abstract class StatsScreenMixin extends Screen
{
	@Unique
	private ButtonWidget toggleWurstButton;
	
	private StatsScreenMixin(WurstClient wurst, Text title)
	{
		super(title);
	}
	
	/**
	 * Adds the hidden "Enable/Disable Wurst" button on the Statistics screen.
	 */
	@Inject(at = @At("TAIL"), method = "createButtons()V")
	private void onCreateButtons(CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getOtfs().disableOtf.shouldHideEnableButton())
			return;
		
		toggleWurstButton = ButtonWidget
			.builder(Text.literal(""), this::toggleWurst).width(150).build();
		
		ClickableWidget doneButton = getDoneButton();
		doneButton.setX(width / 2 + 2);
		doneButton.setWidth(150);
		
		toggleWurstButton.setPosition(width / 2 - 152, doneButton.getY());
		
		updateWurstButtonText(toggleWurstButton);
		addDrawableChild(toggleWurstButton);
	}
	
	@Unique
	private ClickableWidget getDoneButton()
	{
		for(ClickableWidget button : Screens.getButtons(this))
			if(button.getMessage().getString()
				.equals(I18n.translate("gui.done")))
				return button;
			
		throw new IllegalStateException(
			"Can't find the done button on the statistics screen.");
	}
	
	@Unique
	private void toggleWurst(ButtonWidget button)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		wurst.setEnabled(!wurst.isEnabled());
		
		updateWurstButtonText(button);
	}
	
	@Unique
	private void updateWurstButtonText(ButtonWidget button)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		String text = (wurst.isEnabled() ? "Disable" : "Enable") + " Wurst";
		button.setMessage(Text.literal(text));
	}
}
