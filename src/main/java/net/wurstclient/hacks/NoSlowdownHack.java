/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"no slowdown", "no slow down"})
public final class NoSlowdownHack extends Hack
{
	private final CheckboxSetting blocks =
		new CheckboxSetting("Block honey + soul sand slowness", true);
	private final EnumSetting<ItemSlowness> items =
		new EnumSetting<>("Block item slowness", ItemSlowness.values(), ItemSlowness.ALL);
	
	public NoSlowdownHack()
	{
		super("NoSlowdown");
		setCategory(Category.MOVEMENT);
		addSetting(blocks);
		addSetting(items);
	}
	
	public boolean noBlockSlowness()
	{
		return isEnabled() && blocks.isChecked();
	}
	
	public boolean noItemSlowness()
	{
		return isEnabled() && items.getSelected() == ItemSlowness.ALL;
	}
	
	public boolean noNonBlockingItemSlowness()
	{
		return isEnabled() && items.getSelected() == ItemSlowness.EXCEPT_BLOCKING;
	}
	
	private enum ItemSlowness
	{
		ALL("All"),
		EXCEPT_BLOCKING("Except blocking"),
		NONE("None");
		
		private final String name;
		
		private ItemSlowness(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	// See BlockMixin.onGetVelocityMultiplier() and
	// ClientPlayerEntityMixin.wurstIsUsingItem()
}
