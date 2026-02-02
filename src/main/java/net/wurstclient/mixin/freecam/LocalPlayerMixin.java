/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.freecam;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.FreecamHack;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer
{
	@Shadow
	public ClientInput input;
	
	@Unique
	private ClientInput realInput;
	
	private LocalPlayerMixin(WurstClient wurst, ClientLevel world,
		GameProfile profile)
	{
		super(world, profile);
	}
	
	@Inject(at = @At("HEAD"), method = "isShiftKeyDown()Z", cancellable = true)
	private void onIsShiftKeyDown(CallbackInfoReturnable<Boolean> cir)
	{
		if(WurstClient.INSTANCE.getHax().freecamHack.isMovingCamera())
			cir.setReturnValue(false);
	}
	
	@Inject(at = @At("HEAD"), method = "aiStep()V")
	private void onAiStepHead(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getHax().freecamHack.isMovingCamera())
			return;
		
		realInput = input;
		input.tick();
		input = new ClientInput();
	}
	
	@Inject(at = @At("RETURN"), method = "aiStep()V")
	private void onAiStepReturn(CallbackInfo ci)
	{
		if(realInput == null)
			return;
		
		input = realInput;
		realInput = null;
	}
	
	@Override
	public void turn(double deltaYaw, double deltaPitch)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam.isMovingCamera())
		{
			freecam.turn(deltaYaw, deltaPitch);
			return;
		}
		
		super.turn(deltaYaw, deltaPitch);
	}
}
