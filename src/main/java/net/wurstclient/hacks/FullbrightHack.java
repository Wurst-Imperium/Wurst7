/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.options.GameOptions;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"NightVision", "full bright", "brightness", "night vision"})
public final class FullbrightHack extends Hack implements UpdateListener
{
	private final CheckboxSetting fade = new CheckboxSetting("Fade", true);
	
	public FullbrightHack()
	{
		super("Fullbright", "Allows you to see in the dark.");
		setCategory(Category.RENDER);
		addSetting(fade);
		
		WURST.getEventManager().add(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(isEnabled())
			approachGamma(16);
		else
			approachGamma(0.5);
	}
	
	private void approachGamma(double target)
	{
		GameOptions options = MC.options;
		
		if(!fade.isChecked() || Math.abs(options.gamma - target) <= 0.5)
		{
			options.gamma = target;
			return;
		}
		
		if(options.gamma < target)
			options.gamma += 0.5;
		else
			options.gamma -= 0.5;
	}
}
