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
import net.minecraft.world.entity.EntityTypes;
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
			() -> spawnTamedAnimal(EntityTypes.CAT), false);
		assertFilterResult("Cat (untamed)", filter,
			() -> spawnEntity(EntityTypes.CAT), true);
		assertFilterResult("Nautilus (tamed)", filter,
			() -> spawnTamedAnimal(EntityTypes.NAUTILUS), false);
		assertFilterResult("Nautilus (untamed)", filter,
			() -> spawnEntity(EntityTypes.NAUTILUS), true);
		assertFilterResult("Parrot (tamed)", filter,
			() -> spawnTamedAnimal(EntityTypes.PARROT), false);
		assertFilterResult("Parrot (untamed)", filter,
			() -> spawnEntity(EntityTypes.PARROT), true);
		assertFilterResult("Wolf (tamed)", filter,
			() -> spawnTamedAnimal(EntityTypes.WOLF), false);
		assertFilterResult("Wolf (untamed)", filter,
			() -> spawnEntity(EntityTypes.WOLF), true);
		assertFilterResult("Zombie Nautilus (tamed)", filter,
			() -> spawnTamedAnimal(EntityTypes.ZOMBIE_NAUTILUS), false);
		assertFilterResult("Zombie Nautilus (untamed)", filter,
			() -> spawnEntity(EntityTypes.ZOMBIE_NAUTILUS), true);
		
		// Normal horse-likes - filter out if tamed
		assertFilterResult("Donkey (tamed)", filter,
			() -> spawnTamedEquine(EntityTypes.DONKEY), false);
		assertFilterResult("Donkey (untamed)", filter,
			() -> spawnEntity(EntityTypes.DONKEY), true);
		assertFilterResult("Horse (tamed)", filter,
			() -> spawnTamedEquine(EntityTypes.HORSE), false);
		assertFilterResult("Horse (untamed)", filter,
			() -> spawnEntity(EntityTypes.HORSE), true);
		assertFilterResult("Llama (tamed)", filter,
			() -> spawnTamedEquine(EntityTypes.LLAMA), false);
		assertFilterResult("Llama (untamed)", filter,
			() -> spawnEntity(EntityTypes.LLAMA), true);
		assertFilterResult("Mule (tamed)", filter,
			() -> spawnTamedEquine(EntityTypes.MULE), false);
		assertFilterResult("Mule (untamed)", filter,
			() -> spawnEntity(EntityTypes.MULE), true);
		assertFilterResult("Trader Llama (tamed)", filter,
			() -> spawnTamedEquine(EntityTypes.TRADER_LLAMA), false);
		assertFilterResult("Trader Llama (untamed)", filter,
			() -> spawnEntity(EntityTypes.TRADER_LLAMA), true);
		assertFilterResult("Zombie Horse (tamed)", filter,
			() -> spawnTamedEquine(EntityTypes.ZOMBIE_HORSE), false);
		assertFilterResult("Zombie Horse (untamed)", filter,
			() -> spawnEntity(EntityTypes.ZOMBIE_HORSE), true);
		
		// Special case: Skeleton Horses self-tame upon trap activation.
		// Relying on the tamed flag seems fine in this case.
		assertFilterResult(
			"Skeleton Horse (tamed, as if from an already-activated trap)",
			filter, () -> spawnTamedEquine(EntityTypes.SKELETON_HORSE), false);
		assertFilterResult(
			"Skeleton Horse (untamed, as if from a not-yet-activated trap)",
			filter, () -> spawnEntity(EntityTypes.SKELETON_HORSE), true);
		
		// Special case: Camels (both types) override isTamed() so that they are
		// always tamed. They support normal tamed flag too but ignore it.
		// They should only be considered pets if they have a saddle.
		assertFilterResult("Camel (saddled)", filter,
			() -> spawnSaddledMob(EntityTypes.CAMEL), false);
		assertFilterResult("Camel Husk (saddled)", filter,
			() -> spawnSaddledMob(EntityTypes.CAMEL_HUSK), false);
		assertFilterResult("Camel (natural)", filter,
			() -> spawnEntity(EntityTypes.CAMEL), true);
		assertFilterResult("Camel Husk (natural)", filter,
			() -> spawnEntity(EntityTypes.CAMEL_HUSK), true);
		assertFilterResult("Camel (with unused tamed flag)", filter,
			() -> spawnTamedEquine(EntityTypes.CAMEL), true);
		assertFilterResult("Camel Husk (with unused tamed flag)", filter,
			() -> spawnTamedEquine(EntityTypes.CAMEL_HUSK), true);
		
		// Special case: Striders don't support the tamed flag but otherwise
		// work in a similar way to Camels. Pet if saddled.
		assertFilterResult("Strider (saddled)", filter,
			() -> spawnSaddledMob(EntityTypes.STRIDER), false);
		assertFilterResult("Strider (natural)", filter,
			() -> spawnEntity(EntityTypes.STRIDER), true);
		
		// Special case: Pigs behave the same way as Striders so we should treat
		// them the same way. Pet if saddled.
		assertFilterResult("Pig (saddled)", filter,
			() -> spawnSaddledMob(EntityTypes.PIG), false);
		assertFilterResult("Pig (natural)", filter,
			() -> spawnEntity(EntityTypes.PIG), true);
		
		// Special case: Pet Ghasts (Happy Ghasts) are an entirely separate mob.
		assertFilterResult("Happy Ghast", filter,
			() -> spawnEntity(EntityTypes.HAPPY_GHAST), false);
		assertFilterResult("Ghast", filter,
			() -> spawnEntity(EntityTypes.GHAST), true);
		
		// Special case: Ocelots use a "trust" system that isn't tied to any
		// particular player. Pet if trusting.
		assertFilterResult("Ocelot (trusting)", filter,
			() -> spawnTrustingOcelot(), false);
		assertFilterResult("Ocelot (wild)", filter,
			() -> spawnEntity(EntityTypes.OCELOT), true);
		
		// Special case: Foxes use an entirely different "trust" system with up
		// to two trusted players. Pet if either trusted player is set.
		assertFilterResult("Fox (trusting)", filter, () -> spawnTrustingFox(),
			false);
		assertFilterResult("Fox (wild)", filter,
			() -> spawnEntity(EntityTypes.FOX), true);
		
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
		Ocelot entity = spawnEntity(EntityTypes.OCELOT);
		entity.setTrusting(true);
		return entity;
	}
	
	private Fox spawnTrustingFox()
	{
		Fox entity = spawnEntity(EntityTypes.FOX);
		context.runOnClient(mc -> entity.addTrustedEntity(mc.player));
		return entity;
	}
}
