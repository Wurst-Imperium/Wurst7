/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.gametest.SingleplayerTest;

public final class AutoSprintHackTest extends SingleplayerTest
{
	public AutoSprintHackTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing AutoSprint hack");
		runCommand("tp @s 1 -60 0");
		context.waitFor(mc -> mc.player.getY() == -60);
		context.runOnClient(mc -> mc.options.fovEffectScale().set(0.0));
		
		runWurstCommand("t AutoSprint on");
		runWurstCommand("setcheckbox AutoSprint omnidirectional_sprint on");
		Vec3 start = context.computeOnClient(mc -> mc.player.position());
		input.holdKeyFor(GLFW.GLFW_KEY_A, 10);
		double distance = context.computeOnClient(
			mc -> mc.player.position().subtract(start).horizontalDistance());
		context.runOnClient(mc -> {
			mc.player.setDeltaMovement(Vec3.ZERO);
			mc.player.avatarState().resetBob();
		});
		
		if(distance < 2)
			throw new RuntimeException("Omnidirectional sprint only moved "
				+ distance + " blocks in 10 ticks, expected at least 2");
		
		// Clean up
		runWurstCommand("t AutoSprint off");
		runWurstCommand("setcheckbox AutoSprint omnidirectional_sprint off");
		runCommand("tp @s 0 -57 0");
		context.waitFor(mc -> mc.player.getY() == -57);
		context.runOnClient(mc -> mc.options.fovEffectScale().set(1.0));
		clearParticles();
	}
}
