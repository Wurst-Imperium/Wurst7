/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity>
{
	@Shadow
	@Final
	protected EntityRenderDispatcher dispatcher;
	
	@Inject(at = {@At("HEAD")},
		method = {
			"renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
		cancellable = true)
	private void onRenderLabelIfPresent(T entity, Text text,
		MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
		int i, CallbackInfo ci)
	{
		if(entity instanceof LivingEntity)
			text = WurstClient.INSTANCE.getHax().healthTagsHack
				.addHealth((LivingEntity)entity, text);
		
		wurstRenderLabelIfPresent(entity, text, matrixStack,
			vertexConsumerProvider, i);
		ci.cancel();
	}
	
	/**
	 * Copy of renderLabelIfPresent() since calling the original would result in
	 * an infinite loop. Also makes it easier to modify.
	 */
	protected void wurstRenderLabelIfPresent(T entity, Text text,
		MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)
	{
		double d = this.dispatcher.getSquaredDistanceToCamera(entity);
		if(d > 4096.0)
			return;
		
		NameTagsHack nameTagsHack = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		boolean bl = !entity.isSneaky() || nameTagsHack.isEnabled();
		float f = entity.getHeight() + 0.5F;
		int i = "deadmau5".equals(text.getString()) ? -10 : 0;
		
		matrices.push();
		matrices.translate(0F, f, 0F);
		matrices.multiply(this.dispatcher.getRotation());
		
		float scale = 0.025F;
		if(nameTagsHack.isEnabled())
		{
			double distance = WurstClient.MC.player.distanceTo(entity);
			
			if(distance > 10)
				scale *= distance / 10;
		}
		
		matrices.scale(-scale, -scale, scale);
		
		Matrix4f matrix4f = matrices.peek().getPositionMatrix();
		float g = WurstClient.MC.options.getTextBackgroundOpacity(0.25F);
		int j = (int)(g * 255.0F) << 24;
		
		TextRenderer textRenderer = this.getTextRenderer();
		float h = -textRenderer.getWidth(text) / 2;
		
		textRenderer.draw(text, h, i, 0x20FFFFFF, false, matrix4f,
			vertexConsumers, bl ? TextRenderer.TextLayerType.SEE_THROUGH
				: TextRenderer.TextLayerType.NORMAL,
			j, light);
		
		if(bl)
			textRenderer.draw(text, h, i, -1, false, matrix4f, vertexConsumers,
				TextRenderer.TextLayerType.NORMAL, 0, light);
		
		matrices.pop();
	}
	
	@Shadow
	public TextRenderer getTextRenderer()
	{
		return null;
	}
}
