/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ChatInputListener.ChatInputEvent;

@Mixin(ChatComponent.class)
public class ChatHudMixin
{
	@Shadow
	@Final
	private List<GuiMessage.Line> trimmedMessages;
	
	@Inject(at = @At("HEAD"),
		method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
		cancellable = true)
	private void onAddMessage(Component messageDontUse,
		@Nullable MessageSignature signature,
		@Nullable GuiMessageTag indicatorDontUse, CallbackInfo ci,
		@Local(argsOnly = true) LocalRef<Component> message,
		@Local(argsOnly = true) LocalRef<GuiMessageTag> indicator)
	{
		ChatInputEvent event =
			new ChatInputEvent(message.get(), trimmedMessages);
		
		EventManager.fire(event);
		if(event.isCancelled())
		{
			ci.cancel();
			return;
		}
		
		message.set(event.getComponent());
		indicator.set(WurstClient.INSTANCE.getOtfs().noChatReportsOtf
			.modifyIndicator(message.get(), signature, indicator.get()));
	}
}
