/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.wurstclient.gametest.WurstClientTestHelper;
import net.wurstclient.gametest.WurstTest;

public enum AltManagerTest
{
	;
	
	// TODO: Test more of AltManager
	
	public static void testAltManagerButton(ClientGameTestContext context)
	{
		WurstTest.LOGGER.info("Checking AltManager button position");
		
		context.runOnClient(mc -> {
			if(!(mc.gui.screen() instanceof TitleScreen))
				throw new RuntimeException("Not on the title screen");
		});
		
		Button multiplayerButton =
			context.computeOnClient(mc -> findButton(mc, "menu.multiplayer"));
		Button realmsButton =
			context.computeOnClient(mc -> findButton(mc, "menu.online"));
		Button altManagerButton =
			context.computeOnClient(mc -> findButton(mc, "Alt Manager"));
		
		checkButtonPosition(altManagerButton, realmsButton.getRight() + 4,
			multiplayerButton.getBottom() + 4);
		
		WurstClientTestHelper.assertScreenshotEquals(context,
			"alt_manager_button", "https://i.imgur.com/jyWiuCe.png");
	}
	
	private static Button findButton(Minecraft mc, String translationKey)
	{
		String message = I18n.get(translationKey);
		
		for(Renderable drawable : mc.gui.screen().renderables)
			if(drawable instanceof Button button
				&& button.getMessage().getString().equals(message))
				return button;
			
		throw new RuntimeException(message + " button could not be found");
	}
	
	private static void checkButtonPosition(Button button, int expectedX,
		int expectedY)
	{
		String buttonName = button.getMessage().getString();
		
		if(button.getX() != expectedX)
			throw new RuntimeException(buttonName
				+ " button is at the wrong X coordinate. Expected X: "
				+ expectedX + ", actual X: " + button.getX());
		
		if(button.getY() != expectedY)
			throw new RuntimeException(buttonName
				+ " button is at the wrong Y coordinate. Expected Y: "
				+ expectedY + ", actual Y: " + button.getY());
	}
}
