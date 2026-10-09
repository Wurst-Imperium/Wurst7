/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.noslowdown;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.HackList;

@Mixin(Entity.class)
public abstract class EntityMixin
{
	/**
	 * Removes block slowdown for the local player while NoSlowdown is enabled.
	 */
	@Inject(method = "getBlockSpeedFactor()F",
		at = @At("RETURN"),
		cancellable = true)
	private void onGetBlockSpeedFactor(CallbackInfoReturnable<Float> cir)
	{
		if((Object)this != WurstClient.MC.player)
			return;
		
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.noSlowdownHack.isEnabled())
			return;
		
		if(cir.getReturnValueF() < 1)
			cir.setReturnValue(1F);
	}
}
