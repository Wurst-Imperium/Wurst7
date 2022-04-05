/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoSignHack;

@Mixin(SignEditScreen.class)
public abstract class SignEditScreenMixin extends Screen
{
	@Shadow
	@Final
	private String[] text;
	
	private SignEditScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}
	
	@Inject(at = {@At("HEAD")}, method = {"init()V"})
	private void onInit(CallbackInfo ci)
	{
		AutoSignHack autoSignHack = WurstClient.INSTANCE.getHax().autoSignHack;
		
		String[] autoSignText = autoSignHack.getSignText();
		if(autoSignText == null)
			return;
		
		text = autoSignText;
		finishEditing();
	}
	
	@Inject(at = {@At("HEAD")}, method = {"finishEditing()V"})
	private void onFinishEditing(CallbackInfo ci)
	{
		WurstClient.INSTANCE.getHax().autoSignHack.setSignText(text);
	}
	
	@Shadow
	private void finishEditing()
	{
		
	}
}
