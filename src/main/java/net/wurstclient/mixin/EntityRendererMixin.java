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

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>
{
	@Shadow
	@Final
	protected EntityRenderDispatcher dispatcher;
	
	@Inject(at = @At("HEAD"),
		method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
		cancellable = true)
	private void onRenderLabelIfPresent(S state, Text text,
		MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
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
	protected void wurstRenderLabelIfPresent(S state, Text text,
		MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		// get attachment point
		Vec3d attVec = state.nameLabelPos;
		if(attVec == null)
			return;
		
		// disable sneaking changes if NameTags is enabled
		boolean notSneaky = !state.sneaking || nameTags.isEnabled();
		
		int labelY = "deadmau5".equals(text.getString()) ? -10 : 0;
		
		matrices.push();
		matrices.translate(attVec.x, attVec.y + 0.5, attVec.z);
		matrices.multiply(dispatcher.getRotation());
		
		// adjust scale if NameTags is enabled
		float scale = 0.025F * nameTags.getScale();
		if(nameTags.isEnabled())
		{
			Vec3d entityPos = new Vec3d(state.x, state.y, state.z);
			double distance =
				WurstClient.MC.player.getPos().distanceTo(entityPos);
			if(distance > 10)
				scale *= distance / 10;
		}
		matrices.scale(scale, -scale, scale);
		
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		float bgOpacity =
			WurstClient.MC.options.getTextBackgroundOpacity(0.25F);
		int bgColor = (int)(bgOpacity * 255F) << 24;
		TextRenderer tr = getTextRenderer();
		float labelX = -tr.getWidth(text) / 2;
		
		// adjust layers if using NameTags in see-through mode
		TextLayerType bgLayer = notSneaky && !nameTags.isSeeThrough()
			? TextLayerType.SEE_THROUGH : TextLayerType.NORMAL;
		TextLayerType textLayer = nameTags.isSeeThrough()
			? TextLayerType.SEE_THROUGH : TextLayerType.NORMAL;
		
		// draw background
		tr.draw(text, labelX, labelY, 0x20FFFFFF, false, matrix,
			vertexConsumers, bgLayer, bgColor, light);
		
		// draw text
		if(notSneaky)
			tr.draw(text, labelX, labelY, 0xFFFFFFFF, false, matrix,
				vertexConsumers, textLayer, 0, light);
		
		matrices.pop();
	}
	
	/**
	 * Disables the nametag distance limit if configured in NameTags.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;getSquaredDistanceToCamera(Lnet/minecraft/entity/Entity;)D"),
		method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V")
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
		method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V")
	private void restoreSquaredDistanceToCamera(T entity, S state,
		float tickDelta, CallbackInfo ci,
		@Share("actualDistanceSq") LocalDoubleRef actualDistanceSq)
	{
		state.squaredDistanceToCamera = actualDistanceSq.get();
	}
	
	@Shadow
	public abstract TextRenderer getTextRenderer();
}
