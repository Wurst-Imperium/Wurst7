/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.antiknockback;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer
{
	private LocalPlayerMixin(WurstClient wurst, ClientLevel world,
		GameProfile profile)
	{
		super(world, profile);
	}
	
	/**
	 * Applies AntiKnockback's strength settings to regular knockback for the
	 * local player.
	 */
	@Override
	public void lerpMotion(Vec3 vec)
	{
		super.lerpMotion(WurstClient.INSTANCE.getHax().antiKnockbackHack
			.modifyKnockback(vec));
	}
}
