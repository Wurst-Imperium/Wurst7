/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.nukers;

import net.wurstclient.settings.EnumSetting;

public final class NukerShapeSetting
	extends EnumSetting<NukerShapeSetting.NukerShape>
{
	public NukerShapeSetting()
	{
		super("Shape",
			"\u00a7lNote:\u00a7r If your range is set too high, the cube shape"
				+ " will start to look like a sphere because you can't reach"
				+ " the corners. Ranges 1-3 work best for the cube shape.",
			NukerShape.values(), NukerShape.SPHERE);
	}
	
	public enum NukerShape
	{
		SPHERE("Sphere"),
		CUBE("Cube");
		
		private final String name;
		
		private NukerShape(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
