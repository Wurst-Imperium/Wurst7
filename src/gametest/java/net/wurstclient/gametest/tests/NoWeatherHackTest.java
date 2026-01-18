/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import static net.wurstclient.gametest.WurstClientTestHelper.*;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.wurstclient.gametest.WurstTest;

public enum NoWeatherHackTest
{
	;
	
	public static void testNoWeatherHack(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		WurstTest.LOGGER.info("Testing NoWeather hack");
		TestServerContext server = spContext.getServer();
		
		// Setup (rainy morning, looking straight up)
		runCommand(server, "time set 0");
		runCommand(server, "tp @s ~ ~ ~ 0 -90");
		runCommand(server, "weather rain");
		server.runOnServer(s -> s.overworld().setRainLevel(1.0F));
		context.runOnClient(mc -> mc.level.setRainLevel(1.0F));
		context.waitTicks(10);
		assertScreenshotEquals(context, "noweather_raining_setup",
			"https://i.imgur.com/JQVtBh7.png");
		
		// Enable NoWeather
		runWurstCommand(context, "t NoWeather on");
		assertScreenshotEquals(context, "noweather_rain_disabled",
			"https://i.imgur.com/YNFnIPj.png");
		
		// Enable time changing
		runWurstCommand(context, "setcheckbox NoWeather change_world_time on");
		assertScreenshotEquals(context, "noweather_time_6000",
			"https://i.imgur.com/wxaAvAi.png");
		
		// Change time to 18000 (midnight)
		runWurstCommand(context, "setslider NoWeather time 18000");
		assertScreenshotEquals(context, "noweather_time_18000",
			"https://i.imgur.com/6RaX1xL.png");
		
		// Change moon phase to 4
		runWurstCommand(context, "setcheckbox NoWeather change_moon_phase on");
		runWurstCommand(context, "setslider NoWeather moon_phase 4");
		assertScreenshotEquals(context, "noweather_moon_phase_4",
			"https://i.imgur.com/EjalAH4.png");
		
		// Clean up
		runWurstCommand(context, "t NoWeather off");
		runCommand(server, "time set 6000");
		runCommand(server, "weather clear");
		runCommand(server, "tp @s ~ ~ ~ 0 0");
		server.runOnServer(s -> s.overworld().setRainLevel(0.0F));
		context.runOnClient(mc -> mc.level.setRainLevel(0.0F));
		clearParticles(context);
		context.waitTicks(7); // for hand animation
	}
}
