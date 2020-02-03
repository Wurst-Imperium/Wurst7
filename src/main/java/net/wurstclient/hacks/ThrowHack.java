/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class ThrowHack extends Hack implements UpdateListener
{
	private final SliderSetting amount = new SliderSetting("Amount",
		"Amount of uses per click.", 16, 2, 1000000, 1, ValueDisplay.INTEGER);
	
	public ThrowHack()
	{
		super("Throw",
			"Uses an item multiple times. Can be used to throw\n"
				+ "snowballs and eggs, spawn mobs, place minecarts, etc. in\n"
				+ "very large quantities.\n\n"
				+ "This can cause a lot of lag and even crash a server.");
		
		setCategory(Category.OTHER);
		addSetting(amount);
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + amount.getValueString() + "]";
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(IMC.getItemUseCooldown() > 0)
			return;
		
		if(!MC.options.keyUse.isPressed())
			return;
		
		for(int i = 0; i < amount.getValueI(); i++)
			IMC.rightClick();
	}
}
