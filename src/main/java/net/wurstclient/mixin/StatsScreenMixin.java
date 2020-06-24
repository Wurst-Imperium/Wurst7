/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsListener;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;

@Mixin(StatsScreen.class)
public abstract class StatsScreenMixin extends Screen implements StatsListener
{
	private StatsScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}
	
	@Inject(at = {@At("TAIL")}, method = {"createButtons()V"})
	private void onCreateButtons(CallbackInfo ci)
	{
		ButtonWidget toggleWurstButton = new ButtonWidget(width / 2 - 152,
			height - 28, 150, 20, new LiteralText(""), this::toggleWurst);
		
		updateWurstButtonText(toggleWurstButton);
		addButton(toggleWurstButton);
		
		for(AbstractButtonWidget button : buttons)
		{
			if(!button.getMessage().getString()
				.equals(I18n.translate("gui.done")))
				continue;
			
			button.x = width / 2 + 2;
			button.setWidth(150);
		}
	}
	
	private void toggleWurst(ButtonWidget button)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		wurst.setEnabled(!wurst.isEnabled());
		
		updateWurstButtonText(button);
	}
	
	private void updateWurstButtonText(ButtonWidget button)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		String text = (wurst.isEnabled() ? "Disable" : "Enable") + " Wurst";
		button.setMessage(new LiteralText(text));
	}
}
