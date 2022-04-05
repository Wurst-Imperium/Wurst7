/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
	public NoFireOverlayHack()
	{
		super("NoFireOverlay");
		setCategory(Category.RENDER);
		addSetting(mode);
	}
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
			"\u00a7lLower\u00a7r mode lowers the overlay.\n"
					+ "\u00a7lRemove\u00a7r mode removes the overlay.",
			Mode.values(), Mode.LOWER);
	public boolean shouldCancelOverlay()
	{
		return isEnabled() && mode.getSelected() == Mode.REMOVE;
	}

	public boolean shouldLowerOverlay()
	{
		return isEnabled() && mode.getSelected() == Mode.LOWER;
	}

	private enum Mode
	{
		LOWER,
		REMOVE
	}
	// See InGameOverlayRendererMixin.onRenderFireOverlay()
}
