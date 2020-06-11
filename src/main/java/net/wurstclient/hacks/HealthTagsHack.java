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
	int health = 0;
	int maxHealth = 0;
	public String addHealth(LivingEntity entity, String nametag)
	{
		if(!isEnabled())
			return nametag;
		
		health = (int)entity.getHealth() / 2;
		maxHealth = (int) entity.getMaximumHealth() / 2;
		return nametag + " " + getColor(health) + health + "/" + maxHealth + "❤";
	}
	
	private String getColor(int health)
	{
		if(health <= maxHealth * 0.25)
			return "\u00a74";
		
		if(health <= maxHealth * 0.5)
			return "\u00a76";
		
		if(health <= maxHealth * 0.75)
			return "\u00a7e";
		
		return "\u00a7a";
	}
}
