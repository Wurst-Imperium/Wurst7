/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chestesp.groups;

import java.awt.Color;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.wurstclient.hacks.chestesp.ChestEspBlockGroup;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;

public final class CraftersGroup extends ChestEspBlockGroup
{
	@Override
	protected CheckboxSetting createIncludeSetting()
	{
		return new CheckboxSetting("Include crafters", false);
	}
	
	@Override
	protected ColorSetting createColorSetting()
	{
		return new ColorSetting("Crafter color",
			"Crafters will be highlighted in this color.", Color.WHITE);
	}
	
	@Override
	protected boolean matches(BlockEntity be)
	{
		return be instanceof CrafterBlockEntity;
	}
}
