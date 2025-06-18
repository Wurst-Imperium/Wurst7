/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.FogModifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.wurstclient.WurstClient;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.util.List;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin
{
	@Shadow
	@Final
	private MappableRingBuffer fogBuffer;
	
	@Shadow
	protected abstract Vector4f getFogColor(Camera camera, float tickProgress,
		ClientWorld world, int viewDistance, float skyDarkness, boolean thick);
	
	@Shadow
	protected abstract CameraSubmersionType getCameraSubmersionType(
		Camera camera, boolean thick);
	
	@Shadow
	@Final
	private static List<FogModifier> FOG_MODIFIERS;
	
	@Shadow
	protected abstract void applyFog(ByteBuffer buffer, int bufPos,
		Vector4f fogColor, float environmentalStart, float environmentalEnd,
		float renderDistanceStart, float renderDistanceEnd, float skyEnd,
		float cloudEnd);
	
	/**
	 * @author IUDevman
	 *
	 *         Removes the circular fog that appears around the player near the
	 *         end of
	 *         the render distance.
	 *		
	 *         NOTE: This method basically copies what the original one does.
	 *		
	 *         I cannot stress enough that this seems to be the only way to do
	 *         this.
	 *         Modifying the fogData render distance variables in other ways
	 *         just does
	 *         not work.
	 *
	 *         Combined with AtmosphericFogModifierMixin and
	 *         DimensionOrBossFogModifierMixin, this removes all the regular
	 *         fog,
	 *         replicating the behavior of previous game versions.
	 *		
	 *         The variables used in this mixin and others for NoFog are not
	 *         important.
	 *         We just need to set them to such a far distance that the player
	 *         cannot
	 *         see the effect.
	 */
	@Inject(
		method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
		at = @At("HEAD"),
		cancellable = true)
	private void onApplyFog(Camera camera, int viewDistance, boolean thick,
		RenderTickCounter tickCounter, float skyDarkness, ClientWorld world,
		CallbackInfoReturnable<Vector4f> cir)
	{
		float tickProgress = tickCounter.getTickProgress(false);
		Vector4f fogColor = this.getFogColor(camera, tickProgress, world,
			viewDistance, skyDarkness, thick);
		
		float viewDistanceModified = (float)(viewDistance * 16);
		
		CameraSubmersionType cameraSubmersionType =
			this.getCameraSubmersionType(camera, thick);
		
		Entity entity = camera.getFocusedEntity();
		
		FogData fogData = new FogData();
		
		for(FogModifier fogModifier : FOG_MODIFIERS)
		{
			if(fogModifier.shouldApply(cameraSubmersionType, entity))
			{
				fogModifier.applyStartEndModifier(fogData, entity,
					camera.getBlockPos(), world, viewDistanceModified,
					tickCounter);
				break;
			}
		}
		
		float viewDistanceCorrection =
			MathHelper.clamp(viewDistanceModified / 10.0F, 4.0F, 64.0F);
		fogData.renderDistanceStart =
			viewDistanceModified - viewDistanceCorrection;
		fogData.renderDistanceEnd = viewDistanceModified;
		
		if(WurstClient.INSTANCE.getHax().noFogHack.isEnabled())
		{
			fogData.renderDistanceStart *= 100;
			fogData.renderDistanceEnd *= 100;
		}
		
		try(GpuBuffer.MappedView mappedView =
			RenderSystem.getDevice().createCommandEncoder()
				.mapBuffer(this.fogBuffer.getBlocking(), false, true))
		{
			this.applyFog(mappedView.data(), 0, fogColor,
				fogData.environmentalStart, fogData.environmentalEnd,
				fogData.renderDistanceStart, fogData.renderDistanceEnd,
				fogData.skyEnd, fogData.cloudEnd);
		}
		
		cir.setReturnValue(fogColor);
	}
}
