/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

public final class EspStyleSetting extends EnumSetting<EspStyleSetting.EspStyle>
{
	public EspStyleSetting()
	{
		super("Style", EspStyle.values(), EspStyle.BOXES);
	}
	
	public EspStyleSetting(EspStyle selected)
	{
		super("Style", EspStyle.values(), selected);
	}
	
	public EspStyleSetting(String name, String description, EspStyle selected)
	{
		super(name, description, EspStyle.values(), selected);
	}
	
	public boolean hasBoxes()
	{
		return getSelected().boxes;
	}
	
	public boolean hasLines()
	{
		return getSelected().lines;
	}
	
	public enum EspStyle
	{
		BOXES("Boxes only", true, false),
		LINES("Lines only", false, true),
		LINES_AND_BOXES("Lines and boxes", true, true);
		
		private final String name;
		private final boolean boxes;
		private final boolean lines;
		
		private EspStyle(String name, boolean boxes, boolean lines)
		{
			this.name = name;
			this.boxes = boxes;
			this.lines = lines;
		}
		
		public boolean hasBoxes()
		{
			return boxes;
		}
		
		public boolean hasLines()
		{
			return lines;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
