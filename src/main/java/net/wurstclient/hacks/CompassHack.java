/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.List;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.IndicatorListSetting;

@SearchTags({"show position", "ShowPosition", "GuiHack", "AdvancedGui",
	"gui", "position show", "durability hack", "DurabilityHack", "show durability", "Armor Durability"})
public final class CompassHack extends Hack
{
	private int indicatorLength = 1;
	private final SliderSetting size = new SliderSetting("Gui scale",
		"Scale that the Compass should appear on the screen.\n",
        1, 0.2, 2, 0.1, v -> (float)v + "");
    private final IndicatorListSetting compassIndicators = 
		new IndicatorListSetting("Indicators", "The points you want to be able to see n your compass", "Green_0_0");
    
    
    	public CompassHack()
	{
		super("CompassHack", "Shows positions and there diretion from the player");
		setCategory(Category.RENDER);
		addSetting(compassIndicators);
		addSetting(size);
    }
    public boolean indicatorsHaveChanged()
	{
		if(compassIndicators.getLength()!=indicatorLength)
		{
			indicatorLength=compassIndicators.getLength();
			return true;
		}
		else
		{
			return false;
		}
	}
	public List<String> currentIndicators()
	{
		return compassIndicators.getIndicators();
    }
    
    public float getSize()
	{
		return size.getValueF();
	}
}