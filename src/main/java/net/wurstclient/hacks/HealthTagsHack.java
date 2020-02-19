/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.LivingEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"health tags"})
public final class HealthTagsHack extends Hack
{
	public HealthTagsHack()
	{
		super("HealthTags", "Shows the health of players in their nametags.");
		setCategory(Category.RENDER);
	}
	
	public String addHealth(LivingEntity entity, String nametag)
	{
		if(!isEnabled())
			return nametag;
		
		int health = (int)entity.getHealth();
		return nametag + " " + getColor(health) + health;
	}
	
	private String getColor(int health)
	{
		if(health <= 5)
			return "\u00a74";
		
		if(health <= 10)
			return "\u00a76";
		
		if(health <= 15)
			return "\u00a7e";
		
		return "\u00a7a";
	}
}
