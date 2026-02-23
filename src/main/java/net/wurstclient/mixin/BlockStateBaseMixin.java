/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.IsNormalCubeListener.IsNormalCubeEvent;
import net.wurstclient.hack.HackList;
import net.wurstclient.hacks.HandNoClipHack;

@Mixin(BlockStateBase.class)
public abstract class BlockStateBaseMixin extends StateHolder<Block, BlockState>
{
	private BlockStateBaseMixin(WurstClient wurst, Block owner,
		Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap,
		MapCodec<BlockState> codec)
	{
		super(owner, propertyMap, codec);
	}
	
	@Inject(
		method = "isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
		at = @At("TAIL"),
		cancellable = true)
	private void onIsFullCube(BlockGetter world, BlockPos pos,
		CallbackInfoReturnable<Boolean> cir)
	{
		IsNormalCubeEvent event = new IsNormalCubeEvent();
		EventManager.fire(event);
		
		cir.setReturnValue(cir.getReturnValue() && !event.isCancelled());
	}
	
	@Inject(
		method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
		at = @At("HEAD"),
		cancellable = true)
	private void onGetOutlineShape(BlockGetter view, BlockPos pos,
		CollisionContext context, CallbackInfoReturnable<VoxelShape> cir)
	{
		if(context == CollisionContext.empty())
			return;
		
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null)
			return;
		
		HandNoClipHack handNoClipHack = hax.handNoClipHack;
		if(!handNoClipHack.isEnabled() || handNoClipHack.isBlockInList(pos))
			return;
		
		cir.setReturnValue(Shapes.empty());
	}
	
	@Inject(
		method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
		at = @At("HEAD"),
		cancellable = true)
	private void onGetCollisionShape(BlockGetter world, BlockPos pos,
		CollisionContext context, CallbackInfoReturnable<VoxelShape> cir)
	{
		if(getFluidState().isEmpty())
			return;
		
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.jesusHack.shouldBeSolid())
			return;
		
		cir.setReturnValue(Shapes.block());
		cir.cancel();
	}
	
	@Shadow
	public abstract FluidState getFluidState();
}
