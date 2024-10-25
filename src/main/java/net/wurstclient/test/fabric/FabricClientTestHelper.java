/*
 * Copied from https://github.com/FabricMC/fabric/blob/
 * f17fc976e9d0d6fe6fc7303fb25ca7b24d122c98/fabric-api-base/src/testmodClient/
 * java/net/fabricmc/fabric/test/base/client/FabricClientTestHelper.java
 * and formatted to match our code style.
 *
 * This isn't a part of Fabric's public API (yet), so for now the only way to
 * use it is to make a copy.
 */
/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wurstclient.test.fabric;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;

// Provides thread safe utils for interacting with a running game.
public final class FabricClientTestHelper
{
	public static void waitForLoadingComplete()
	{
		waitFor("Loading to complete", client -> client.getOverlay() == null,
			Duration.ofMinutes(5));
	}
	
	public static void waitForScreen(Class<? extends Screen> screenClass)
	{
		waitFor("Screen %s".formatted(screenClass.getName()),
			client -> client.currentScreen != null
				&& client.currentScreen.getClass() == screenClass);
	}
	
	public static void openGameMenu()
	{
		setScreen(client -> new GameMenuScreen(true));
		waitForScreen(GameMenuScreen.class);
	}
	
	public static void openInventory()
	{
		setScreen(client -> new InventoryScreen(
			Objects.requireNonNull(client.player)));
		
		boolean creative = submitAndWait(
			client -> Objects.requireNonNull(client.player).isCreative());
		waitForScreen(
			creative ? CreativeInventoryScreen.class : InventoryScreen.class);
	}
	
	public static void closeScreen()
	{
		setScreen(client -> null);
	}
	
	private static void setScreen(
		Function<MinecraftClient, Screen> screenSupplier)
	{
		submit(client -> {
			client.setScreen(screenSupplier.apply(client));
			return null;
		});
	}
	
	public static void takeScreenshot(String name)
	{
		takeScreenshot(name, Duration.ofMillis(50));
	}
	
	public static void takeScreenshot(String name, Duration delay)
	{
		// Allow time for any screens to open
		waitFor(delay);
		
		submitAndWait(client -> {
			ScreenshotRecorder.saveScreenshot(
				FabricLoader.getInstance().getGameDir().toFile(), name + ".png",
				client.getFramebuffer(), message -> {});
			return null;
		});
	}
	
	public static void clickScreenButton(String translationKey)
	{
		final String buttonText = Text.translatable(translationKey).getString();
		
		waitFor("Click button" + buttonText, client -> {
			final Screen screen = client.currentScreen;
			
			if(screen == null)
				return false;
			
			// Replaced the accessor with an access widener
			for(Drawable drawable : screen.drawables)
			{
				if(drawable instanceof PressableWidget pressableWidget
					&& pressMatchingButton(pressableWidget, buttonText))
					return true;
				
				if(drawable instanceof Widget widget)
					widget.forEachChild(clickableWidget -> pressMatchingButton(
						clickableWidget, buttonText));
			}
			
			// Was unable to find the button to press
			return false;
		});
	}
	
	private static boolean pressMatchingButton(ClickableWidget widget,
		String text)
	{
		if(widget instanceof ButtonWidget buttonWidget
			&& text.equals(buttonWidget.getMessage().getString()))
		{
			buttonWidget.onPress();
			return true;
		}
		
		// Replaced the accessor with an access widener
		if(widget instanceof CyclingButtonWidget<?> buttonWidget
			&& text.equals(buttonWidget.optionText.getString()))
		{
			buttonWidget.onPress();
			return true;
		}
		
		return false;
	}
	
	public static void waitForWorldTicks(long ticks)
	{
		// Wait for the world to be loaded and get the start ticks
		waitFor("World load",
			client -> client.world != null
				&& !(client.currentScreen instanceof LevelLoadingScreen),
			Duration.ofMinutes(30));
		final long startTicks = submitAndWait(client -> client.world.getTime());
		waitFor("World load", client -> Objects.requireNonNull(client.world)
			.getTime() > startTicks + ticks, Duration.ofMinutes(10));
	}
	
	public static void enableDebugHud()
	{
		submitAndWait(client -> {
			client.inGameHud.getDebugHud().toggleDebugHud();
			return null;
		});
	}
	
	public static void setPerspective(Perspective perspective)
	{
		submitAndWait(client -> {
			client.options.setPerspective(perspective);
			return null;
		});
	}
	
	// Removed connectToServer()
	
	public static void waitForTitleScreenFade()
	{
		waitFor("Title screen fade", client -> {
			if(!(client.currentScreen instanceof TitleScreen titleScreen))
				return false;
			
			// Replaced the accessor with an access widener
			return !titleScreen.doBackgroundFade;
		});
	}
	
	private static void waitFor(String what,
		Predicate<MinecraftClient> predicate)
	{
		waitFor(what, predicate, Duration.ofSeconds(10));
	}
	
	private static void waitFor(String what,
		Predicate<MinecraftClient> predicate, Duration timeout)
	{
		final LocalDateTime end = LocalDateTime.now().plus(timeout);
		
		while(true)
		{
			boolean result = submitAndWait(predicate::test);
			
			if(result)
				break;
			
			if(LocalDateTime.now().isAfter(end))
				throw new RuntimeException("Timed out waiting for " + what);
			
			waitFor(Duration.ofMillis(50));
		}
	}
	
	private static void waitFor(Duration duration)
	{
		try
		{
			Thread.sleep(duration.toMillis());
			
		}catch(InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("resource")
	private static <T> CompletableFuture<T> submit(
		Function<MinecraftClient, T> function)
	{
		return MinecraftClient.getInstance()
			.submit(() -> function.apply(MinecraftClient.getInstance()));
	}
	
	public static <T> T submitAndWait(Function<MinecraftClient, T> function)
	{
		return submit(function).join();
	}
}
