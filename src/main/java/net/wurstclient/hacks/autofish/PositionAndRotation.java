/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.util.Rotation;

public record PositionAndRotation(Vec3 pos, Rotation rotation)
{
	public PositionAndRotation(Entity entity)
	{
		this(entity.position(),
			Rotation.wrapped(entity.getYRot(), entity.getXRot()));
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
