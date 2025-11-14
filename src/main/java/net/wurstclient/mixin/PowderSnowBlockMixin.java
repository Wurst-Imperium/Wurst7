/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.wurstclient.WurstClient;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin extends Block implements BucketPickup
{
	private PowderSnowBlockMixin(WurstClient wurst, Properties settings)
	{
		super(settings);
	}
	
	@Inject(at = @At("HEAD"),
		method = "canEntityWalkOnPowderSnow(Lnet/minecraft/world/entity/Entity;)Z",
		cancellable = true)
	private static void onCanWalkOnPowderSnow(Entity entity,
		CallbackInfoReturnable<Boolean> cir)
	{
		if(!WurstClient.INSTANCE.getHax().snowShoeHack.isEnabled())
			return;
		
		if(entity == WurstClient.MC.player)
			cir.setReturnValue(true);
	}
}
