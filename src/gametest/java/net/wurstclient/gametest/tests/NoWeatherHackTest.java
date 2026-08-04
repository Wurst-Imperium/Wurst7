/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.wurstclient.gametest.SingleplayerTest;

public final class NoWeatherHackTest extends SingleplayerTest
{
	public NoWeatherHackTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing NoWeather hack");
		
		// Setup (rainy morning, looking straight up)
		rotatePlayer(-90);
		setTime(0);
		setRain(true);
		context.waitTick();// to update EnvironmentAttributeSystem/Probe cache
		assertScreenshotEquals("noweather_raining_setup",
			"https://i.imgur.com/LHBNoxk.png");
		
		// Enable NoWeather
		runWurstCommand("t NoWeather on");
		context.waitTick();// to update EnvironmentAttributeSystem/Probe cache
		assertScreenshotEquals("noweather_rain_disabled",
			"https://i.imgur.com/YNFnIPj.png");
		
		// Enable time changing
		runWurstCommand("setcheckbox NoWeather change_world_time on");
		context.waitTick();// to update EnvironmentAttributeSystem/Probe cache
		assertScreenshotEquals("noweather_time_6000",
			"https://i.imgur.com/wxaAvAi.png");
		
		// Change time to 18000 (midnight)
		runWurstCommand("setslider NoWeather time 18000");
		context.waitTick();// to update EnvironmentAttributeSystem/Probe cache
		assertScreenshotEquals("noweather_time_18000",
			"https://i.imgur.com/6RaX1xL.png");
		
		// Change moon phase to 4
		runWurstCommand("setcheckbox NoWeather change_moon_phase on");
		runWurstCommand("setslider NoWeather moon_phase 4");
		context.waitTick();// to update EnvironmentAttributeSystem/Probe cache
		assertScreenshotEquals("noweather_moon_phase_4",
			"https://i.imgur.com/EjalAH4.png");
		
		// Clean up
		rotatePlayer(0);
		runWurstCommand("t NoWeather off");
		setTime(6000);
		setRain(false);
		clearParticles();
		context.waitTick();// to update EnvironmentAttributeSystem/Probe cache
	}
	
	private void rotatePlayer(int pitch)
	{
		runCommand("tp @s ~ ~ ~ 0 " + pitch);
		context.waitFor(mc -> mc.player.getXRot() == pitch);
		context.runOnClient(mc -> {
			mc.player.yBob = mc.player.getYRot();
			mc.player.yBobO = mc.player.yBob;
			mc.player.xBob = mc.player.getXRot();
			mc.player.xBobO = mc.player.xBob;
		});
	}
	
	private void setTime(int time)
	{
		runCommand("time set " + time);
		context.waitFor(mc -> mc.level.getDayTime() == time);
	}
	
	private void setRain(boolean on)
	{
		runCommand("weather " + (on ? "rain" : "clear"));
		server.runOnServer(s -> s.overworld().setRainLevel(on ? 1 : 0));
		context.runOnClient(mc -> mc.level.setRainLevel(on ? 1 : 0));
	}
}
