/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
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
		MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
		int i)
	{
		double d = this.dispatcher.getSquaredDistanceToCamera(entity);
		
		if(d > 4096)
			return;
		
		NameTagsHack nameTagsHack = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		boolean bl = !entity.isSneaky() || nameTagsHack.isEnabled();
		float f = entity.getHeight() + 0.5F;
		int j = "deadmau5".equals(text.getString()) ? -10 : 0;
		
		matrixStack.push();
		matrixStack.translate(0.0D, f, 0.0D);
		matrixStack.multiply(this.dispatcher.getRotation());
		
		float scale = 0.025F;
		if(nameTagsHack.isEnabled())
		{
			double distance = WurstClient.MC.player.distanceTo(entity);
			
			if(distance > 10)
				scale *= distance / 10;
		}
		
		matrixStack.scale(-scale, -scale, scale);
		
		Matrix4f matrix4f = matrixStack.peek().getModel();
		float g = WurstClient.MC.options.getTextBackgroundOpacity(0.25F);
		int k = (int)(g * 255.0F) << 24;
		
		TextRenderer textRenderer = this.getFontRenderer();
		float h = -textRenderer.getWidth(text) / 2;
		
		textRenderer.draw(text.asOrderedText(), h, j, 553648127, false,
			matrix4f, vertexConsumerProvider, bl, k, i);
		
		if(bl)
			textRenderer.draw(text.asOrderedText(), h, j, -1, false, matrix4f,
				vertexConsumerProvider, false, 0, i);
		
		matrixStack.pop();
	}
	
	@Shadow
	public TextRenderer getFontRenderer()
	{
		return null;
	}
}
