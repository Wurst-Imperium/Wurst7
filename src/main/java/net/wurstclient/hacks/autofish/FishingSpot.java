/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public record FishingSpot(PositionAndRotation input, Vec3 bobberPos,
	boolean openWater)
{
	public FishingSpot(PositionAndRotation input, FishingHook bobber)
	{
		this(input, bobber.position(), bobber.isOpenWaterFishing());
	}
}
