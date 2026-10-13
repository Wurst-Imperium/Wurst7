/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.antiknockback;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin
{
	/**
	 * Applies AntiKnockback's strength settings to explosion knockback before
	 * it is added to the local player's velocity.
	 */
	@ModifyExpressionValue(
		method = "handleExplosion(Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;playerKnockback()Ljava/util/Optional;"))
	private Optional<Vec3> onHandleExplosionPlayerKnockback(
		Optional<Vec3> original)
	{
		return original.map(
			WurstClient.INSTANCE.getHax().antiKnockbackHack::modifyKnockback);
	}
}
