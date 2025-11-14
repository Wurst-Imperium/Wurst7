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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity>
{
	@Shadow
	@Final
	protected EntityRenderDispatcher entityRenderDispatcher;
	
	@Inject(at = @At("HEAD"),
		method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
		cancellable = true)
	private void onRenderLabelIfPresent(T entity, Component text,
		PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i,
		float tickDelta, CallbackInfo ci)
	{
		// add HealthTags info
		if(entity instanceof LivingEntity)
			text = WurstClient.INSTANCE.getHax().healthTagsHack
				.addHealth((LivingEntity)entity, text);
		
		// do NameTags adjustments
		wurstRenderLabelIfPresent(entity, text, matrixStack,
			vertexConsumerProvider, i, tickDelta);
		ci.cancel();
	}
	
	/**
	 * Copy of renderLabelIfPresent() since calling the original would result in
	 * an infinite loop. Also makes it easier to modify.
	 */
	protected void wurstRenderLabelIfPresent(T entity, Component text,
		PoseStack matrices, MultiBufferSource vertexConsumers, int light,
		float tickDelta)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		// disable distance limit if configured in NameTags
		double distanceSq = entityRenderDispatcher.distanceToSqr(entity);
		if(distanceSq > 4096 && !nameTags.isUnlimitedRange())
			return;
		
		// get attachment point
		Vec3 attVec = entity.getAttachments().getNullable(
			EntityAttachment.NAME_TAG, 0, entity.getViewYRot(tickDelta));
		if(attVec == null)
			return;
		
		// disable sneaking changes if NameTags is enabled
		boolean notSneaky = !entity.isDiscrete() || nameTags.isEnabled();
		
		int labelY = "deadmau5".equals(text.getString()) ? -10 : 0;
		
		matrices.pushPose();
		matrices.translate(attVec.x, attVec.y + 0.5, attVec.z);
		matrices.mulPose(entityRenderDispatcher.cameraOrientation());
		
		// adjust scale if NameTags is enabled
		float scale = 0.025F * nameTags.getScale();
		if(nameTags.isEnabled())
		{
			double distance = WurstClient.MC.player.distanceTo(entity);
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
	
	@Shadow
	public abstract Font getFont();
}
