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

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
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
public abstract class AbstractTerrainRenderContextMixin
{
	@Shadow
	private BlockPos pos;
	@Shadow
	private BlockState blockState;
	
	/**
	 * Applies X-Ray's opacity mask after Indigo has already done its shading
	 * and tinting.
	 */
	@Inject(method = "transform", at = @At("RETURN"), require = 0)
	private void onTransform(MutableQuadView quad,
		CallbackInfoReturnable<Boolean> cir)
	{
		if(!cir.getReturnValueZ())
			return;
		
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		if(!xray.isOpacityMode() || xray.isVisible(blockState.getBlock(), pos))
			return;
		
		quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT);
		for(int i = 0; i < 4; i++)
			quad.color(i, quad.color(i) & xray.getOpacityColorMask());
	}
}
