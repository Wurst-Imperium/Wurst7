/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.nametags;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin
{
	@Shadow
	@Final
	public SimpleFeatureRenderPhase nameTags;
	
	@Shadow
	@Final
	public TranslucentFeatureRenderPhase seeThroughNameTags;
	
	@WrapOperation(
		method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
	private void wrapLabelScale(PoseStack matrices, float x, float y, float z,
		Operation<Void> original, PoseStack matrices2,
		@Nullable Vec3 nameTagAttachment, int offset, Component name,
		boolean seeThrough, int lightCoords, CameraRenderState camera)
	{
		NameTagsHack nameTagsHack = WurstClient.INSTANCE.getHax().nameTagsHack;
		if(!nameTagsHack.isEnabled())
		{
			original.call(matrices, x, y, z);
			return;
		}
		
		float scale = 0.025F * nameTagsHack.getScale();
		Matrix4f pose = new Matrix4f(matrices.last().pose());
		double distance =
			Math.sqrt(TranslucentSubmit.computeDistanceToCameraSq(pose));
		if(distance > 10)
			scale *= distance / 10;
		
		original.call(matrices, scale, -scale, scale);
	}
	
	/**
	 * Makes name tags remain visible while the player is sneaking when NameTags
	 * is enabled.
	 */
	@ModifyVariable(
		method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At("HEAD"),
		argsOnly = true)
	private boolean forceNotSneaking(boolean notSneaking)
	{
		return notSneaking
			|| WurstClient.INSTANCE.getHax().nameTagsHack.isEnabled();
	}
	
	@WrapOperation(
		method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V",
			ordinal = 0))
	private void swapNormalNameTagSubmit(SimpleFeatureRenderPhase phase,
		SubmitNode submit, Operation<Void> original)
	{
		if(!WurstClient.INSTANCE.getHax().nameTagsHack.isSeeThrough())
		{
			original.call(phase, submit);
			return;
		}
		
		seeThroughNameTags
			.submit(copyWithDisplayMode((NameTagFeatureRenderer.Submit)submit,
				Font.DisplayMode.SEE_THROUGH));
	}
	
	@WrapOperation(
		method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/phase/TranslucentFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/TranslucentSubmit;)V"))
	private void swapSeeThroughNameTagSubmit(
		TranslucentFeatureRenderPhase phase, TranslucentSubmit submit,
		Operation<Void> original)
	{
		if(!WurstClient.INSTANCE.getHax().nameTagsHack.isSeeThrough())
		{
			original.call(phase, submit);
			return;
		}
		
		nameTags.submit(copyWithDisplayMode(
			(NameTagFeatureRenderer.Submit)submit, Font.DisplayMode.NORMAL));
	}
	
	private NameTagFeatureRenderer.Submit copyWithDisplayMode(
		NameTagFeatureRenderer.Submit nameTag, Font.DisplayMode displayMode)
	{
		return new NameTagFeatureRenderer.Submit(nameTag.pose(), nameTag.x(),
			nameTag.y(), nameTag.text(), nameTag.lightCoords(), nameTag.color(),
			nameTag.backgroundColor(), displayMode);
	}
}
