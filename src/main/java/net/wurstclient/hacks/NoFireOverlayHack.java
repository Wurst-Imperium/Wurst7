/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"no fire overlay"})
public final class NoFireOverlayHack extends Hack
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"\u00a7lLower\u00a7r mode shrinks the fire overlay.\n"
			+ "\u00a7lNone\u00a7r mode removes the overlay.",
		Mode.values(), Mode.LOWER);
	
	public NoFireOverlayHack()
	{
		super("NoFireOverlay",
			"Blocks the overlay when you are on fire.\n\n"
				+ "\u00a7c\u00a7lWARNING:\u00a7r This can cause you to burn\n"
				+ "to death without noticing.");
		
		setCategory(Category.RENDER);
		addSetting(mode);
	}
	
	public boolean shouldCancelOverlay()
	{
		return isEnabled() && mode.getSelected() == Mode.NONE;
	}
	
	public boolean shouldLowerOverlay()
	{
		return isEnabled() && mode.getSelected() == Mode.LOWER;
	}
	
	private enum Mode
	{
		LOWER,
		NONE;
	}
}
