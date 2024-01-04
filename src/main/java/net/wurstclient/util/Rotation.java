/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.WurstClient;

public record Rotation(float yaw, float pitch)
{
	private static final MinecraftClient MC = WurstClient.MC;
	
	public static Rotation wrapped(float yaw, float pitch)
	{
		return new Rotation(MathHelper.wrapDegrees(yaw),
			MathHelper.wrapDegrees(pitch));
	}
	
	public void sendPlayerLookPacket()
	{
		sendPlayerLookPacket(MC.player.isOnGround());
	}
	
	public void sendPlayerLookPacket(boolean onGround)
	{
		MC.player.networkHandler.sendPacket(
			new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround));
	}
}
