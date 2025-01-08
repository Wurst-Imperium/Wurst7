/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

public enum AltManagerTest
{
	;
	
	public static void testAltManagerButton(MinecraftClient mc)
	{
		System.out.println("Checking AltManager button position");
		
		if(!(mc.currentScreen instanceof TitleScreen))
			throw new RuntimeException("Not on the title screen");
		
		ButtonWidget multiplayerButton = findButton(mc, "menu.multiplayer");
		ButtonWidget realmsButton = findButton(mc, "menu.online");
		ButtonWidget altManagerButton = findButton(mc, "Alt Manager");
		
		checkButtonPosition(altManagerButton, realmsButton.getRight() + 4,
			multiplayerButton.getBottom() + 4);
	}
}
