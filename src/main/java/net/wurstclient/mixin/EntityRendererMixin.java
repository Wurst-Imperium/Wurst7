/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.wurstclient.WurstClient;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity>
{
	@Shadow
	@Final
	protected EntityRenderDispatcher renderManager;
	
	@Inject(at = {@At("HEAD")},
		method = {
			"renderLabelIfPresent(Lnet/minecraft/entity/Entity;Ljava/lang/String;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
		cancellable = true)
	private void onRenderLabelIfPresent(T entity, String string,
		MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
		int i, CallbackInfo ci)
	{
		if(!(entity instanceof LivingEntity))
			return;
		
		String healthTag = WurstClient.INSTANCE.getHax().healthTagsHack
			.addHealth((LivingEntity)entity, string);
		
		wurstRenderLabelIfPresent(entity, healthTag, matrixStack,
			vertexConsumerProvider, i);
		ci.cancel();
	}
	
	/**
	 * Copy of renderLabelIfPresent() since calling the original would result in
	 * an infinite loop.
	 */
	protected void wurstRenderLabelIfPresent(T entity, String string,
		MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
		int i)
	{
		double d = this.renderManager.getSquaredDistanceToCamera(entity);
		if(d <= 4096.0D)
		{
			boolean bl = !entity.isSneaky();
			float f = entity.getHeight() + 0.5F;
			int j = "deadmau5".equals(string) ? -10 : 0;
			matrixStack.push();
			matrixStack.translate(0.0D, f, 0.0D);
			matrixStack.multiply(this.renderManager.getRotation());
			matrixStack.scale(-0.025F, -0.025F, 0.025F);
			Matrix4f matrix4f = matrixStack.peek().getModel();
			float g = MinecraftClient.getInstance().options
				.getTextBackgroundOpacity(0.25F);
			int k = (int)(g * 255.0F) << 24;
			TextRenderer textRenderer = this.getFontRenderer();
			float h = -textRenderer.getStringWidth(string) / 2;
			textRenderer.draw(string, h, j, 553648127, false, matrix4f,
				vertexConsumerProvider, bl, k, i);
			if(bl)
				textRenderer.draw(string, h, j, -1, false, matrix4f,
					vertexConsumerProvider, false, 0, i);
			matrixStack.pop();
		}
	}
	
	@Shadow
	public TextRenderer getFontRenderer()
	{
		return null;
	}
}
