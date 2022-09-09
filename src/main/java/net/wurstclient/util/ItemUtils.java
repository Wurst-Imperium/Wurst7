/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;

public enum ItemUtils
{
	;
	
	/**
	 * @param nameOrId
	 *            a String containing the item's name ({@link Identifier}) or
	 *            numeric ID.
	 * @return the requested item, or null if the item doesn't exist.
	 */
	public static Item getItemFromNameOrID(String nameOrId)
	{
		if(MathUtils.isInteger(nameOrId))
		{
			// There is no getOrEmpty() for raw IDs, so this detects when the
			// Registry defaults and returns null instead
			int id = Integer.parseInt(nameOrId);
			Item item = Registry.ITEM.get(id);
			if(id != 0 && Registry.ITEM.getRawId(item) == 0)
				return null;
			
			return item;
		}
		
		try
		{
			return Registry.ITEM.getOrEmpty(new Identifier(nameOrId))
				.orElse(null);
			
		}catch(InvalidIdentifierException e)
		{
			return null;
		}
	}
}
