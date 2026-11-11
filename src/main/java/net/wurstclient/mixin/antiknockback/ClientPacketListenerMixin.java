/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.antiknockback;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin
{
	/**
	 * Applies AntiKnockback's strength settings to regular absolute knockback
	 * for the local player.
	 */
	@ModifyExpressionValue(
		method = "handleSetEntityMotion(Lnet/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket;movement()Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 modifySetEntityMotion(Vec3 original,
		ClientboundSetEntityMotionPacket packet)
	{
		if(WurstClient.MC.player == null
			|| packet.id() != WurstClient.MC.player.getId())
			return original;
		
		return WurstClient.INSTANCE.getHax().antiKnockbackHack
			.modifyKnockback(original);
	}
	
	/**
	 * Applies AntiKnockback's strength settings to additive knockback
	 * (explosions) for the local player.
	 */
	@ModifyExpressionValue(
		method = "handleAddEntityMotion(Lnet/minecraft/network/protocol/game/ClientboundAddEntityMotionPacket;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/game/ClientboundAddEntityMotionPacket;impulse()Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 modifyAddEntityMotion(Vec3 original,
		ClientboundAddEntityMotionPacket packet)
	{
		if(WurstClient.MC.player == null
			|| packet.id() != WurstClient.MC.player.getId())
			return original;
		
		return WurstClient.INSTANCE.getHax().antiKnockbackHack
			.modifyKnockback(original);
	}
}
