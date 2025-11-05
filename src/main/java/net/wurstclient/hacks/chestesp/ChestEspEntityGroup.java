/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chestesp;

import java.util.ArrayList;

import net.minecraft.entity.Entity;
import net.wurstclient.util.EntityUtils;

public abstract class ChestEspEntityGroup extends ChestEspGroup
{
	private final ArrayList<Entity> entities = new ArrayList<>();
	
	protected abstract boolean matches(Entity e);
	
	public final void addIfMatches(Entity e)
	{
		if(!matches(e))
			return;
		
		entities.add(e);
	}
	
	@Override
	public final void clear()
	{
		entities.clear();
		super.clear();
	}
	
	public final void updateBoxes(float partialTicks)
	{
		boxes.clear();
		
		for(Entity e : entities)
			boxes.add(EntityUtils.getLerpedBox(e, partialTicks));
	}
}
