/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import java.time.Instant;

import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.util.math.Vec3d;

public interface IClientPlayerEntity
{
	public void setNoClip(boolean noClip);
	
	public float getLastYaw();
	
	public float getLastPitch();
	
	public void setMovementMultiplier(Vec3d movementMultiplier);
	
	public boolean isTouchingWaterBypass();
	
	public NetworkEncryptionUtils.class_7425 signChatMessage(Instant timestamp,
		String message);
}
