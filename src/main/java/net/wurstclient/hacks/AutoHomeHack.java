/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"auto home", "AutoHome", "auto tp home", "auto teleport home"})
public final class AutoHomeHack extends Hack implements UpdateListener
{
	private final SliderSetting health = new SliderSetting("Health",
		"Goes to your home used for /home when your health reaches this value or falls below it.",
		6.5, 0.5, 9.5, 0.5, ValueDisplay.DECIMAL);

	public AutoHomeHack()
	{
		super("AutoHome");
		
		setCategory(Category.COMBAT);
		addSetting(health);
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
			// check if the player should teleport home
		if(MC.player.getHealth() > health.getValueF() * 2F)
			return;

		MC.getNetworkHandler().sendChatCommand("home");

			// Disables once the player teleports
		setEnabled(false);
	}
}
