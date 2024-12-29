/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import java.time.Duration;

public enum XRayHackTest
{
	;
	
	public static void testXRayHack()
	{
		System.out.println("Testing X-Ray hack");
		buildTestRig();
		clearChat();
		
		// Enable X-Ray with default settings
		runWurstCommand("setcheckbox X-Ray only_show_exposed off");
		runWurstCommand("setslider X-Ray opacity 0");
		runWurstCommand("t X-Ray on");
		takeScreenshot("xray_default", Duration.ofMillis(300));
		runWurstCommand("t X-Ray off");
		clearChat();
		
		// Exposed only
		runWurstCommand("setcheckbox X-Ray only_show_exposed on");
		runWurstCommand("setslider X-Ray opacity 0");
		runWurstCommand("t X-Ray on");
		takeScreenshot("xray_exposed_only", Duration.ofMillis(300));
		runWurstCommand("t X-Ray off");
		clearChat();
		
		// Opacity mode
		runWurstCommand("setcheckbox X-Ray only_show_exposed off");
		runWurstCommand("setslider X-Ray opacity 0.5");
		runWurstCommand("t X-Ray on");
		takeScreenshot("xray_opacity", Duration.ofMillis(300));
		runWurstCommand("t X-Ray off");
		clearChat();
		
		// Exposed only + opacity
		runWurstCommand("setcheckbox X-Ray only_show_exposed on");
		runWurstCommand("setslider X-Ray opacity 0.5");
		runWurstCommand("t X-Ray on");
		takeScreenshot("xray_exposed_only_opacity", Duration.ofMillis(300));
		runWurstCommand("t X-Ray off");
		clearChat();
		
		// Clean up
		runChatCommand("fill ~-7 ~ ~-7 ~7 ~30 ~7 air");
		runWurstCommand("setcheckbox X-Ray only_show_exposed off");
		runWurstCommand("setslider X-Ray opacity 0");
		runWurstCommand("t X-Ray off");
		clearChat();
	}
	
	private static void buildTestRig()
	{
		runChatCommand("fill ~-5 ~ ~5 ~5 ~5 ~7 stone");
		
		// Hidden ores
		runChatCommand("setblock ~-4 ~1 ~6 minecraft:coal_ore");
		runChatCommand("setblock ~-2 ~1 ~6 minecraft:iron_ore");
		runChatCommand("setblock ~0 ~1 ~6 minecraft:gold_ore");
		runChatCommand("setblock ~2 ~1 ~6 minecraft:diamond_ore");
		runChatCommand("setblock ~4 ~1 ~6 minecraft:emerald_ore");
		runChatCommand("setblock ~-4 ~3 ~6 minecraft:lapis_ore");
		runChatCommand("setblock ~-2 ~3 ~6 minecraft:redstone_ore");
		runChatCommand("setblock ~0 ~3 ~6 minecraft:copper_ore");
		runChatCommand("setblock ~2 ~3 ~6 minecraft:nether_gold_ore");
		runChatCommand("setblock ~4 ~3 ~6 minecraft:nether_quartz_ore");
		
		// Partially exposed ores (air block in front)
		runChatCommand("setblock ~-4 ~1 ~5 minecraft:coal_ore");
		runChatCommand("setblock ~-2 ~1 ~5 minecraft:iron_ore");
		runChatCommand("setblock ~0 ~1 ~5 minecraft:gold_ore");
		runChatCommand("setblock ~2 ~1 ~5 minecraft:diamond_ore");
		runChatCommand("setblock ~4 ~1 ~5 minecraft:emerald_ore");
		runChatCommand("setblock ~-4 ~3 ~5 minecraft:lapis_ore");
		runChatCommand("setblock ~-2 ~3 ~5 minecraft:redstone_ore");
		runChatCommand("setblock ~0 ~3 ~5 minecraft:copper_ore");
		runChatCommand("setblock ~2 ~3 ~5 minecraft:nether_gold_ore");
		runChatCommand("setblock ~4 ~3 ~5 minecraft:nether_quartz_ore");
	}
}
