/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>
{
	@Shadow
	@Final
	protected EntityRenderDispatcher entityRenderDispatcher;
	
	@Inject(at = @At("HEAD"),
		method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		cancellable = true)
	private void onRenderLabelIfPresent(S state, Component text,
		PoseStack matrices, MultiBufferSource vertexConsumers, int light,
		CallbackInfo ci)
	{
		// do NameTags adjustments
		wurstRenderLabelIfPresent(state, text, matrices, vertexConsumers,
			light);
		ci.cancel();
	}
	
	/**
	 * Copy of renderLabelIfPresent() since calling the original would result in
	 * an infinite loop. Also makes it easier to modify.
	 */
	protected void wurstRenderLabelIfPresent(S state, Component text,
		PoseStack matrices, MultiBufferSource vertexConsumers, int light)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		// get attachment point
		Vec3 attVec = state.nameTagAttachment;
		if(attVec == null)
			return;
		
		// disable sneaking changes if NameTags is enabled
		boolean notSneaky = !state.isDiscrete || nameTags.isEnabled();
		
		int labelY = "deadmau5".equals(text.getString()) ? -10 : 0;
		
		matrices.pushPose();
		matrices.translate(attVec.x, attVec.y + 0.5, attVec.z);
		matrices.mulPose(entityRenderDispatcher.cameraOrientation());
		
		// adjust scale if NameTags is enabled
		float scale = 0.025F * nameTags.getScale();
		if(nameTags.isEnabled())
		{
			Vec3 entityPos = new Vec3(state.x, state.y, state.z);
			double distance =
				WurstClient.MC.player.position().distanceTo(entityPos);
			if(distance > 10)
				scale *= distance / 10;
		}
		matrices.scale(scale, -scale, scale);
		
		Matrix4f matrix = matrices.last().pose();
		float bgOpacity = WurstClient.MC.options.getBackgroundOpacity(0.25F);
		int bgColor = (int)(bgOpacity * 255F) << 24;
		Font tr = getFont();
		float labelX = -tr.width(text) / 2;
		
		// adjust layers if using NameTags in see-through mode
		DisplayMode bgLayer = notSneaky && !nameTags.isSeeThrough()
			? DisplayMode.SEE_THROUGH : DisplayMode.NORMAL;
		DisplayMode textLayer = nameTags.isSeeThrough()
			? DisplayMode.SEE_THROUGH : DisplayMode.NORMAL;
		
		// draw background
		tr.drawInBatch(text, labelX, labelY, 0x20FFFFFF, false, matrix,
			vertexConsumers, bgLayer, bgColor, light);
		
		// draw text
		if(notSneaky)
			tr.drawInBatch(text, labelX, labelY, 0xFFFFFFFF, false, matrix,
				vertexConsumers, textLayer, 0, light);
		
		matrices.popPose();
	}
	
	/**
	 * Disables the nametag distance limit if configured in NameTags.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"),
		method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V")
	private double fakeSquaredDistanceToCamera(
		EntityRenderDispatcher dispatcher, Entity entity,
		Operation<Double> original,
		@Share("actualDistanceSq") LocalDoubleRef actualDistanceSq)
	{
		actualDistanceSq.set(original.call(dispatcher, entity));
		
		if(WurstClient.INSTANCE.getHax().nameTagsHack.isUnlimitedRange())
			return 0;
		
		return actualDistanceSq.get();
	}
	
	/**
	 * Restores the true squared distance so we don't break other code that
	 * might rely on it.
	 */
	@Inject(at = @At("TAIL"),
		method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V")
	private void restoreSquaredDistanceToCamera(T entity, S state,
		float tickDelta, CallbackInfo ci,
		@Share("actualDistanceSq") LocalDoubleRef actualDistanceSq)
	{
		state.distanceToCameraSq = actualDistanceSq.get();
	}
	
	@Shadow
	public abstract Font getFont();
}
