/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"no background", "NoGuiBackground", "no gui background",
	"NoGradient", "no gradient"})
public final class NoBackgroundHack extends Hack
{
	public final CheckboxSetting allGuis = new CheckboxSetting("All GUIs",
		"Removes the background for all GUIs, not just inventories.", false);
	
	public NoBackgroundHack()
	{
		super("NoBackground");
		setCategory(Category.RENDER);
		addSetting(allGuis);
	}
	
	public boolean shouldCancelBackground(Screen screen)
	{
		if(!isEnabled())
			return false;
		
		if(MC.world == null)
			return false;
		
		if(!allGuis.isChecked() && !(screen instanceof HandledScreen))
			return false;
		
		return true;
	}
	
	// See ScreenMixin.onRenderBackground()
}
