/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;

public class RotationUtils
{
	public static Vec3d getClientLookVec()
	{
		ClientPlayerEntity player = WurstClient.MC.player;
		float f = 0.017453292F;
		float pi = (float)Math.PI;
		
		float f1 = MathHelper.cos(-player.yaw * f - pi);
		float f2 = MathHelper.sin(-player.yaw * f - pi);
		float f3 = -MathHelper.cos(-player.pitch * f);
		float f4 = MathHelper.sin(-player.pitch * f);
		
		return new Vec3d(f2 * f3, f4, f1 * f3);
	}
}
