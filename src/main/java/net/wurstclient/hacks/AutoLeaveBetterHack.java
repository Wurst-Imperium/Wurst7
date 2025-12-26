/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.world.entity.player.Player;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"auto leave", "proximity leave", "player detector", "panic"})
public final class AutoLeaveBetterHack extends Hack implements UpdateListener
{
	private final SliderSetting range = new SliderSetting("Player Range",
		"Leaves the server when a NON-FRIEND player gets this close to you.",
		10, // default
		1, // min
		60, // max
		1, // step
		ValueDisplay.INTEGER.withSuffix(" blocks"));
	
	public AutoLeaveBetterHack()
	{
		super("AutoLeaveBetter");
		setCategory(Category.COMBAT);
		addSetting(range);
	}

	@Override
	public String getRenderName()
	{
		return getName() + " [" + range.getValueI() + " blocks]";
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		WURST.getHax().autoReconnectHack.setEnabled(false);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.player == null || MC.level == null)
			return;
		
		double maxRange = range.getValue();
		double maxRangeSq = maxRange * maxRange;
		
		for(Player other : MC.level.players())
		{
			if(other == MC.player)
				continue;

			if(other.isRemoved() || other.getHealth() <= 0)
				continue;
			
			if(WurstClient.INSTANCE.getFriends().isFriend(other))
				continue;

			if(MC.player.distanceToSqr(other) <= maxRangeSq)
			{
				MC.level.disconnect();
				setEnabled(false);
				return;
			}
		}
	}
}
