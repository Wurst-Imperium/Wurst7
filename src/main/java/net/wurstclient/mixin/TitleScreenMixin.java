/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.altmanager.screens.AltManagerScreen;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen
{
	private TitleScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}
	
	@Inject(at = {@At("RETURN")}, method = {"initWidgetsNormal(II)V"})
	private void onInitWidgetsNormal(int y, int spacingY, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		addButton(new ButtonWidget(width / 2 + 2, y + spacingY * 2, 98, 20,
			new LiteralText("Alt Manager"),
			b -> client.openScreen(new AltManagerScreen(this,
				WurstClient.INSTANCE.getAltManager()))));
		
		for(AbstractButtonWidget button : buttons)
		{
			if(!button.getMessage().getString()
				.equals(I18n.translate("menu.online")))
				continue;
			
			button.setWidth(98);
		}
	}
}
