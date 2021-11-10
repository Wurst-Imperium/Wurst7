/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"FastMine", "SpeedMine", "SpeedyGonzales", "fast break",
	"fast mine", "no break delay", "speed mine", "speedy gonzales"})
public final class NoBreakDelayHack extends Hack implements UpdateListener
{
	private final CheckboxSetting disableCreative = new CheckboxSetting(
		"Disable in creative mode", true);
	
	public NoBreakDelayHack()
	{
		super("NoBreakDelay");
		setCategory(Category.BLOCKS);
		addSetting(disableCreative);
	}
	
	@Override
	protected void onEnable()
	{
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
		if (!disableCreative.isChecked()
			|| !MC.player.getAbilities().creativeMode)
			IMC.getInteractionManager().setBlockHitDelay(0);
	}
}
