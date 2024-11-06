/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.fabric.FabricClientTestHelper.*;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.tutorial.TutorialStep;

public enum WurstClientTestHelper
{
	;
	
	public static boolean testAltManagerButton(MinecraftClient mc)
	{
		System.out.println("Checking AltManager button position");
		
		if(!(mc.currentScreen instanceof TitleScreen))
			throw new RuntimeException("Not on the title screen");
		
		ButtonWidget multiplayerButton = findButton(mc, "menu.multiplayer");
		ButtonWidget realmsButton = findButton(mc, "menu.online");
		ButtonWidget altManagerButton = findButton(mc, "Alt Manager");
		
		checkButtonPosition(altManagerButton, realmsButton.getRight() + 4,
			multiplayerButton.getBottom() + 4);
		
		return true;
	}
	
	private static ButtonWidget findButton(MinecraftClient mc,
		String translationKey)
	{
		String message = I18n.translate(translationKey);
		
		for(Drawable drawable : mc.currentScreen.drawables)
			if(drawable instanceof ButtonWidget button
				&& button.getMessage().getString().equals(message))
				return button;
			
		throw new RuntimeException(message + " button could not be found");
	}
	
	private static void checkButtonPosition(ButtonWidget button, int expectedX,
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
	
	public static void setTextfieldText(int index, String text)
	{
		waitFor("Set textfield " + index + " to " + text, mc -> {
			Screen screen = mc.currentScreen;
			if(screen == null)
				return false;
			
			int currentIndex = 0;
			for(Drawable drawable : screen.drawables)
			{
				if(!(drawable instanceof TextFieldWidget textField))
					continue;
				
				if(currentIndex == index)
				{
					textField.setText(text);
					return true;
				}
				
				currentIndex++;
			}
			
			return false;
		});
	}
	
	public static void runChatCommand(String command)
	{
		submitAndWait(mc -> {
			ClientPlayNetworkHandler netHandler = mc.getNetworkHandler();
			
			// Validate command using client-side command dispatcher
			ParseResults<?> results = netHandler.getCommandDispatcher()
				.parse(command, netHandler.getCommandSource());
			
			// Command is invalid, fail the test
			if(!results.getExceptions().isEmpty())
			{
				StringBuilder errors =
					new StringBuilder("Invalid command: " + command);
				for(CommandSyntaxException e : results.getExceptions().values())
					errors.append("\n").append(e.getMessage());
				
				throw new RuntimeException(errors.toString());
			}
			
			// Command is valid, send it
			netHandler.sendChatCommand(command);
			return null;
		});
	}
	
	public static void clearChat()
	{
		submitAndWait(mc -> {
			mc.inGameHud.getChatHud().clear(true);
			return null;
		});
	}
	
	public static void dismissTutorialToasts()
	{
		submitAndWait(mc -> {
			mc.getTutorialManager().setStep(TutorialStep.NONE);
			return null;
		});
	}
}
