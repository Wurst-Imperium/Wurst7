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

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.wurstclient.WurstClient;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin
{
	/**
	 * This mixin is injected into the `BLOCK` case of the `item.getUseAction()`
	 * switch.
	 */
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
		ordinal = 3),
		method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V")
	private void onApplyEquipOffsetBlocking(AbstractClientPlayerEntity player,
		float tickProgress, float pitch, Hand hand, float swingProgress,
		ItemStack item, float equipProgress, MatrixStack matrices,
		OrderedRenderCommandQueue entityRenderCommandQueue, int light,
		CallbackInfo ci)
	{
		// lower shield when blocking
		if(item.getItem() == Items.SHIELD)
			WurstClient.INSTANCE.getHax().noShieldOverlayHack
				.adjustShieldPosition(matrices, true);
	}
	
	/**
	 * This mixin is injected into the last `else` block of
	 * renderFirstPersonItem(), right after `else if(player.isUsingRiptide())`.
	 */
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
		ordinal = 2),
		method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V")
	private void onApplySwingOffsetNotBlocking(
		AbstractClientPlayerEntity player, float tickProgress, float pitch,
		Hand hand, float swingProgress, ItemStack item, float equipProgress,
		MatrixStack matrices,
		OrderedRenderCommandQueue entityRenderCommandQueue, int light,
		CallbackInfo ci)
	{
		// lower shield when not blocking
		if(item.getItem() == Items.SHIELD)
			WurstClient.INSTANCE.getHax().noShieldOverlayHack
				.adjustShieldPosition(matrices, false);
	}
}
