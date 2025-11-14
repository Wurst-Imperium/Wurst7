/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import java.time.Duration;
import java.util.function.Predicate;

import net.minecraft.client.CameraType;

public enum NoFallHackTest
{
	;
	
	public static void testNoFallHack()
	{
		System.out.println("Testing NoFall hack");
		setPerspective(CameraType.THIRD_PERSON_BACK);
		runChatCommand("gamemode survival");
		assertOnGround();
		assertPlayerHealth(health -> health == 20);
		
		// Fall 10 blocks with NoFall enabled
		runWurstCommand("t NoFall on");
		runChatCommand("tp ~ ~10 ~");
		waitForWorldTicks(5);
		waitUntil("player is on ground", mc -> mc.player.onGround());
		waitForWorldTicks(5);
		takeScreenshot("nofall_on_10_blocks", Duration.ZERO);
		assertPlayerHealth(health -> health == 20);
		
		// Fall 10 blocks with NoFall disabled
		runWurstCommand("t NoFall off");
		runChatCommand("tp ~ ~10 ~");
		waitForWorldTicks(5);
		waitUntil("player is on ground", mc -> mc.player.onGround());
		waitForWorldTicks(5);
		takeScreenshot("nofall_off_10_blocks", Duration.ZERO);
		assertPlayerHealth(health -> Math.abs(health - 13) <= 1);
		
		// Clean up
		submitAndWait(mc -> mc.player.heal(20));
		runChatCommand("gamemode creative");
		setPerspective(CameraType.FIRST_PERSON);
	}
	
	private static void assertOnGround()
	{
		if(!submitAndGet(mc -> mc.player.onGround()))
			throw new RuntimeException("Player is not on ground");
	}
	
	private static void assertPlayerHealth(Predicate<Float> healthCheck)
	{
		float health = submitAndGet(mc -> mc.player.getHealth());
		if(!healthCheck.test(health))
			throw new RuntimeException("Player's health is wrong: " + health);
		
		System.out.println("Player's health is correct: " + health);
	}
}
