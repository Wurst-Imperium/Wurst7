/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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

import net.minecraft.block.BlockState;
import net.minecraft.block.SlimeBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NoSlowdownHack;

@Mixin(SlimeBlock.class)
public abstract class SlimeBlockMixin extends TransparentBlock
{
	private SlimeBlockMixin(WurstClient wurst, Settings block$Settings_1) 
	{
		super(block$Settings_1);	
	}
	
	// Cancels bouncing, which in turn also removes slowdown
	@Inject(at = {@At("HEAD")},
			method = {
				"bounce(Lnet/minecraft/entity/Entity;)V"},
			cancellable = true)
	private void onBounce(Entity entity, CallbackInfo ci)
	{
		NoSlowdownHack hax = WurstClient.INSTANCE.getHax().noSlowdownHack;
		if(hax.isEnabled() && hax.getSlimeUtil())
		ci.cancel();
	}
	
	// Smooth transition from normal block to slime block
	@Inject(at = { @At("HEAD") },
			method = {
				"onSteppedOn(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/Entity;)V" },
			cancellable = true)
	private void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) 
	{
		NoSlowdownHack hax = WurstClient.INSTANCE.getHax().noSlowdownHack;
		if (hax.isEnabled() && hax.getSlimeUtil())
			ci.cancel();
			
	}

}
