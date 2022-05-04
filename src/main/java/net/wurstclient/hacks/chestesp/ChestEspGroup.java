/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chestesp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.util.math.Box;
import net.wurstclient.settings.ColorSetting;

public abstract class ChestEspGroup
{
	protected final ArrayList<Box> boxes = new ArrayList<>();
	private final ColorSetting color;
	
	public ChestEspGroup(ColorSetting color)
	{
		this.color = Objects.requireNonNull(color);
	}
	
	public void clear()
	{
		boxes.clear();
	}
	
	public ColorSetting getSetting()
	{
		return color;
	}
	
	public float[] getColorF()
	{
		return color.getColorF();
	}
	
	public List<Box> getBoxes()
	{
		return Collections.unmodifiableList(boxes);
	}
}
