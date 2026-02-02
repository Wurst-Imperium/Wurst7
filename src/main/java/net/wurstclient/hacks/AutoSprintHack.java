/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"auto sprint"})
public final class AutoSprintHack extends Hack implements UpdateListener
{
	private final CheckboxSetting allDirections =
		new CheckboxSetting("Omnidirectional Sprint",
			"Sprint in all directions, not just forward.", false);
	
	private final CheckboxSetting hungry = new CheckboxSetting("Hungry Sprint",
		"Sprint even on low hunger.", false);
	
	public AutoSprintHack()
	{
		super("AutoSprint");
		setCategory(Category.MOVEMENT);
		addSetting(allDirections);
		addSetting(hungry);
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
		LocalPlayer player = MC.player;
		if(player.horizontalCollision || player.isShiftKeyDown())
			return;
		
		if(player.isInWater() || player.isUnderWater())
			return;
		
		if(!allDirections.isChecked() && player.zza <= 0)
			return;
		
		if(player.input.getMoveVector().length() <= 1e-5F)
			return;
		
		player.setSprinting(true);
	}
	
	public boolean shouldOmniSprint()
	{
		return isEnabled() && allDirections.isChecked();
	}
	
	public boolean shouldSprintHungry()
	{
		return isEnabled() && hungry.isChecked();
	}
}
