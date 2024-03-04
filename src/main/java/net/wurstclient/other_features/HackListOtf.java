/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import java.awt.Color;
import java.util.Comparator;

import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.Hack;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"ArrayList", "ModList", "CheatList", "mod list", "array list",
	"hack list", "cheat list"})
@DontBlock
public final class HackListOtf extends OtherFeature
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"\u00a7lAuto\u00a7r mode renders the whole list if it fits onto the screen.\n"
			+ "\u00a7lCount\u00a7r mode only renders the number of active hacks.\n"
			+ "\u00a7lHidden\u00a7r mode renders nothing.",
		Mode.values(), Mode.AUTO);
	
	private final EnumSetting<Position> position = new EnumSetting<>("Position",
		"Which side of the screen the HackList should be shown on."
			+ "\nChange this to \u00a7lRight\u00a7r when using TabGUI.",
		Position.values(), Position.LEFT);
	
	private final ColorSetting color = new ColorSetting("Color",
		"Color of the HackList text.\n"
			+ "Only visible when \u00a76RainbowUI\u00a7r is disabled.",
		Color.WHITE);
	
	private final EnumSetting<SortBy> sortBy = new EnumSetting<>("Sort by",
		"Determines how the HackList entries are sorted.\n"
			+ "Only visible when \u00a76Mode\u00a7r is set to \u00a76Auto\u00a7r.",
		SortBy.values(), SortBy.NAME);
	
	private final CheckboxSetting revSort =
		new CheckboxSetting("Reverse sorting", false);
	
	private final CheckboxSetting animations = new CheckboxSetting("Animations",
		"When enabled, entries slide into and out of the HackList as hacks are enabled and disabled.",
		true);
	
	private SortBy prevSortBy;
	private Boolean prevRevSort;
	
	public HackListOtf()
	{
		super("HackList", "Shows a list of active hacks on the screen.");
		
		addSetting(mode);
		addSetting(position);
		addSetting(color);
		addSetting(sortBy);
		addSetting(revSort);
		addSetting(animations);
	}
	
	public Mode getMode()
	{
		return mode.getSelected();
	}
	
	public Position getPosition()
	{
		return position.getSelected();
	}
	
	public boolean isAnimations()
	{
		return animations.isChecked();
	}
	
	public Comparator<Hack> getComparator()
	{
		if(revSort.isChecked())
			return sortBy.getSelected().comparator.reversed();
		
		return sortBy.getSelected().comparator;
	}
	
	public boolean shouldSort()
	{
		try
		{
			// width of a renderName could change at any time
			// must sort the HackList every tick
			if(sortBy.getSelected() == SortBy.WIDTH)
				return true;
			
			if(sortBy.getSelected() != prevSortBy)
				return true;
			
			if(!Boolean.valueOf(revSort.isChecked()).equals(prevRevSort))
				return true;
			
			return false;
			
		}finally
		{
			prevSortBy = sortBy.getSelected();
			prevRevSort = revSort.isChecked();
		}
	}
	
	public int getColor()
	{
		return color.getColorI() & 0x00FFFFFF;
	}
	
	public static enum Mode
	{
		AUTO("Auto"),
		
		COUNT("Count"),
		
		HIDDEN("Hidden");
		
		private final String name;
		
		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public static enum Position
	{
		LEFT("Left"),
		
		RIGHT("Right");
		
		private final String name;
		
		private Position(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public static enum SortBy
	{
		NAME("Name", (a, b) -> a.getName().compareToIgnoreCase(b.getName())),
		
		WIDTH("Width", Comparator.comparingInt(
			h -> WurstClient.MC.textRenderer.getWidth(h.getRenderName())));
		
		private final String name;
		private final Comparator<Hack> comparator;
		
		private SortBy(String name, Comparator<Hack> comparator)
		{
			this.name = name;
			this.comparator = comparator;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
