/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"anti knockback", "AntiVelocity", "anti velocity", "NoKnockback",
	"no knockback", "AntiKB", "anti kb"})
public final class AntiKnockbackHack extends Hack
{
	private final SliderSetting hStrength =
		new SliderSetting("Horizontal Strength",
			"How far to reduce horizontal knockback.\n"
				+ "-100% = double knockback\n" + "0% = normal knockback\n"
				+ "100% = no knockback\n" + ">100% = reverse knockback",
			1, -1, 2, 0.01, ValueDisplay.PERCENTAGE);
	
	private final SliderSetting vStrength =
		new SliderSetting("Vertical Strength",
			"How far to reduce vertical knockback.\n"
				+ "-100% = double knockback\n" + "0% = normal knockback\n"
				+ "100% = no knockback\n" + ">100% = reverse knockback",
			1, -1, 2, 0.01, ValueDisplay.PERCENTAGE);
	
	public AntiKnockbackHack()
	{
		super("AntiKnockback");
		setCategory(Category.COMBAT);
		addSetting(hStrength);
		addSetting(vStrength);
	}
	
	public Vec3 modifyKnockback(Vec3 original)
	{
		if(!isEnabled())
			return original;
		
		double verticalMultiplier = 1 - vStrength.getValue();
		double horizontalMultiplier = 1 - hStrength.getValue();
		
		return original.multiply(horizontalMultiplier, verticalMultiplier,
			horizontalMultiplier);
	}
}
