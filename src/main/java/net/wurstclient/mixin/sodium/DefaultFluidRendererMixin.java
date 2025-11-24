/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;
import net.wurstclient.hacks.XRayHack;

@Pseudo
@Mixin(targets = {
	"net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer"},
	remap = false)
public class DefaultFluidRendererMixin
{
	@Unique
	private ThreadLocal<BlockPos.MutableBlockPos> mutablePosForExposedCheck =
		ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
	
	/**
	 * This mixin hides and shows fluids when using X-Ray with Sodium installed.
	 *
	 * <p>
	 * Works with Sodium >=0.6.0-beta.1 and <0.6.1.
	 */
	@Inject(at = @At("HEAD"),
		method = "isFluidOccluded(Lnet/minecraft/class_1920;IIILnet/minecraft/class_2350;Lnet/minecraft/class_2680;Lnet/minecraft/class_3611;)Z",
		cancellable = true,
		remap = false,
		require = 0)
	private void onIsFluidOccludedInSodium060(BlockAndTintGetter world, int x,
		int y, int z, Direction dir, BlockState state, Fluid fluid,
		CallbackInfoReturnable<Boolean> cir)
	{
		BlockPos.MutableBlockPos pos = mutablePosForExposedCheck.get();
		pos.set(x, y, z);
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, pos);
		EventManager.fire(event);
		
		if(event.isRendered() != null)
			cir.setReturnValue(!event.isRendered());
	}
	
	/**
	 * This mixin hides and shows fluids when using X-Ray with Sodium installed.
	 *
	 * <p>
	 * Works with Sodium >=0.6.1 and <0.6.13. Last tested with Sodium
	 * 0.6.1+mc1.21.3.
	 */
	@Inject(at = @At("HEAD"),
		method = "isFluidOccluded(Lnet/minecraft/class_1920;IIILnet/minecraft/class_2350;Lnet/minecraft/class_2680;Lnet/minecraft/class_3610;)Z",
		cancellable = true,
		require = 0)
	private void onIsFluidOccluded(BlockAndTintGetter world, int x, int y,
		int z, Direction dir, BlockState state, FluidState fluid,
		CallbackInfoReturnable<Boolean> cir)
	{
		BlockPos.MutableBlockPos pos = mutablePosForExposedCheck.get();
		pos.set(x, y, z);
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, pos);
		EventManager.fire(event);
		
		if(event.isRendered() != null)
			cir.setReturnValue(!event.isRendered());
	}
	
	/**
	 * Hides and shows the top side of fluids when using X-Ray with Sodium
	 * installed.
	 *
	 * <p>
	 * Works with Sodium >=0.6.13. Last updated for Sodium 0.6.13+mc1.21.4.
	 */
	@Inject(at = @At("HEAD"),
		method = "isFullBlockFluidOccluded(Lnet/minecraft/class_1920;Lnet/minecraft/class_2338;Lnet/minecraft/class_2350;Lnet/minecraft/class_2680;Lnet/minecraft/class_3610;)Z",
		cancellable = true,
		remap = false,
		require = 0)
	private void onIsFullBlockFluidOccluded(BlockAndTintGetter world,
		BlockPos pos, Direction dir, BlockState state, FluidState fluid,
		CallbackInfoReturnable<Boolean> cir)
	{
		// Note: the null BlockPos is here to skip the "exposed only" check
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, null);
		EventManager.fire(event);
		
		if(event.isRendered() != null)
			cir.setReturnValue(!event.isRendered());
	}
	
	/**
	 * Hides and shows all other sides of fluids when using X-Ray with Sodium
	 * installed.
	 */
	@Inject(at = @At("HEAD"),
		method = "isSideExposed(Lnet/minecraft/class_1920;IIILnet/minecraft/class_2350;F)Z",
		cancellable = true,
		remap = false,
		require = 0)
	private void onIsSideExposed(BlockAndTintGetter world, int x, int y, int z,
		Direction dir, float height, CallbackInfoReturnable<Boolean> cir)
	{
		BlockPos pos = new BlockPos(x, y, z);
		BlockState state = world.getBlockState(pos);
		
		// Note: the null BlockPos is here to skip the "exposed only" check
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, null);
		EventManager.fire(event);
		
		if(event.isRendered() == null)
			return;
		
		BlockPos nPos = pos.offset(dir.getUnitVec3i());
		BlockState neighborState = world.getBlockState(nPos);
		
		cir.setReturnValue(!neighborState.getFluidState().getType()
			.isSame(state.getFluidState().getType()) && event.isRendered());
	}
	
	/**
	 * Modifies opacity of fluids when using X-Ray with Sodium installed.
	 *
	 * <p>
	 * Works with Sodium >=0.6.13. Last updated for Sodium 0.6.13+mc1.21.4.
	 */
	@ModifyExpressionValue(at = @At(value = "INVOKE",
		target = "Lnet/caffeinemc/mods/sodium/api/util/ColorARGB;toABGR(I)I"),
		method = "updateQuad",
		remap = false,
		require = 0)
	private int onUpdateQuad(int original, @Local(argsOnly = true) BlockPos pos,
		@Local(argsOnly = true) FluidState state)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		if(!xray.isOpacityMode()
			|| xray.isVisible(state.createLegacyBlock().getBlock(), pos))
			return original;
		
		return original & xray.getOpacityColorMask();
	}
}
