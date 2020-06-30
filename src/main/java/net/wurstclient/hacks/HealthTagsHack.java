/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
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
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"health tags"})
public final class HealthTagsHack extends Hack
{
	CheckboxSetting showMaxHealth = new CheckboxSetting("Show max health", "Shows players' max health.", false);
	CheckboxSetting colorByPercentage = new CheckboxSetting("Color by percentage", "Color health display based on percentage of max health instead of fixed values.", false);

	public HealthTagsHack()
	{
		super("HealthTags", "Shows the health of players in their nametags.");

		addSetting(showMaxHealth);
		addSetting(colorByPercentage);

		setCategory(Category.RENDER);
	}
	
	public Text addHealth(LivingEntity entity, Text nametag)
	{
		if(!isEnabled())
			return nametag;

		// Keep as float for more precise health color calculations.
		float health = entity.getHealth();
		float maxHealth = entity.getMaxHealth();


		MutableText formattedHealth;
		if (showMaxHealth.isChecked())
		{
			formattedHealth = new LiteralText(" ").append(String.format("%.0f", health)).append("/").append(String.format("%.0f", maxHealth)).formatted(getColor(health, maxHealth));
		}
		else
		{
			formattedHealth = new LiteralText(" ").append(String.format("%.0f", health)).formatted(getColor(health, maxHealth));
		}

		return ((MutableText)nametag).append(formattedHealth);
	}
	
	private Formatting getColor(float health, float maxHealth)
	{
		if (colorByPercentage.isChecked())
		{
			float percentage = health / maxHealth;

			if (percentage <= 0.25)
				return Formatting.DARK_RED;

			if (percentage <= 0.5)
				return Formatting.GOLD;

			if (percentage <= 0.75)
				return Formatting.YELLOW;

		}
		else
		{
			if(health <= 5)
				return Formatting.DARK_RED;

			if(health <= 10)
				return Formatting.GOLD;

			if(health <= 15)
				return Formatting.YELLOW;

		}
		return Formatting.GREEN;

	}

}
