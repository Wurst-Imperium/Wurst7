/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.Vec3d;

public record FishingSpot(PositionAndRotation input, Vec3d bobberPos,
	boolean openWater)
{
	public FishingSpot(PositionAndRotation input, FishingBobberEntity bobber)
	{
		this(input, bobber.getPos(), bobber.isInOpenWater());
	}
}
