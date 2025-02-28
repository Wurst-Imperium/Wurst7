/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ChatInputListener.ChatInputEvent;

@Mixin(ChatHud.class)
public class ChatHudMixin
{
	@Shadow
	@Final
	private List<ChatHudLine.Visible> visibleMessages;
	
	@Inject(at = @At("HEAD"),
		method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
		cancellable = true)
	private void onAddMessage(Text messageDontUse,
		@Nullable MessageSignatureData signature,
		@Nullable MessageIndicator indicatorDontUse, CallbackInfo ci,
		@Local(argsOnly = true) LocalRef<Text> message,
		@Local(argsOnly = true) LocalRef<MessageIndicator> indicator)
	{
		ChatInputEvent event =
			new ChatInputEvent(message.get(), visibleMessages);
		
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
