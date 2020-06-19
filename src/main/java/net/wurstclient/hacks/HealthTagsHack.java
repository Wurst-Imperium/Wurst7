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
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"health tags"})
public final class HealthTagsHack extends Hack
{
	CheckboxSetting showMaxHealth = new CheckboxSetting("Show max health", "Shows players' max health.", false);
	CheckboxSetting colorByPercentage = new CheckboxSetting("Color by percentage", "Color health display based on percentage of max health instead of fixed values.", false);

	public HealthTagsHack()
	{
		super("HealthTags", "Shows the health of players in their nametags.");

		setCategory(Category.RENDER);
	}
	
	public String addHealth(LivingEntity entity, String nametag)
	{
		if(!isEnabled())
			return nametag;

		float health = entity.getHealth();
		float maxHealth = entity.getMaximumHealth();

		if (showMaxHealth.isChecked())
			return nametag + " " + getColor(health, maxHealth) + (int)health + " " + (int)maxHealth;

		return nametag + " " + getColor(health, maxHealth) + health;
	}
	
	private String getColor(float health, float maxHealth)
	{
		final String colorDarkRed = "\u00a74";
		final String colorGold = "\u00a76";
		final String colorYellow = "\u00a7e";
		final String colorGreen = "\u00a7a";

		if (colorByPercentage.isChecked())
		{
			float percentage = health / maxHealth;

			if (percentage <= 0.25)
				return colorDarkRed;

			if (percentage <= 0.5)
				return colorGold;

			if (percentage <= 0.75)
				return colorYellow;

			return colorGreen;
		}
		else
		{
			if (health <= 5)
				return "\u00a74";

			if (health <= 10)
				return "\u00a76";

			if (health <= 15)
				return "\u00a7e";

			return "\u00a7a";
		}
	}
}
