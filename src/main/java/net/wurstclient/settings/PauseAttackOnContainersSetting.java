/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.wurstclient.WurstClient;

public final class PauseAttackOnContainersSetting extends CheckboxSetting
{
	public PauseAttackOnContainersSetting(boolean checked)
	{
		super("Pause on containers",
			"description.wurst.setting.generic.pause_attack_on_containers",
			checked);
	}
	
	public PauseAttackOnContainersSetting(String name, String description,
		boolean checked)
	{
		super(name, description, checked);
	}
	
	public boolean shouldPause()
	{
		if(!isChecked())
			return false;
		
		Screen screen = WurstClient.MC.currentScreen;
		
		return screen instanceof HandledScreen
			&& !(screen instanceof AbstractInventoryScreen);
	}
}
