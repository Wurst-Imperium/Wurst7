/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests.filters;

import java.util.List;
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
		
		// Normal pets: filter out if tamed
		for(EntityType<? extends TamableAnimal> type : List.of(EntityTypes.CAT,
			EntityTypes.NAUTILUS, EntityTypes.PARROT, EntityTypes.WOLF,
			EntityTypes.ZOMBIE_NAUTILUS))
		{
			assertFilteredOut(type.toShortString() + " (tamed)", filter,
				() -> spawnTamedAnimal(type));
			assertAllowed(type.toShortString() + " (wild)", filter,
				() -> spawnEntity(type));
		}
		
		// Normal horse-likes: filter out if tamed
		for(EntityType<? extends AbstractHorse> type : List.of(
			EntityTypes.DONKEY, EntityTypes.HORSE, EntityTypes.LLAMA,
			EntityTypes.MULE, EntityTypes.TRADER_LLAMA,
			EntityTypes.ZOMBIE_HORSE))
		{
			assertFilteredOut(type.toShortString() + " (tamed)", filter,
				() -> spawnTamedEquine(type));
			assertAllowed(type.toShortString() + " (wild)", filter,
				() -> spawnEntity(type));
		}
		
		// Special case: Skeleton Horses self-tame upon trap activation.
		// Relying on the tamed flag seems fine in this case.
		assertFilteredOut(
			"Skeleton Horse (tamed, as if from an already-activated trap)",
			filter, () -> spawnTamedEquine(EntityTypes.SKELETON_HORSE));
		assertAllowed(
			"Skeleton Horse (untamed, as if from a not-yet-activated trap)",
			filter, () -> spawnEntity(EntityTypes.SKELETON_HORSE));
		
		// Special case: Camels (both types) override isTamed() so that they are
		// always tamed. They support normal tamed flag too but ignore it.
		// They should only be considered pets if they have a saddle.
		for(EntityType<? extends AbstractHorse> type : List
			.of(EntityTypes.CAMEL, EntityTypes.CAMEL_HUSK))
		{
			assertFilteredOut(type.toShortString() + " (saddled)", filter,
				() -> spawnSaddledMob(type));
			assertAllowed(type.toShortString() + " (wild)", filter,
				() -> spawnEntity(type));
			assertAllowed(type.toShortString() + " (with unused tamed flag)",
				filter, () -> spawnTamedEquine(type));
		}
		
		// Special case: Pigs and Striders don't support the tamed flag but
		// otherwise work in a similar way to Camels. Pet if saddled.
		for(EntityType<? extends Mob> type : List.of(EntityTypes.PIG,
			EntityTypes.STRIDER))
		{
			assertFilteredOut(type.toShortString() + " (saddled)", filter,
				() -> spawnSaddledMob(type));
			assertAllowed(type.toShortString() + " (wild)", filter,
				() -> spawnEntity(type));
		}
		
		// Special case: Pet Ghasts (Happy Ghasts) are an entirely separate mob.
		assertFilteredOut(EntityTypes.HAPPY_GHAST.toShortString(), filter,
			() -> spawnEntity(EntityTypes.HAPPY_GHAST));
		assertAllowed(EntityTypes.GHAST.toShortString(), filter,
			() -> spawnEntity(EntityTypes.GHAST));
		
		// Special case: Ocelots use a "trust" system that isn't tied to any
		// particular player. Pet if trusting.
		assertFilteredOut(EntityTypes.OCELOT.toShortString() + " (trusting)",
			filter, () -> spawnTrustingOcelot());
		assertAllowed(EntityTypes.OCELOT.toShortString() + " (wild)", filter,
			() -> spawnEntity(EntityTypes.OCELOT));
		
		// Special case: Foxes use an entirely different "trust" system with up
		// to two trusted players. Pet if either trusted player is set.
		assertFilteredOut(EntityTypes.FOX.toShortString() + " (trusting)",
			filter, () -> spawnTrustingFox());
		assertAllowed(EntityTypes.FOX.toShortString() + " (wild)", filter,
			() -> spawnEntity(EntityTypes.FOX));
		
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
