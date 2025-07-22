/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.LivingEntity;
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
	public final CheckboxSetting entityHealth = new CheckboxSetting("Health", false);
	public final CheckboxSetting entityArmor = new CheckboxSetting("Armor", false);
	public final CheckboxSetting entityDistance = new CheckboxSetting("Distance", false);

	public HealthTagsHack()
	{
		super("HealthTags");
		setCategory(Category.RENDER);
		addSetting(entityHealth);
		addSetting(entityArmor);
		addSetting(entityDistance);
	}
	
	public Text addHealth(LivingEntity entity, MutableText nametag)
	{
		int health = (int)entity.getHealth();
		int armor = (int)entity.getArmor();
		int distance = (int)MC.player.distanceTo(entity); 
		
		MutableText formattedHealth = Text.literal(" ").append(Integer.toString(health)).formatted(getColor(health));
		MutableText formattedArmor = Text.literal(" ").append(Integer.toString(armor)).formatted(getColor(armor));
		MutableText formattedDistance = Text.literal(" ").append(Integer.toString(distance)).formatted(getColor(distance));

		if (entityHealth.isChecked())
			nametag.append(formattedHealth);

		if(entityArmor.isChecked())
			nametag.append(formattedArmor);

		if(entityDistance.isChecked())
			nametag.append(formattedDistance);
		
		return nametag;
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
	
	// See EntityRenderDispatcherMixin
}
