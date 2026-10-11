/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ChatOutputListener.ChatOutputEvent;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen
{
	private ChatScreenMixin(WurstClient wurst, Component title)
	{
		super(title);
	}
	
	@Inject(method = "handleChatInput(Ljava/lang/String;Z)V",
		at = @At(value = "INVOKE",
			target = "Ljava/lang/String;isEmpty()Z",
			shift = At.Shift.AFTER),
		cancellable = true)
	private void onHandleChatInput(String messageDontUse, boolean addToHistory,
		CallbackInfo ci, @Local(argsOnly = true) LocalRef<String> message)
	{
		if(message.get().isEmpty())
			return;
		
		ChatOutputEvent event = new ChatOutputEvent(message.get());
		EventManager.fire(event);
		
		if(event.isCancelled())
		{
			// Preserve history even for cancelled messages
			// Otherwise the up/down arrows won't work correctly
			if(addToHistory)
				minecraft.gui.hud.getChat().addRecentChat(event.getMessage());
			
			ci.cancel();
			return;
		}
		
		message.set(event.getMessage());
	}
}
