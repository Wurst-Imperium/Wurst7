/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
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

import com.mojang.authlib.GameProfile;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.wurstclient.WurstClient;
import net.wurstclient.events.ChatOutputListener.ChatOutputEvent;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayerEntity
{
	public ClientPlayerEntityMixin(WurstClient wurst, ClientWorld clientWorld_1,
		GameProfile gameProfile_1)
	{
		super(clientWorld_1, gameProfile_1);
	}
	
	@Inject(at = @At("HEAD"),
		method = "sendChatMessage(Ljava/lang/String;)V",
		cancellable = true)
	private void onSendChatMessage(String message, CallbackInfo ci)
	{
		ChatOutputEvent event = new ChatOutputEvent(message, false);
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
}
