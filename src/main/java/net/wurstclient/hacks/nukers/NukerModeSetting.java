/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.nukers;

import net.wurstclient.settings.EnumSetting;

public final class NukerModeSetting
	extends EnumSetting<NukerModeSetting.NukerMode>
{
	public NukerModeSetting()
	{
		super("Mode",
			"\u00a7lNormal\u00a7r mode simply breaks everything around you.\n\n"
				+ "\u00a7lID\u00a7r mode only breaks the selected block type."
				+ " Left-click on a block to select it.\n\n"
				+ "\u00a7lMultiID\u00a7r mode only breaks the block types in"
				+ " your MultiID List.\n\n"
				+ "\u00a7lSmash\u00a7r mode only breaks blocks that can be"
				+ " destroyed instantly (e.g. tall grass).",
			NukerMode.values(), NukerMode.NORMAL);
	}
	
	public enum NukerMode
	{
		NORMAL("Normal"),
		ID("ID"),
		MULTI_ID("MultiID"),
		SMASH("Smash");
		
		private final String name;
		
		private NukerMode(String name)
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
