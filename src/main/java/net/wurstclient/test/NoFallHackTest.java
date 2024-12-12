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

import net.minecraft.client.option.Perspective;

public enum NoFallHackTest
{
	;
	
	public static void testNoFallHack()
	{
		System.out.println("Testing NoFall hack");
		setPerspective(Perspective.THIRD_PERSON_BACK);
		runChatCommand("gamemode survival");
		assertOnGround();
		assertPlayerHealth(20);
		
		// Fall 10 blocks with NoFall enabled
		runWurstCommand("t NoFall on");
		runChatCommand("tp ~ ~10 ~");
		waitUntil("player is on ground", mc -> mc.player.isOnGround());
		waitForWorldTicks(1);
		takeScreenshot("nofall_on_10_blocks", Duration.ZERO);
		assertPlayerHealth(20);
		
		// Fall 10 blocks with NoFall disabled
		runWurstCommand("t NoFall off");
		runChatCommand("tp ~ ~10 ~");
		waitUntil("player is on ground", mc -> mc.player.isOnGround());
		waitForWorldTicks(1);
		takeScreenshot("nofall_off_10_blocks", Duration.ZERO);
		assertPlayerHealth(13);
		
		// Clean up
		submitAndWait(mc -> mc.player.heal(20));
		runChatCommand("gamemode creative");
		setPerspective(Perspective.FIRST_PERSON);
	}
	
	private static void assertOnGround()
	{
		if(!submitAndGet(mc -> mc.player.isOnGround()))
			throw new RuntimeException("Player is not on ground");
	}
	
	private static void assertPlayerHealth(float expectedHealth)
	{
		float actualHealth = submitAndGet(mc -> mc.player.getHealth());
		if(actualHealth != expectedHealth)
			throw new RuntimeException("Player's health is wrong. Expected: "
				+ expectedHealth + ", actual: " + actualHealth);
	}
}
