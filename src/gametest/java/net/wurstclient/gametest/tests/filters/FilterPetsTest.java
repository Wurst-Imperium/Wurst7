/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests.filters;

import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.wurstclient.gametest.tests.EntityFilterTest;
import net.wurstclient.settings.filterlists.EntityFilterList.EntityFilter;
import net.wurstclient.settings.filters.FilterPetsSetting;

public final class FilterPetsTest extends EntityFilterTest
{
	public FilterPetsTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing pet filter");
		Supplier<EntityFilter> filter = () -> new FilterPetsSetting("", true);
		
		// Normal pets - filter out if tamed
		assertFilterResult("Cat (tamed)", filter,
			() -> spawnTamedAnimal(EntityType.CAT), false);
		assertFilterResult("Cat (untamed)", filter,
			() -> spawnEntity(EntityType.CAT), true);
		assertFilterResult("Nautilus (tamed)", filter,
			() -> spawnTamedAnimal(EntityType.NAUTILUS), false);
		assertFilterResult("Nautilus (untamed)", filter,
			() -> spawnEntity(EntityType.NAUTILUS), true);
		assertFilterResult("Parrot (tamed)", filter,
			() -> spawnTamedAnimal(EntityType.PARROT), false);
		assertFilterResult("Parrot (untamed)", filter,
			() -> spawnEntity(EntityType.PARROT), true);
		assertFilterResult("Wolf (tamed)", filter,
			() -> spawnTamedAnimal(EntityType.WOLF), false);
		assertFilterResult("Wolf (untamed)", filter,
			() -> spawnEntity(EntityType.WOLF), true);
		assertFilterResult("Zombie Nautilus (tamed)", filter,
			() -> spawnTamedAnimal(EntityType.ZOMBIE_NAUTILUS), false);
		assertFilterResult("Zombie Nautilus (untamed)", filter,
			() -> spawnEntity(EntityType.ZOMBIE_NAUTILUS), true);
		
		// Normal horse-likes - filter out if tamed
		assertFilterResult("Donkey (tamed)", filter,
			() -> spawnTamedEquine(EntityType.DONKEY), false);
		assertFilterResult("Donkey (untamed)", filter,
			() -> spawnEntity(EntityType.DONKEY), true);
		assertFilterResult("Horse (tamed)", filter,
			() -> spawnTamedEquine(EntityType.HORSE), false);
		assertFilterResult("Horse (untamed)", filter,
			() -> spawnEntity(EntityType.HORSE), true);
		assertFilterResult("Llama (tamed)", filter,
			() -> spawnTamedEquine(EntityType.LLAMA), false);
		assertFilterResult("Llama (untamed)", filter,
			() -> spawnEntity(EntityType.LLAMA), true);
		assertFilterResult("Mule (tamed)", filter,
			() -> spawnTamedEquine(EntityType.MULE), false);
		assertFilterResult("Mule (untamed)", filter,
			() -> spawnEntity(EntityType.MULE), true);
		assertFilterResult("Trader Llama (tamed)", filter,
			() -> spawnTamedEquine(EntityType.TRADER_LLAMA), false);
		assertFilterResult("Trader Llama (untamed)", filter,
			() -> spawnEntity(EntityType.TRADER_LLAMA), true);
		assertFilterResult("Zombie Horse (tamed)", filter,
			() -> spawnTamedEquine(EntityType.ZOMBIE_HORSE), false);
		assertFilterResult("Zombie Horse (untamed)", filter,
			() -> spawnEntity(EntityType.ZOMBIE_HORSE), true);
		
		// Special case: Skeleton Horses self-tame upon trap activation.
		// Relying on the tamed flag seems fine in this case.
		assertFilterResult(
			"Skeleton Horse (tamed, as if from an already-activated trap)",
			filter, () -> spawnTamedEquine(EntityType.SKELETON_HORSE), false);
		assertFilterResult(
			"Skeleton Horse (untamed, as if from a not-yet-activated trap)",
			filter, () -> spawnEntity(EntityType.SKELETON_HORSE), true);
		
		// Special case: Camels (both types) override isTamed() so that they are
		// always tamed. They support normal tamed flag too but ignore it.
		// They should only be considered pets if they have a saddle.
		assertFilterResult("Camel (saddled)", filter,
			() -> spawnSaddledMob(EntityType.CAMEL), false);
		assertFilterResult("Camel Husk (saddled)", filter,
			() -> spawnSaddledMob(EntityType.CAMEL_HUSK), false);
		assertFilterResult("Camel (natural)", filter,
			() -> spawnEntity(EntityType.CAMEL), true);
		assertFilterResult("Camel Husk (natural)", filter,
			() -> spawnEntity(EntityType.CAMEL_HUSK), true);
		assertFilterResult("Camel (with unused tamed flag)", filter,
			() -> spawnTamedEquine(EntityType.CAMEL), true);
		assertFilterResult("Camel Husk (with unused tamed flag)", filter,
			() -> spawnTamedEquine(EntityType.CAMEL_HUSK), true);
		
		// Special case: Striders don't support the tamed flag but otherwise
		// work in a similar way to Camels. Pet if saddled.
		assertFilterResult("Strider (saddled)", filter,
			() -> spawnSaddledMob(EntityType.STRIDER), false);
		assertFilterResult("Strider (natural)", filter,
			() -> spawnEntity(EntityType.STRIDER), true);
		
		// Special case: Pigs behave the same way as Striders so we should treat
		// them the same way. Pet if saddled.
		assertFilterResult("Pig (saddled)", filter,
			() -> spawnSaddledMob(EntityType.PIG), false);
		assertFilterResult("Pig (natural)", filter,
			() -> spawnEntity(EntityType.PIG), true);
		
		// Special case: Pet Ghasts (Happy Ghasts) are an entirely separate mob.
		assertFilterResult("Happy Ghast", filter,
			() -> spawnEntity(EntityType.HAPPY_GHAST), false);
		assertFilterResult("Ghast", filter, () -> spawnEntity(EntityType.GHAST),
			true);
		
		// Special case: Ocelots use a "trust" system that isn't tied to any
		// particular player. Pet if trusting.
		assertFilterResult("Ocelot (trusting)", filter,
			() -> spawnTrustingOcelot(), false);
		assertFilterResult("Ocelot (wild)", filter,
			() -> spawnEntity(EntityType.OCELOT), true);
		
		// Special case: Foxes use an entirely different "trust" system with up
		// to two trusted players. Pet if either trusted player is set.
		assertFilterResult("Fox (trusting)", filter, () -> spawnTrustingFox(),
			false);
		assertFilterResult("Fox (wild)", filter,
			() -> spawnEntity(EntityType.FOX), true);
		
		// Clean up taming particles
		context.waitTick();
		clearParticles();
	}
	
	private <T extends TamableAnimal> T spawnTamedAnimal(EntityType<T> type)
	{
		T entity = spawnEntity(type);
		context.runOnClient(mc -> entity.tame(mc.player));
		return entity;
	}
	
	private <T extends AbstractHorse> T spawnTamedEquine(EntityType<T> type)
	{
		T entity = spawnEntity(type);
		context.runOnClient(mc -> entity.tameWithName(mc.player));
		return entity;
	}
	
	private <T extends Mob> T spawnSaddledMob(EntityType<T> type)
	{
		T entity = spawnEntity(type);
		entity.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
		return entity;
	}
	
	private Ocelot spawnTrustingOcelot()
	{
		Ocelot entity = spawnEntity(EntityType.OCELOT);
		entity.setTrusting(true);
		return entity;
	}
	
	private Fox spawnTrustingFox()
	{
		Fox entity = spawnEntity(EntityType.FOX);
		context.runOnClient(mc -> entity.addTrustedEntity(mc.player));
		return entity;
	}
}
