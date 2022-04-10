/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public interface IClientPlayerEntity
{
	public void setNoClip(boolean noClip);
	
	public float getLastYaw();
	
	public float getLastPitch();
	
	public void setMovementMultiplier(Vec3d movementMultiplier);
	
	public boolean isTouchingWaterBypass();

	public float getAirSpeed();

	void setAirSpeed(float valueF);

	Vec3d getVelocity();

	void setVelocity(Vec3d zero);

	void setVelocity(double x, double y, double z);

	void addVelocity(double deltaX, double deltaY, double deltaZ);

	PlayerAbilities getAbilities();

	void setOnGround(boolean b);

	void setFallDistance(float value);

	boolean isOnGround();

	boolean isTouchingWater();

	boolean isInLava();

	boolean isClimbing();

	Box getBoundingBox();
}
