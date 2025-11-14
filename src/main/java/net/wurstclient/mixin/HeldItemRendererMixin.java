/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.wurstclient.WurstClient;

@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemRendererMixin
{
	@Inject(at = {@At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
		ordinal = 4)}, method = "renderArmWithItem")
	private void onApplyEquipOffsetBlocking(AbstractClientPlayer player,
		float tickDelta, float pitch, InteractionHand hand, float swingProgress,
		ItemStack item, float equipProgress, PoseStack matrices,
		MultiBufferSource vertexConsumers, int light, CallbackInfo ci)
	{
		// lower shield when blocking
		if(item.getItem() == Items.SHIELD)
			WurstClient.INSTANCE.getHax().noShieldOverlayHack
				.adjustShieldPosition(matrices, true);
	}
	
	@Inject(at = {@At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
		ordinal = 1)}, method = "renderArmWithItem")
	private void onApplySwingOffsetNotBlocking(AbstractClientPlayer player,
		float tickDelta, float pitch, InteractionHand hand, float swingProgress,
		ItemStack item, float equipProgress, PoseStack matrices,
		MultiBufferSource vertexConsumers, int light, CallbackInfo ci)
	{
		// lower shield when not blocking
		if(item.getItem() == Items.SHIELD)
			WurstClient.INSTANCE.getHax().noShieldOverlayHack
				.adjustShieldPosition(matrices, false);
	}
}
