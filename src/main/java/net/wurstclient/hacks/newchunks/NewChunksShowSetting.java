/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.newchunks;

import net.wurstclient.settings.EnumSetting;

public final class NewChunksShowSetting
	extends EnumSetting<NewChunksShowSetting.Show>
{
	public NewChunksShowSetting()
	{
		super("Show", Show.values(), Show.NEW_CHUNKS);
	}
	
	public static enum Show
	{
		NEW_CHUNKS("New Chunks", true, false),
		OLD_CHUNKS("Old Chunks", false, true),
		BOTH("Both", true, true);
		
		private final String name;
		private final boolean includeNew;
		private final boolean includeOld;
		
		private Show(String name, boolean showNew, boolean showOld)
		{
			this.name = name;
			includeNew = showNew;
			includeOld = showOld;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		public boolean includesNew()
		{
			return includeNew;
		}
		
		public boolean includesOld()
		{
			return includeOld;
		}
	}
}
