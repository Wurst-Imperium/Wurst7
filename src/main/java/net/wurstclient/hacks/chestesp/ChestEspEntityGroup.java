/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chestesp;

import java.util.ArrayList;

import net.minecraft.entity.Entity;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;

public final class ChestEspEntityGroup extends ChestEspGroup
{
	private final ArrayList<Entity> entities = new ArrayList<>();
	
	public ChestEspEntityGroup(ColorSetting color, CheckboxSetting enabled)
	{
		super(color, enabled);
	}
	
	public void add(Entity e)
	{
		entities.add(e);
	}
	
	@Override
	public void clear()
	{
		entities.clear();
		super.clear();
	}
	
	public void updateBoxes(float partialTicks)
	{
		boxes.clear();
		
		for(Entity e : entities)
		{
			double offsetX = -(e.getX() - e.lastRenderX)
				+ (e.getX() - e.lastRenderX) * partialTicks;
			double offsetY = -(e.getY() - e.lastRenderY)
				+ (e.getY() - e.lastRenderY) * partialTicks;
			double offsetZ = -(e.getZ() - e.lastRenderZ)
				+ (e.getZ() - e.lastRenderZ) * partialTicks;
			
			boxes.add(e.getBoundingBox().offset(offsetX, offsetY, offsetZ));
		}
	}
}
