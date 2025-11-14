/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.wurstclient.WurstClient;

public enum WurstClientTestHelper
{
	;
	
	private static final AtomicInteger screenshotCounter = new AtomicInteger(0);
	
	/**
	 * Runs the given consumer on Minecraft's main thread and waits for it to
	 * complete.
	 */
	public static void submitAndWait(Consumer<Minecraft> consumer)
	{
		Minecraft mc = Minecraft.getInstance();
		mc.submit(() -> consumer.accept(mc)).join();
	}
	
	/**
	 * Runs the given function on Minecraft's main thread, waits for it to
	 * complete, and returns the result.
	 */
	public static <T> T submitAndGet(Function<Minecraft, T> function)
	{
		Minecraft mc = Minecraft.getInstance();
		return mc.submit(() -> function.apply(mc)).join();
	}
	
	/**
	 * Waits for the given duration.
	 */
	public static void wait(Duration duration)
	{
		try
		{
			Thread.sleep(duration.toMillis());
			
		}catch(InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Waits until the given condition is true, or fails if the timeout is
	 * reached.
	 */
	public static void waitUntil(String event, Predicate<Minecraft> condition,
		Duration maxDuration)
	{
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime timeout = startTime.plus(maxDuration);
		System.out.println("Waiting until " + event);
		
		while(true)
		{
			if(submitAndGet(condition::test))
			{
				double seconds =
					Duration.between(startTime, LocalDateTime.now()).toMillis()
						/ 1000.0;
				System.out.println(
					"Waiting until " + event + " took " + seconds + "s");
				break;
			}
			
			if(LocalDateTime.now().isAfter(timeout))
				throw new RuntimeException(
					"Waiting until " + event + " took too long");
			
			wait(Duration.ofMillis(50));
		}
	}
	
	/**
	 * Waits until the given condition is true, or fails after 10 seconds.
	 */
	public static void waitUntil(String event, Predicate<Minecraft> condition)
	{
		waitUntil(event, condition, Duration.ofSeconds(10));
	}
	
	/**
	 * Waits until the given screen is open, or fails after 10 seconds.
	 */
	public static void waitForScreen(Class<? extends Screen> screenClass)
	{
		waitUntil("screen " + screenClass.getName() + " is open",
			mc -> screenClass.isInstance(mc.screen));
	}
	
	/**
	 * Waits for the fading animation of the title screen to finish, or fails
	 * after 10 seconds.
	 */
	public static void waitForTitleScreenFade()
	{
		waitUntil("title screen fade is complete", mc -> {
			if(!(mc.screen instanceof TitleScreen titleScreen))
				return false;
			
			return !titleScreen.fading;
		});
	}
	
	/**
	 * Waits until the red overlay with the Mojang logo and progress bar goes
	 * away, or fails after 5 minutes.
	 */
	public static void waitForResourceLoading()
	{
		waitUntil("loading is complete", mc -> mc.getOverlay() == null,
			Duration.ofMinutes(5));
	}
	
	public static void waitForWorldLoad()
	{
		waitUntil("world is loaded",
			mc -> mc.level != null
				&& !(mc.screen instanceof LevelLoadingScreen),
			Duration.ofMinutes(30));
	}
	
	public static void waitForWorldTicks(int ticks)
	{
		long startTicks = submitAndGet(mc -> mc.level.getGameTime());
		waitUntil(ticks + " world ticks have passed",
			mc -> mc.level.getGameTime() >= startTicks + ticks,
			Duration.ofMillis(ticks * 100).plusMinutes(5));
	}
	
	public static void waitForBlock(int relX, int relY, int relZ, Block block)
	{
		BlockPos pos = submitAndGet(
			mc -> mc.player.blockPosition().offset(relX, relY, relZ));
		waitUntil(
			"block at ~" + relX + " ~" + relY + " ~" + relZ + " ("
				+ pos.toShortString() + ") is " + block,
			mc -> mc.level.getBlockState(pos).getBlock() == block);
	}
	
	/**
	 * Waits for 50ms and then takes a screenshot with the given name.
	 */
	public static void takeScreenshot(String name)
	{
		takeScreenshot(name, Duration.ofMillis(50));
	}
	
	/**
	 * Waits for the given delay and then takes a screenshot with the given
	 * name.
	 */
	public static void takeScreenshot(String name, Duration delay)
	{
		wait(delay);
		
		String count =
			String.format("%02d", screenshotCounter.incrementAndGet());
		String filename = count + "_" + name + ".png";
		File gameDir = FabricLoader.getInstance().getGameDir().toFile();
		
		submitAndWait(mc -> Screenshot.grab(gameDir, filename,
			mc.getMainRenderTarget(), message -> {}));
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
	
	/**
	 * Clicks the button with the given translation key, or fails after 10
	 * seconds.
	 *
	 * <p>
	 * For non-translated buttons, the translationKey parameter should be the
	 * raw button text instead.
	 */
	public static void clickButton(String translationKey)
	{
		String buttonText = I18n.get(translationKey);
		
		waitUntil("button saying " + buttonText + " is visible", mc -> {
			Screen screen = mc.screen;
			if(screen == null)
				return false;
			
			for(Renderable drawable : screen.renderables)
			{
				if(!(drawable instanceof AbstractWidget widget))
					continue;
				
				if(widget instanceof Button button
					&& buttonText.equals(button.getMessage().getString()))
				{
					button.onPress();
					return true;
				}
				
				if(widget instanceof CycleButton<?> button
					&& buttonText.equals(button.name.getString()))
				{
					button.onPress();
					return true;
				}
			}
			
			return false;
		});
	}
	
	/**
	 * Types the given text into the nth text field on the current screen, or
	 * fails after 10 seconds.
	 */
	public static void setTextFieldText(int index, String text)
	{
		waitUntil("text field #" + index + " is visible", mc -> {
			Screen screen = mc.screen;
			if(screen == null)
				return false;
			
			int i = 0;
			for(Renderable drawable : screen.renderables)
			{
				if(!(drawable instanceof EditBox textField))
					continue;
				
				if(i == index)
				{
					textField.setValue(text);
					return true;
				}
				
				i++;
			}
			
			return false;
		});
	}
	
	public static void closeScreen()
	{
		submitAndWait(mc -> mc.setScreen(null));
	}
	
	public static void openGameMenu()
	{
		submitAndWait(mc -> mc.setScreen(new PauseScreen(true)));
	}
	
	public static void openInventory()
	{
		submitAndWait(mc -> mc.setScreen(new InventoryScreen(mc.player)));
	}
	
	public static void toggleDebugHud()
	{
		submitAndWait(mc -> mc.gui.getDebugOverlay().toggleOverlay());
	}
	
	public static void setPerspective(CameraType perspective)
	{
		submitAndWait(mc -> mc.options.setCameraType(perspective));
	}
	
	public static void dismissTutorialToasts()
	{
		submitAndWait(mc -> mc.getTutorial().setStep(TutorialSteps.NONE));
	}
	
	public static void clearChat()
	{
		submitAndWait(mc -> mc.gui.getChat().clearMessages(true));
	}
	
	/**
	 * Runs the given chat command and waits one tick for the action to
	 * complete.
	 *
	 * <p>
	 * Do not put a / at the start of the command.
	 */
	public static void runChatCommand(String command)
	{
		System.out.println("Running command: /" + command);
		submitAndWait(mc -> {
			ClientPacketListener netHandler = mc.getConnection();
			
			// Validate command using client-side command dispatcher
			ParseResults<?> results = netHandler.getCommands().parse(command,
				netHandler.getSuggestionsProvider());
			
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
			netHandler.sendCommand(command);
		});
		waitForWorldTicks(1);
	}
	
	/**
	 * Runs the given Wurst command.
	 *
	 * <p>
	 * Do not put a . at the start of the command.
	 */
	public static void runWurstCommand(String command)
	{
		System.out.println("Running command: ." + command);
		submitAndWait(mc -> {
			WurstClient.INSTANCE.getCmdProcessor().process(command);
		});
	}
	
	/**
	 * Uses the currently held item and/or targeted block/entity, then waits
	 * one tick for the action to complete.
	 *
	 * <p>
	 * This won't work for right clicking in menus.
	 */
	public static void rightClickInGame()
	{
		submitAndWait(Minecraft::startUseItem);
		waitForWorldTicks(1);
	}
	
	public static void assertOneItemInSlot(int slot, Item item)
	{
		submitAndWait(mc -> {
			ItemStack stack = mc.player.getInventory().getItem(slot);
			if(!stack.is(item) || stack.getCount() != 1)
				throw new RuntimeException(
					"Expected 1 " + item.getDescription().getString()
						+ " at slot " + slot + ", found " + stack.getCount()
						+ " " + stack.getItem().getDescription().getString()
						+ " instead");
		});
	}
	
	public static void assertNoItemInSlot(int slot)
	{
		submitAndWait(mc -> {
			ItemStack stack = mc.player.getInventory().getItem(slot);
			if(!stack.isEmpty())
				throw new RuntimeException("Expected no item in slot " + slot
					+ ", found " + stack.getCount() + " "
					+ stack.getItem().getDescription().getString()
					+ " instead");
		});
	}
}
