/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.events.HitResultRayTraceListener;
import net.wurstclient.hack.Hack;

public final class LiquidsHack extends Hack implements HitResultRayTraceListener
{
	public LiquidsHack()
	{
		super("Liquids", "Allows you to place blocks in liquids.");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(HitResultRayTraceListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(HitResultRayTraceListener.class, this);
	}
	
	@Override
	public void onHitResultRayTrace(float float_1)
	{
		MC.crosshairTarget = MC.getCameraEntity()
			.raycast(MC.interactionManager.getReachDistance(), float_1, true);
	}
}
