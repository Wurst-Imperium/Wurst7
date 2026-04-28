/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.fullbright;

import java.util.function.BooleanSupplier;

public final class BadOptimizationsLightmapHook implements BooleanSupplier
{
	private static boolean lightmapNeedsUpdate;
	
	@Override
	public boolean getAsBoolean()
	{
		boolean needsUpdate = lightmapNeedsUpdate;
		lightmapNeedsUpdate = false;
		return needsUpdate;
	}
	
	public static void markForUpdate()
	{
		lightmapNeedsUpdate = true;
	}
}
