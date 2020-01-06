/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.minecraft.client.util.TextFormat;
import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"ArrayList", "ModList", "CheatList", "mod list", "array list",
	"hack list", "cheat list"})
public final class HackListOtf extends OtherFeature
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		TextFormat.BOLD + "Auto" + TextFormat.RESET
			+ " mode renders the whole list if it\n" + "fits onto the screen.\n"
			+ TextFormat.BOLD + "Count" + TextFormat.RESET
			+ " mode only renders the number\n" + "of active hacks.\n"
			+ TextFormat.BOLD + "Hidden" + TextFormat.RESET
			+ " mode renders nothing.",
		Mode.values(), Mode.AUTO);
	
	private final EnumSetting<Position> position =
		new EnumSetting<>("Position", Position.values(), Position.LEFT);
	
	private final CheckboxSetting animations =
		new CheckboxSetting("Animations", true);
	
	public HackListOtf()
	{
		super("HackList", "Shows a list of active hacks on the screen.\n"
			+ "The " + TextFormat.BOLD + "Left" + TextFormat.RESET
			+ " position should only be used while TabGui is\n" + "disabled.");
		
		addSetting(mode);
		addSetting(position);
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
}
