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

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
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
			"renderLabel(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V"},
		cancellable = true)
	private void onRenderLabel(T entity, String text, double x, double y,
		double z, int maxDistance, CallbackInfo ci)
	{
		if(!(entity instanceof LivingEntity))
			return;
		
		String healthTag = WurstClient.INSTANCE.getHax().healthTagsHack
			.addHealth((LivingEntity)entity, text);
		
		wurstRenderLabel(entity, healthTag, x, y, z, maxDistance);
		ci.cancel();
	}
	
	/**
	 * Copy of renderLabel() since calling the original would result in
	 * an infinite loop.
	 */
	protected void wurstRenderLabel(T entity, String text, double x, double y,
		double z, int maxDistance)
	{
		double d = entity.squaredDistanceTo(this.renderManager.camera.getPos());
		if(d <= maxDistance * maxDistance)
		{
			boolean bl = entity.isInSneakingPose();
			float f = this.renderManager.cameraYaw;
			float g = this.renderManager.cameraPitch;
			float h = entity.getHeight() + 0.5F - (bl ? 0.25F : 0.0F);
			int i = "deadmau5".equals(text) ? -10 : 0;
			GameRenderer.renderFloatingText(this.getFontRenderer(), text,
				(float)x, (float)y + h, (float)z, i, f, g, bl);
		}
	}
	
	@Shadow
	public TextRenderer getFontRenderer()
	{
		return null;
	}
}
