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
		runCommand("time set 0");
		runCommand("tp @s ~ ~ ~ 0 -90");
		runCommand("weather rain");
		server.runOnServer(s -> s.overworld().setRainLevel(1.0F));
		context.runOnClient(mc -> mc.level.setRainLevel(1.0F));
		context.waitTicks(10);
		assertScreenshotEquals("noweather_raining_setup",
			"https://i.imgur.com/JQVtBh7.png");
		
		// Enable NoWeather
		runWurstCommand("t NoWeather on");
		assertScreenshotEquals("noweather_rain_disabled",
			"https://i.imgur.com/YNFnIPj.png");
		
		// Enable time changing
		runWurstCommand("setcheckbox NoWeather change_world_time on");
		assertScreenshotEquals("noweather_time_6000",
			"https://i.imgur.com/wxaAvAi.png");
		
		// Change time to 18000 (midnight)
		runWurstCommand("setslider NoWeather time 18000");
		assertScreenshotEquals("noweather_time_18000",
			"https://i.imgur.com/6RaX1xL.png");
		
		// Change moon phase to 4
		runWurstCommand("setcheckbox NoWeather change_moon_phase on");
		runWurstCommand("setslider NoWeather moon_phase 4");
		assertScreenshotEquals("noweather_moon_phase_4",
			"https://i.imgur.com/EjalAH4.png");
		
		// Clean up
		runWurstCommand("t NoWeather off");
		runCommand("time set 6000");
		runCommand("weather clear");
		runCommand("tp @s ~ ~ ~ 0 0");
		server.runOnServer(s -> s.overworld().setRainLevel(0.0F));
		context.runOnClient(mc -> mc.level.setRainLevel(0.0F));
		clearParticles();
		context.waitTicks(7); // for hand to catch up with rotation
	}
}
