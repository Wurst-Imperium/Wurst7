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

@SearchTags({"show position", "ShowPosition", "GuiHack", "AdvancedGui",
	"gui", "position show", "durability hack", "DurabilityHack", "show durability", "Armor Durability"})
public final class GuiHack extends Hack
{
/*	private int indicatorLength = 1;*/
	private final SliderSetting size = new SliderSetting("Gui scale",
		"Scale that the GUI should appear on the screen.\n",
		1, 0.2, 2, 0.1, v -> (float)v + "");
	
	private final CheckboxSetting showPosition =
        new CheckboxSetting("Show position", true);
        
    private final CheckboxSetting showHandDurability =
        new CheckboxSetting("Show duribility of held item", true);
    private final CheckboxSetting showArmorDurability =
		new CheckboxSetting("Show duribility of armor", true);
	/*private final CheckboxSetting showCompass =
		new CheckboxSetting("Show the compass", true);
	private final IndicatorListSetting compassIndicators = 
		new IndicatorListSetting("Indicators", "The points you want to be able to see n your compass", "0_0");*/
	
	public GuiHack()
	{
		super("GuiHack", "Shows more info onscreen");
		setCategory(Category.RENDER);
		addSetting(size);
        addSetting(showPosition);
        addSetting(showHandDurability);
        addSetting(showArmorDurability);
	}
	
	public boolean showPosEnabled()
	{
		return showPosition.isChecked();
    }
    
    public boolean showHandDurabilityEnabled()
	{
		return showHandDurability.isChecked();
    }

    public boolean showArmorDurabilityEnabled()
	{
		return showArmorDurability.isChecked();
	}

	/*public boolean showCompassEnabled()
	{
		return showCompass.isChecked();
	}*/
	
	public float getSize()
	{
		return size.getValueF();
	}
	/*public boolean indicatorsHaveChanged()
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
	}*/
}
