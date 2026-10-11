/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.infinichat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.gui.screens.ChatScreen;
import net.wurstclient.WurstClient;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin
{
	/**
	 * Removes the chat input's 256-character limit while InfiniChat is enabled.
	 */
	@ModifyArg(method = "init()V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/EditBox;setMaxLength(I)V"),
		index = 0)
	private int modifyChatMaxLength(int original)
	{
		return WurstClient.INSTANCE.getHax().infiniChatHack.isEnabled()
			? Integer.MAX_VALUE : original;
	}
	
	/**
	 * Prevents chat commands from being truncated on submission while
	 * InfiniChat is enabled.
	 */
	@WrapOperation(
		method = "normalizeChatMessage(Ljava/lang/String;)Ljava/lang/String;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/StringUtil;trimChatMessage(Ljava/lang/String;)Ljava/lang/String;"))
	private String wrapTrimChatMessage(String message,
		Operation<String> original)
	{
		// Keep the limit on normal chat messages so you don't get kicked
		if(WurstClient.INSTANCE.getHax().infiniChatHack.isEnabled()
			&& (message.startsWith("/") || message.startsWith(".")))
			return message;
		
		return original.call(message);
	}
}
