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
import net.wurstclient.hacks.NameTagsHack;

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
	private void onRenderLabel(T entity, String string, double x, double y,
		double z, int maxDistance, CallbackInfo ci)
	{
		if(entity instanceof LivingEntity)
			string = WurstClient.INSTANCE.getHax().healthTagsHack
				.addHealth((LivingEntity)entity, string);
		
		wurstRenderLabel(entity, string, x, y, z, maxDistance);
		ci.cancel();
	}
	
	/**
	 * Copy of renderLabel() since calling the original would result in
	 * an infinite loop. Also makes it easier to modify.
	 */
	protected void wurstRenderLabel(T entity, String string, double x, double y,
		double z, int maxDistance)
	{
		double d = entity.squaredDistanceTo(renderManager.camera.getPos());
		
		if(d > maxDistance * maxDistance)
			return;
		
		NameTagsHack nameTagsHack = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		boolean bl = entity.isInSneakingPose() || nameTagsHack.isEnabled();
		float f = this.renderManager.cameraYaw;
		float g = this.renderManager.cameraPitch;
		float h = entity.getHeight() + 0.5F - (bl ? 0.25F : 0.0F);
		int i = "deadmau5".equals(string) ? -10 : 0;
		
		GameRenderer.renderFloatingText(this.getFontRenderer(), string,
			(float)x, (float)y + h, (float)z, i, f, g, bl);
	}
	
	@Shadow
	public TextRenderer getFontRenderer()
	{
		return null;
	}
}
