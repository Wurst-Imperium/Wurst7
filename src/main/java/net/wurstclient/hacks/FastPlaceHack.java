/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

public final class FastPlaceHack extends Hack implements UpdateListener
{
	public FastPlaceHack()
	{
		super("FastPlace", "Allows you to place blocks 5 times faster.\n"
			+ "Tip: This can speed up other hacks like AutoBuild.");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getEventManager().add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		WURST.getEventManager().remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		IMC.setItemUseCooldown(0);
	}
}
