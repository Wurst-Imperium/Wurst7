/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.class_7479;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen
{
	@Shadow
	protected TextFieldWidget chatField;
	
	/**
	 * previewManager
	 */
	@Shadow
	private class_7479 field_39347;
	
	private ChatScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}
	
	@Inject(at = {@At("TAIL")}, method = {"init()V"})
	protected void onInit(CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().infiniChatHack.isEnabled())
			chatField.setMaxLength(Integer.MAX_VALUE);
	}
	
	/**
	 * Currently unnamed method that decides whether or not to send preview
	 * data.
	 * <p>
	 * Calls one of the following:
	 * <ul>
	 * <li>method_44035(String) - send preview data
	 * <li>method_44036() - don't send (also delete old preview data)
	 * </ul>
	 */
	@Inject(at = @At("HEAD"),
		method = "method_44059(Ljava/lang/String;)V",
		cancellable = true)
	private void method_44059(String message, CallbackInfo ci)
	{
		String normalized = method_44054(message);
		
		if(normalized.startsWith(".say "))
		{
			// send preview, but only for the part after ".say "
			field_39347.method_44035(normalized.substring(5));
			ci.cancel();
			
		}else if(normalized.startsWith("."))
		{
			// disable preview (as if typing a /command)
			field_39347.method_44036();
			ci.cancel();
		}
	}
	
	/**
	 * normalize()
	 */
	@Shadow
	public String method_44054(String string)
	{
		return null;
	}
}
