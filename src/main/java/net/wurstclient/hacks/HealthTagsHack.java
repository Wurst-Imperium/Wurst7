/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
	
	public Text addHealth(LivingEntity entity, Text nametag)
	{
		if(!isEnabled())
			return nametag;
		
		int health = (int)entity.getHealth();
		
		MutableText formattedHealth = new LiteralText(" ")
			.append(Integer.toString(health)).formatted(getColor(health));
		return ((MutableText)nametag).append(formattedHealth);
	}
	
	private Formatting getColor(int health)
	{
		if(health <= 5)
			return Formatting.DARK_RED;
		
		if(health <= 10)
			return Formatting.GOLD;
		
		if(health <= 15)
			return Formatting.YELLOW;
		
		return Formatting.GREEN;
	}
}
