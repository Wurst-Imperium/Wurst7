/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.DeathListener.DeathEvent;
import net.wurstclient.hacks.AutoRespawnHack;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen
{
	private DeathScreenMixin(WurstClient wurst, Text title)
	{
		super(title);
	}
	
	@Inject(at = @At("TAIL"), method = "tick()V")
	private void onTick(CallbackInfo ci)
	{
		EventManager.fire(DeathEvent.INSTANCE);
	}
	
	@Inject(at = @At("TAIL"), method = "init()V")
	private void onInit(CallbackInfo ci)
	{
		AutoRespawnHack autoRespawn =
			WurstClient.INSTANCE.getHax().autoRespawnHack;
		
		if(!autoRespawn.shouldShowButton())
			return;
		
		int backButtonX = width / 2 - 100;
		int backButtonY = height / 4;
		
		addDrawableChild(
			ButtonWidget.builder(Text.literal("AutoRespawn: OFF"), b -> {
				autoRespawn.setEnabled(true);
				autoRespawn.onDeath();
			}).dimensions(backButtonX, backButtonY + 48, 200, 20).build());
	}
}
