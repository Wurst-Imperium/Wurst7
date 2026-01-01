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
import net.wurstclient.events.GetAmbientOcclusionLightLevelListener.GetAmbientOcclusionLightLevelEvent;
import net.wurstclient.events.IsNormalCubeListener.IsNormalCubeEvent;
import net.wurstclient.hack.HackList;
import net.wurstclient.hacks.HandNoClipHack;

@Mixin(BlockStateBase.class)
public abstract class AbstractBlockStateMixin
	extends StateHolder<Block, BlockState>
{
	private AbstractBlockStateMixin(WurstClient wurst, Block owner,
		Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap,
		MapCodec<BlockState> codec)
	{
		super(owner, propertyMap, codec);
	}
	
	@Inject(at = @At("TAIL"),
		method = "isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
		cancellable = true)
	private void onIsFullCube(BlockGetter world, BlockPos pos,
		CallbackInfoReturnable<Boolean> cir)
	{
		IsNormalCubeEvent event = new IsNormalCubeEvent();
		EventManager.fire(event);
		
		cir.setReturnValue(cir.getReturnValue() && !event.isCancelled());
	}
	
	@Inject(at = @At("TAIL"),
		method = "getShadeBrightness(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
		cancellable = true)
	private void onGetAmbientOcclusionLightLevel(BlockGetter blockView,
		BlockPos blockPos, CallbackInfoReturnable<Float> cir)
	{
		GetAmbientOcclusionLightLevelEvent event =
			new GetAmbientOcclusionLightLevelEvent((BlockState)(Object)this,
				cir.getReturnValueF());
		
		EventManager.fire(event);
		cir.setReturnValue(event.getLightLevel());
	}
	
	@Inject(at = @At("HEAD"),
		method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
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
	
	@Inject(at = @At("HEAD"),
		method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
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
