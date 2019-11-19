/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.KnockbackListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"anti knockback", "AntiVelocity", "anti velocity", "NoKnockback",
	"no knockback", "AntiKB", "anti kb"})
public final class AntiKnockbackHack extends Hack implements KnockbackListener
{
	private final SliderSetting strength = new SliderSetting("Strength",
		"How far to reduce knockback.\n" + "100% = no knockback", 1, 0.01, 1,
		0.01, ValueDisplay.PERCENTAGE);
	
	public AntiKnockbackHack()
	{
		super("AntiKnockback",
			"Prevents you from getting pushed by players and mobs.");
		setCategory(Category.COMBAT);
		addSetting(strength);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(KnockbackListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(KnockbackListener.class, this);
	}
	
	@Override
	public void onKnockback(KnockbackEvent event)
	{
		double multiplier = 1 - strength.getValue();
		event.setX(event.getDefaultX() * multiplier);
		event.setY(event.getDefaultY() * multiplier);
		event.setZ(event.getDefaultZ() * multiplier);
	}
}
