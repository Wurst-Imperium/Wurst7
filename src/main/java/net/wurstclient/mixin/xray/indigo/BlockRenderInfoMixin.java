/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.xray.indigo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.XRayHack;

/**
 * Last updated for Fabric Renderer Indigo 8.0.0+51b152e147 (Minecraft 26.1.1).
 */
@Pseudo
@Mixin(
	targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl",
	remap = false)
public abstract class BlockRenderInfoMixin
{
	@Shadow
	private BlockPos pos;
	@Shadow
	private BlockState blockState;
	
	/**
	 * Hides and shows regular blocks when using X-Ray, if Indigo is running
	 * and Sodium is not installed.
	 */
	@Inject(method = "shouldCullFace",
		at = @At("HEAD"),
		require = 0,
		cancellable = true)
	private void onShouldCullFace(Direction face,
		CallbackInfoReturnable<Boolean> cir)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		Boolean shouldDrawSide = xray.shouldDrawSide(blockState, pos);
		
		if(shouldDrawSide != null)
			cir.setReturnValue(!shouldDrawSide);
	}
}
