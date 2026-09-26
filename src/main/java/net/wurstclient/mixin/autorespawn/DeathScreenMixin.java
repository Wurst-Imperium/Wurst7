/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.autorespawn;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoRespawnHack;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen
{
	private DeathScreenMixin(WurstClient wurst, Component title)
	{
		super(title);
	}
	
	@Inject(method = "tick()V", at = @At("TAIL"))
	private void onTick(CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().autoRespawnHack.isEnabled())
			respawn();
	}
	
	@Inject(method = "init()V", at = @At("TAIL"))
	private void onInit(CallbackInfo ci)
	{
		AutoRespawnHack autoRespawn =
			WurstClient.INSTANCE.getHax().autoRespawnHack;
		
		if(!autoRespawn.shouldShowButton())
			return;
		
		int backButtonX = width / 2 - 100;
		int backButtonY = height / 4;
		
		addRenderableWidget(
			Button.builder(Component.literal("AutoRespawn: OFF"), b -> {
				autoRespawn.setEnabled(true);
				respawn();
			}).bounds(backButtonX, backButtonY + 48, 200, 20).build());
	}
	
	@Unique
	private void respawn()
	{
		minecraft.player.respawn();
		minecraft.gui.setScreen(null);
	}
}
