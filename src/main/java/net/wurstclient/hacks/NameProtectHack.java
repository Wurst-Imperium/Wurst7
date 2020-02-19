/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"name protect"})
public final class NameProtectHack extends Hack
{
	public NameProtectHack()
	{
		super("NameProtect", "Hides all player names.");
		setCategory(Category.RENDER);
	}
	
	public String protect(String string)
	{
		if(!isEnabled() || MC.player == null)
			return string;
		
		String me = MC.getSession().getUsername();
		if(string.contains(me))
			return string.replace(me, "\u00a7oMe\u00a7r");
		
		int i = 0;
		for(PlayerListEntry info : MC.player.networkHandler.getPlayerList())
		{
			i++;
			String name =
				info.getProfile().getName().replaceAll("\u00a7(?:\\w|\\d)", "");
			
			if(string.contains(name))
				return string.replace(name, "\u00a7oPlayer" + i + "\u00a7r");
		}
		
		for(AbstractClientPlayerEntity player : MC.world.getPlayers())
		{
			i++;
			String name = player.getName().asString();
			
			if(string.contains(name))
				return string.replace(name, "\u00a7oPlayer" + i + "\u00a7r");
		}
		
		return string;
	}
}
