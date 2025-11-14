/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
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
		
		Screen screen = WurstClient.MC.screen;
		
		return screen instanceof AbstractContainerScreen
			&& !(screen instanceof EffectRenderingInventoryScreen);
	}
}
