/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.SearchTags;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.navigator.NavigatorMainScreen;

@SearchTags({"ClickGUI", "click gui", "SearchGUI", "search gui", "HackMenu",
	"hack menu"})
@DontSaveState
public final class NavigatorHack extends Hack
{
	public NavigatorHack()
	{
		super("Navigator",
			"A searchable GUI that learns your preferences over time.");
	}
	
	@Override
	public void onEnable()
	{
		if(!(MC.currentScreen instanceof NavigatorMainScreen))
			MC.openScreen(new NavigatorMainScreen());
		
		setEnabled(false);
	}
}
