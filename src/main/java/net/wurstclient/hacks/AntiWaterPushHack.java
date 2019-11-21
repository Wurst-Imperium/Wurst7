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
import net.wurstclient.events.VelocityFromFluidListener;
import net.wurstclient.hack.Hack;

@SearchTags({"anti water push", "NoWaterPush", "no water push"})
public final class AntiWaterPushHack extends Hack
	implements VelocityFromFluidListener
{
	public AntiWaterPushHack()
	{
		super("AntiWaterPush", "Prevents you from getting pushed by water.");
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(VelocityFromFluidListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(VelocityFromFluidListener.class, this);
	}
	
	@Override
	public void onVelocityFromFluid(VelocityFromFluidEvent event)
	{
		event.cancel();
	}
}
