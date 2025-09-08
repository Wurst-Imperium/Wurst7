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

import org.lwjgl.glfw.GLFW;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.WurstClient;

public enum WurstClientTestHelper
{
	;
	
	private static final AtomicInteger screenshotCounter = new AtomicInteger(0);
	
	/**
	 * Runs the given consumer on Minecraft's main thread and waits for it to
	 * complete.
	 */
	public static void submitAndWait(Consumer<MinecraftClient> consumer)
	{
		MinecraftClient mc = MinecraftClient.getInstance();
		mc.submit(() -> consumer.accept(mc)).join();
	}
	
	/**
	 * Runs the given function on Minecraft's main thread, waits for it to
	 * complete, and returns the result.
	 */
	public static <T> T submitAndGet(Function<MinecraftClient, T> function)
	{
		MinecraftClient mc = MinecraftClient.getInstance();
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
	public static void waitUntil(String event,
		Predicate<MinecraftClient> condition, Duration maxDuration)
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
	public static void waitUntil(String event,
		Predicate<MinecraftClient> condition)
	{
		waitUntil(event, condition, Duration.ofSeconds(10));
	}
	
	/**
	 * Waits until the given screen is open, or fails after 10 seconds.
	 */
	public static void waitForScreen(Class<? extends Screen> screenClass)
	{
		waitUntil("screen " + screenClass.getName() + " is open",
			mc -> screenClass.isInstance(mc.currentScreen));
	}
	
	/**
	 * Waits for the fading animation of the title screen to finish, or fails
	 * after 10 seconds.
	 */
	public static void waitForTitleScreenFade()
	{
		waitUntil("title screen fade is complete", mc -> {
			if(!(mc.currentScreen instanceof TitleScreen titleScreen))
				return false;
			
			return !titleScreen.doBackgroundFade;
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
			mc -> mc.world != null
				&& !(mc.currentScreen instanceof LevelLoadingScreen),
			Duration.ofMinutes(30));
	}
	
	public static void waitForWorldTicks(int ticks)
	{
		long startTicks = submitAndGet(mc -> mc.world.getTime());
		waitUntil(ticks + " world ticks have passed",
			mc -> mc.world.getTime() >= startTicks + ticks,
			Duration.ofMillis(ticks * 100).plusMinutes(5));
	}
	
	public static void waitForBlock(int relX, int relY, int relZ, Block block)
	{
		BlockPos pos =
			submitAndGet(mc -> mc.player.getBlockPos().add(relX, relY, relZ));
		waitUntil(
			"block at ~" + relX + " ~" + relY + " ~" + relZ + " ("
				+ pos.toShortString() + ") is " + block,
			mc -> mc.world.getBlockState(pos).getBlock() == block);
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
		
		submitAndWait(mc -> ScreenshotRecorder.saveScreenshot(gameDir, filename,
			mc.getFramebuffer(), 1, message -> {}));
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
		String buttonText = I18n.translate(translationKey);
		MouseInput pressContext = new MouseInput(GLFW.GLFW_KEY_UNKNOWN, 0);
		
		waitUntil("button saying " + buttonText + " is visible", mc -> {
			Screen screen = mc.currentScreen;
			if(screen == null)
				return false;
			
			for(Drawable drawable : screen.drawables)
			{
				if(!(drawable instanceof ClickableWidget widget))
					continue;
				
				if(widget instanceof ButtonWidget button
					&& buttonText.equals(button.getMessage().getString()))
				{
					button.onPress(pressContext);
					return true;
				}
				
				if(widget instanceof CyclingButtonWidget<?> button
					&& buttonText.equals(button.optionText.getString()))
				{
					button.onPress(pressContext);
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
			Screen screen = mc.currentScreen;
			if(screen == null)
				return false;
			
			int i = 0;
			for(Drawable drawable : screen.drawables)
			{
				if(!(drawable instanceof TextFieldWidget textField))
					continue;
				
				if(i == index)
				{
					textField.setText(text);
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
		submitAndWait(mc -> mc.setScreen(new GameMenuScreen(true)));
	}
	
	public static void openInventory()
	{
		submitAndWait(mc -> mc.setScreen(new InventoryScreen(mc.player)));
	}
	
	public static void toggleDebugHud()
	{
		submitAndWait(mc -> mc.debugHudEntryList.toggleF3Enabled());
	}
	
	public static void setPerspective(Perspective perspective)
	{
		submitAndWait(mc -> mc.options.setPerspective(perspective));
	}
	
	public static void dismissTutorialToasts()
	{
		submitAndWait(mc -> mc.getTutorialManager().setStep(TutorialStep.NONE));
	}
	
	public static void clearChat()
	{
		submitAndWait(mc -> mc.inGameHud.getChatHud().clear(true));
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
		submitAndWait(MinecraftClient::doItemUse);
		waitForWorldTicks(1);
	}
	
	public static void assertOneItemInSlot(int slot, Item item)
	{
		submitAndWait(mc -> {
			ItemStack stack = mc.player.getInventory().getStack(slot);
			if(!stack.isOf(item) || stack.getCount() != 1)
				throw new RuntimeException(
					"Expected 1 " + item.getName().getString() + " at slot "
						+ slot + ", found " + stack.getCount() + " "
						+ stack.getItem().getName().getString() + " instead");
		});
	}
	
	public static void assertNoItemInSlot(int slot)
	{
		submitAndWait(mc -> {
			ItemStack stack = mc.player.getInventory().getStack(slot);
			if(!stack.isEmpty())
				throw new RuntimeException("Expected no item in slot " + slot
					+ ", found " + stack.getCount() + " "
					+ stack.getItem().getName().getString() + " instead");
		});
	}
}
