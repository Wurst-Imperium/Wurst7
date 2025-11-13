/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"health tags"})
public final class HealthTagsHack extends Hack
{
	public HealthTagsHack()
	{
		super("HealthTags");
		setCategory(Category.RENDER);
	}
	
	public Component addHealth(LivingEntity entity, MutableComponent nametag)
	{
		if(!isEnabled())
			return nametag;
		
		int health = (int)entity.getHealth();
		
		MutableComponent formattedHealth = Component.literal(" ")
			.append(Integer.toString(health)).withStyle(getColor(health));
		return nametag.append(formattedHealth);
	}
	
	private ChatFormatting getColor(int health)
	{
		if(health <= 5)
			return ChatFormatting.DARK_RED;
		
		if(health <= 10)
			return ChatFormatting.GOLD;
		
		if(health <= 15)
			return ChatFormatting.YELLOW;
		
		return ChatFormatting.GREEN;
	}
	
	// See EntityRendererMixin
}
