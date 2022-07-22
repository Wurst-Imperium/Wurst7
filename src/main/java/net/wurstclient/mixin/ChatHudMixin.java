/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ChatInputListener.ChatInputEvent;

@Mixin(ChatHud.class)
public class ChatHudMixin extends DrawableHelper
{
	@Shadow
	private List<ChatHudLine.Visible> visibleMessages;
	@Shadow
	private static Logger LOGGER;
	@Shadow
	private MinecraftClient client;
	
	@Inject(at = @At("HEAD"),
		method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
		cancellable = true)
	private void onAddMessage(Text message,
		@Nullable MessageIndicator indicator, CallbackInfo ci)
	{
		ChatInputEvent event = new ChatInputEvent(message, visibleMessages);
		
		EventManager.fire(event);
		if(event.isCancelled())
		{
			ci.cancel();
			return;
		}
		
		message = event.getComponent();
		shadow$addMessage(message, client.inGameHud.getTicks(), indicator,
			false);
		
		String messageString =
			message.getString().replace("\r", "\\r").replace("\n", "\\n");
		String indicatorString =
			Util.map(indicator, MessageIndicator::loggedName);
		
		if(indicatorString != null)
			LOGGER.info("[{}] [CHAT] {}", indicatorString, messageString);
		else
			LOGGER.info("[CHAT] {}", messageString);
		
		ci.cancel();
	}
	
	@Shadow
	private void shadow$addMessage(Text message, int messageId,
		@Nullable MessageIndicator indicator, boolean refresh)
	{
		
	}
}
