/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.util.Rotation;

public record PositionAndRotation(Vec3d pos, Rotation rotation)
{
	public PositionAndRotation(Entity entity)
	{
		this(entity.getPos(),
			Rotation.wrapped(entity.getYaw(), entity.getPitch()));
	}
	
	public boolean isNearlyIdenticalTo(PositionAndRotation other)
	{
		return pos.distanceTo(other.pos) < 0.5
			&& rotation.getAngleTo(other.rotation) < 5;
	}
	
	public double differenceTo(PositionAndRotation other)
	{
		return pos.distanceTo(other.pos)
			+ rotation.getAngleTo(other.rotation) / 100;
	}
}
