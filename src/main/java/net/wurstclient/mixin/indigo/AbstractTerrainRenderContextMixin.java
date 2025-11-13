/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.indigo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

// import
// net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
// import
// net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
// import
// net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;

@Pseudo
@Mixin(
	targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext",
	remap = false)
public abstract class AbstractTerrainRenderContextMixin
{
	// @Shadow
	// @Final
	// private BlockRenderInfo blockInfo;
	
	// /**
	// * Applies X-Ray's opacity mask to the block color after all the normal
	// * coloring and shading is done, if Indigo is running.
	// */
	// @Inject(at = @At("RETURN"),
	// method =
	// "shadeQuad(Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;ZZZ)V",
	// require = 0)
	// private void onShadeQuad(MutableQuadViewImpl quad, boolean ao,
	// boolean emissive, boolean vanillaShade, CallbackInfo ci)
	// {
	// XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
	// if(!xray.isOpacityMode() || xray
	// .isVisible(blockInfo.blockState.getBlock(), blockInfo.blockPos))
	// return;
	
	// for(int i = 0; i < 4; i++)
	// quad.color(i, quad.color(i) & xray.getOpacityColorMask());
	// }
}
