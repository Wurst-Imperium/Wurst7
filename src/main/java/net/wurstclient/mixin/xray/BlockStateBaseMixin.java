/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.xray;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.wurstclient.WurstClient;

@Mixin(BlockStateBase.class)
public abstract class BlockStateBaseMixin extends StateHolder<Block, BlockState>
{
	private BlockStateBaseMixin(WurstClient wurst, Block owner,
		Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap,
		MapCodec<BlockState> codec)
	{
		super(owner, propertyMap, codec);
	}
	
	/*
	 * This, together with the gamma override, makes ores bright enough to see
	 * when using X-Ray.
	 *
	 * Uses priority 980 to be applied before Iris's MixinBlockStateBehavior
	 * with priority 990.
	 */
	@Inject(
		method = "getShadeBrightness(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
		at = @At("RETURN"),
		cancellable = true,
		order = 980)
	private void onGetShadeBrightness(BlockGetter blockGetter,
		BlockPos blockPos, CallbackInfoReturnable<Float> original)
	{
		if(!WurstClient.INSTANCE.getHax().xRayHack.isEnabled())
			return;
		
		original.setReturnValue(1F);
	}
}
