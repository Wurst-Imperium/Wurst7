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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.wurstclient.gametest.SingleplayerTest;

public final class AttributeSwapMechanicTest extends SingleplayerTest
{
	public AttributeSwapMechanicTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing if attribute swapping still works");
		
		runCommand("gamemode survival");
		runCommand("item replace entity @s hotbar.0 with diamond_sword");
		runCommand("item replace entity @s hotbar.1 with stick");
		context.waitTicks(2);
		clearToasts();
		waitForHandSwing();
		
		IronGolem golem = spawnGolem();
		context.waitTick();
		
		// Same tick: switch to stick, then attack. The server should use the
		// stick item but still have the sword's cached attack damage.
		input.holdKey(options -> options.keyHotbarSlots[1]);
		input.holdKey(options -> options.keyAttack);
		context.waitTick();
		input.releaseKey(options -> options.keyHotbarSlots[1]);
		input.releaseKey(options -> options.keyAttack);
		context.waitTick();
		
		float golemDamage = golem.getMaxHealth() - golem.getHealth();
		if(golemDamage < 5F)
			throw new RuntimeException(
				"Attribute swapping seems to be patched: golem only took "
					+ golemDamage + " damage");
		
		int swordLostDurability = context.computeOnClient(
			mc -> mc.player.getInventory().getItem(0).getDamageValue());
		if(swordLostDurability != 0)
			throw new RuntimeException(
				"Attribute swapping seems to be patched: sword lost "
					+ swordLostDurability + " durability");
		
		// Clean up
		golem.discard();
		clearParticles();
		input.pressKey(options -> options.keyHotbarSlots[0]);
		clearInventory();
		runCommand("gamemode creative");
		waitForHandSwing();
	}
	
	private IronGolem spawnGolem()
	{
		return server.computeOnServer(s -> {
			IronGolem golem = EntityType.IRON_GOLEM.create(s.overworld(),
				EntitySpawnReason.COMMAND);
			golem.setPos(0.5, -57, 2);
			golem.setNoAi(true);
			s.overworld().addFreshEntity(golem);
			return golem;
		});
	}
}
