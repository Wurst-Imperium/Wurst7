/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.util.Mth;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.Rotation;

@SearchTags({"head roll", "nodding", "yes"})
public final class HeadRollHack extends Hack implements UpdateListener
{
	public HeadRollHack()
	{
		super("HeadRoll");
		setCategory(Category.FUN);
	}
	
	@Override
	protected void onEnable()
	{
		// disable incompatible derps
		WURST.getHax().derpHack.setEnabled(false);
		WURST.getHax().tiredHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		float timer = MC.player.tickCount % 20 / 10F;
		float pitch = Mth.sin(timer * (float)Math.PI) * 90F;
		
		new Rotation(MC.player.getYRot(), pitch).sendPlayerLookPacket();
	}
}
