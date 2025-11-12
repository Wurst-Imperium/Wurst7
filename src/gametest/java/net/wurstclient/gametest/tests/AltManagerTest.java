/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.wurstclient.gametest.WurstTest;

public enum AltManagerTest
{
	;
	
	// TODO: Test more of AltManager
	
	public static void testAltManagerButton(ClientGameTestContext context)
	{
		WurstTest.LOGGER.info("Checking AltManager button position");
		
		context.runOnClient(mc -> {
			if(!(mc.currentScreen instanceof TitleScreen))
				throw new RuntimeException("Not on the title screen");
			
			ButtonWidget multiplayerButton = findButton(mc, "menu.multiplayer");
			ButtonWidget realmsButton = findButton(mc, "menu.online");
			ButtonWidget altManagerButton = findButton(mc, "Alt Manager");
			
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
	public static ButtonWidget findButton(MinecraftClient mc,
		String translationKey)
	{
		String message = I18n.translate(translationKey);
		
		for(Drawable drawable : mc.currentScreen.drawables)
			if(drawable instanceof ButtonWidget button
				&& button.getMessage().getString().equals(message))
				return button;
			
		throw new RuntimeException(message + " button could not be found");
	}
	
	/**
	 * Looks for the given button at the given coordinates and fails if it is
	 * not there.
	 */
	public static void checkButtonPosition(ButtonWidget button, int expectedX,
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
