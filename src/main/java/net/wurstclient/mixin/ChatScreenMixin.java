/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ChatOutputListener.ChatOutputEvent;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen
{
	@Shadow
	protected TextFieldWidget chatField;
	
	private ChatScreenMixin(WurstClient wurst, Text title)
	{
		super(title);
	}
	
	@Inject(at = @At("TAIL"), method = "init()V")
	protected void onInit(CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().infiniChatHack.isEnabled())
			chatField.setMaxLength(Integer.MAX_VALUE);
	}
	
	@Inject(at = @At("HEAD"),
		method = "sendMessage(Ljava/lang/String;Z)V",
		cancellable = true)
	public void onSendMessage(String message, boolean addToHistory,
		CallbackInfo ci)
	{
		// Ignore empty messages just like vanilla
		if((message = normalize(message)).isEmpty())
			return;
		
		// Create and fire the chat output event
		ChatOutputEvent event = new ChatOutputEvent(message);
		EventManager.fire(event);
		
		// If the event hasn't been modified or cancelled,
		// let the vanilla method handle the message
		boolean cancelled = event.isCancelled();
		if(!cancelled && !event.isModified())
			return;
		
		// Otherwise, cancel the vanilla method and handle the message here
		ci.cancel();
		
		// Add the message to history, even if it was cancelled
		// Otherwise the up/down arrows won't work correctly
		String newMessage = event.getMessage();
		if(addToHistory)
			client.inGameHud.getChatHud().addToMessageHistory(newMessage);
		
		// If the event isn't cancelled, send the modified message
		if(!cancelled)
			if(newMessage.startsWith("/"))
				client.player.networkHandler
					.sendChatCommand(newMessage.substring(1));
			else
				client.player.networkHandler.sendChatMessage(newMessage);
	}
	
	@Shadow
	public abstract String normalize(String chatText);
}
