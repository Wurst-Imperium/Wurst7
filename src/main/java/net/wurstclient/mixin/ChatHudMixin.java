/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.events.ChatInputListener.ChatInputEvent;

@Mixin(ChatHud.class)
public class ChatHudMixin extends DrawableHelper
{
	@Shadow
	private List<ChatHudLine<OrderedText>> visibleMessages;
	@Shadow
	private static Logger LOGGER;
	@Shadow
	private MinecraftClient client;
	
	@Inject(at = @At("HEAD"),
		method = "addMessage(Lnet/minecraft/text/Text;I)V",
		cancellable = true)
	private void onAddMessage(Text chatText, int chatLineId, CallbackInfo ci)
	{
		ChatInputEvent event = new ChatInputEvent(chatText, visibleMessages);
		
		WurstClient.INSTANCE.getEventManager().fire(event);
		if(event.isCancelled())
		{
			ci.cancel();
			return;
		}
		
		chatText = event.getComponent();
		shadow$addMessage(chatText, chatLineId, client.inGameHud.getTicks(),
			false);
		
		LOGGER.info("[CHAT] {}", chatText.getString().replaceAll("\r", "\\\\r")
			.replaceAll("\n", "\\\\n"));
		ci.cancel();
	}
	
	@Shadow
	private void shadow$addMessage(Text text, int messageId, int timestamp,
		boolean bl)
	{
		
	}
}
