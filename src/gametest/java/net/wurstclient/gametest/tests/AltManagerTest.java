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
import net.wurstclient.gametest.WurstTest;

public enum AltManagerTest
{
	;
	
	// TODO: Test more of AltManager
	
	public static void testAltManagerButton(ClientGameTestContext context)
	{
		WurstTest.LOGGER.info("Checking AltManager button position");
		
		context.runOnClient(mc -> {
			if(!(mc.screen instanceof TitleScreen))
				throw new RuntimeException("Not on the title screen");
			
			Button multiplayerButton = findButton(mc, "menu.multiplayer");
			Button realmsButton = findButton(mc, "menu.online");
			Button altManagerButton = findButton(mc, "Alt Manager");
			
			checkButtonPosition(altManagerButton, realmsButton.getRight() + 4,
				multiplayerButton.getBottom() + 4);
		});
	}
	
	/**
	 * Returns the first button on the current screen that has the given
	 * translation key, or fails if not found.
	 *
	 * <p>
	 * For non-translated buttons, the translationKey parameter should be the
	 * raw button text instead.
	 */
	public static Button findButton(Minecraft mc, String translationKey)
	{
		String message = I18n.get(translationKey);
		
		for(Renderable drawable : mc.screen.renderables)
			if(drawable instanceof Button button
				&& button.getMessage().getString().equals(message))
				return button;
			
		throw new RuntimeException(message + " button could not be found");
	}
	
	/**
	 * Looks for the given button at the given coordinates and fails if it is
	 * not there.
	 */
	public static void checkButtonPosition(Button button, int expectedX,
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
