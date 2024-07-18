/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.VelocityFromEntityCollisionListener;
import net.wurstclient.hack.Hack;

@SearchTags({"anti entity push", "NoEntityPush", "no entity push"})
public final class AntiEntityPushHack extends Hack
	implements VelocityFromEntityCollisionListener
{
	public AntiEntityPushHack()
	{
		super("AntiEntityPush");
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(VelocityFromEntityCollisionListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(VelocityFromEntityCollisionListener.class, this);
	}
	
	@Override
	public void onVelocityFromEntityCollision(
		VelocityFromEntityCollisionEvent event)
	{
		if(event.getEntity() == MC.player)
			event.cancel();
	}
}
