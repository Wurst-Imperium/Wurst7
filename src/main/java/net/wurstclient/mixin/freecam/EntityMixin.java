/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.freecam;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.FreecamHack;

@Mixin(Entity.class)
public class EntityMixin
{
	/**
	 * Makes the modified raytrace work outside of player's reach.
	 */
	@Inject(at = @At("HEAD"),
		method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;",
		cancellable = true)
	private void onGetEyePosition(float partialTicks,
		CallbackInfoReturnable<Vec3> cir)
	{
		if(!((Entity)(Object)this instanceof LocalPlayer))
			return;
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam.isMovingCamera())
			cir.setReturnValue(freecam.getCamPos(partialTicks));
	}
}
