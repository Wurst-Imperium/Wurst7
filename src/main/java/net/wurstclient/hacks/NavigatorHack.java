/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.navigator.NavigatorMainScreen;

@DontSaveState
@DontBlock
@SearchTags({"ClickGUI", "click gui", "SearchGUI", "search gui", "HackMenu",
	"hack menu"})
public final class NavigatorHack extends Hack
{
	public NavigatorHack()
	{
		super("Navigator","搜索面板(触控面板),用于搜索Wurst的功能");
	}
	
	@Override
	public void onEnable()
	{
		if(!(MC.currentScreen instanceof NavigatorMainScreen))
			MC.setScreen(new NavigatorMainScreen());
		
		setEnabled(false);
	}
}
