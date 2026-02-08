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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
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
	 */
	@ModifyReturnValue(at = @At("RETURN"),
		method = "getShadeBrightness(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F")
	private float modifyShadeBrightness(float original)
	{
		if(!WurstClient.INSTANCE.getHax().xRayHack.isEnabled())
			return original;
		
		return 1;
	}
}
