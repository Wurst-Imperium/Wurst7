/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.altmanager.screens.AltManagerScreen;
import net.wurstclient.mixinterface.IScreen;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen
{
	private ClickableWidget realmsButton = null;
	private ButtonWidget altsButton;
	
	private TitleScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}
	
	@Inject(at = {@At("RETURN")}, method = {"init()V"})
	private void onInitWidgetsNormal(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		for(Drawable d : ((IScreen)this).getButtons())
		{
			if(!(d instanceof ClickableWidget))
				continue;
			
			ClickableWidget button = (ClickableWidget)d;
			if(!button.getMessage().getString()
				.equals(I18n.translate("menu.online")))
				continue;
			
			realmsButton = button;
			break;
		}
		
		if(realmsButton == null)
			throw new IllegalStateException("Couldn't find realms button!");
		
		// make Realms button smaller
		realmsButton.setWidth(98);
		
		// add AltManager button
		addDrawableChild(altsButton = new ButtonWidget(width / 2 + 2,
			realmsButton.y, 98, 20, Text.literal("Alt Manager"),
			b -> client.setScreen(new AltManagerScreen(this,
				WurstClient.INSTANCE.getAltManager()))));
	}
	
	@Inject(at = {@At("RETURN")}, method = {"tick()V"})
	private void onTick(CallbackInfo ci)
	{
		if(realmsButton == null || altsButton == null)
			return;
			
		// adjust AltManager button if Realms button has been moved
		// happens when ModMenu is installed
		altsButton.y = realmsButton.y;
	}
	
	/**
	 * Stops the multiplayer button being grayed out if the user's Microsoft
	 * account is parental-control'd or banned from online play.
	 */
	@Inject(at = @At("HEAD"),
		method = "getMultiplayerDisabledText()Lnet/minecraft/text/Text;",
		cancellable = true)
	private void onGetMultiplayerDisabledText(CallbackInfoReturnable<Text> cir)
	{
		cir.setReturnValue(null);
	}
}
