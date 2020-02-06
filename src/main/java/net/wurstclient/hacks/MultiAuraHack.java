/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.mob.ZombiePigmanEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RotationUtils;

@SearchTags({"multi aura", "ForceField", "force field"})
public final class MultiAuraHack extends Hack implements UpdateListener
{
	private final CheckboxSetting useCooldown = new CheckboxSetting(
		"Use cooldown", "Use your weapon's cooldown as the attack speed.\n"
			+ "When checked, the 'Speed' slider will be ignored.",
		true);
	
	private final SliderSetting speed = new SliderSetting("Speed",
		"Attack speed in clicks per second.\n"
			+ "This value will be ignored when\n"
			+ "'Use cooldown' is checked.",
		12, 0.1, 20, 0.1, ValueDisplay.DECIMAL);
	
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting filterPlayers = new CheckboxSetting(
		"Filter players", "Won't attack other players.", false);
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"Filter sleeping", "Won't attack sleeping players.", false);
	private final SliderSetting filterFlying =
		new SliderSetting("Filter flying",
			"Won't attack players that\n" + "are at least the given\n"
				+ "distance above ground.",
			0, 0, 2, 0.05,
			v -> v == 0 ? "off" : ValueDisplay.DECIMAL.getValueString(v));
	
	private final CheckboxSetting filterMonsters = new CheckboxSetting(
		"Filter monsters", "Won't attack zombies, creepers, etc.", false);
	private final CheckboxSetting filterPigmen = new CheckboxSetting(
		"Filter pigmen", "Won't attack zombie pigmen.", false);
	private final CheckboxSetting filterEndermen =
		new CheckboxSetting("Filter endermen", "Won't attack endermen.", false);
	
	private final CheckboxSetting filterAnimals = new CheckboxSetting(
		"Filter animals", "Won't attack pigs, cows, etc.", false);
	private final CheckboxSetting filterBabies =
		new CheckboxSetting("Filter babies",
			"Won't attack baby pigs,\n" + "baby villagers, etc.", false);
	private final CheckboxSetting filterPets =
		new CheckboxSetting("Filter pets",
			"Won't attack tamed wolves,\n" + "tamed horses, etc.", false);
	
	private final CheckboxSetting filterVillagers = new CheckboxSetting(
		"Filter villagers", "Won't attack villagers.", false);
	private final CheckboxSetting filterGolems =
		new CheckboxSetting("Filter golems",
			"Won't attack iron golems,\n" + "snow golems and shulkers.", false);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"Filter invisible", "Won't attack invisible entities.", false);
	
	private int timer;
	
	public MultiAuraHack()
	{
		super("MultiAura",
			"Faster Killaura that attacks multiple entities at once.");
		setCategory(Category.COMBAT);
		
		addSetting(useCooldown);
		addSetting(speed);
		addSetting(range);
		
		addSetting(filterPlayers);
		addSetting(filterSleeping);
		addSetting(filterFlying);
		addSetting(filterMonsters);
		addSetting(filterPigmen);
		addSetting(filterEndermen);
		addSetting(filterAnimals);
		addSetting(filterBabies);
		addSetting(filterPets);
		addSetting(filterVillagers);
		addSetting(filterGolems);
		addSetting(filterInvisible);
	}
	
	@Override
	public void onEnable()
	{
		// disable other killauras
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		WURST.getHax().triggerBotHack.setEnabled(false);
		
		timer = 0;
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		// update timer
		timer += 50;
		
		// check timer / cooldown
		if(useCooldown.isChecked() ? player.getAttackCooldownProgress(0) < 1
			: timer < 1000 / speed.getValue())
			return;
		
		// get entities
		double rangeSq = Math.pow(range.getValue(), 2);
		Stream<LivingEntity> stream = StreamSupport
			.stream(world.getEntities().spliterator(), true)
			.filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity)e)
			.filter(e -> !e.removed && e.getHealth() > 0)
			.filter(e -> player.squaredDistanceTo(e) <= rangeSq)
			.filter(e -> e != player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> !WURST.getFriends().contains(e.getEntityName()));
		
		if(filterPlayers.isChecked())
			stream = stream.filter(e -> !(e instanceof PlayerEntity));
		
		if(filterSleeping.isChecked())
			stream = stream.filter(e -> !(e instanceof PlayerEntity
				&& ((PlayerEntity)e).isSleeping()));
		
		if(filterFlying.getValue() > 0)
			stream = stream.filter(e -> {
				
				if(!(e instanceof PlayerEntity))
					return true;
				
				Box box = e.getBoundingBox();
				box = box.union(box.offset(0, -filterFlying.getValue(), 0));
				return world.doesNotCollide(box);
			});
		
		if(filterMonsters.isChecked())
			stream = stream.filter(e -> !(e instanceof Monster));
		
		if(filterPigmen.isChecked())
			stream = stream.filter(e -> !(e instanceof ZombiePigmanEntity));
		
		if(filterEndermen.isChecked())
			stream = stream.filter(e -> !(e instanceof EndermanEntity));
		
		if(filterAnimals.isChecked())
			stream = stream.filter(
				e -> !(e instanceof AnimalEntity || e instanceof AmbientEntity
					|| e instanceof WaterCreatureEntity));
		
		if(filterBabies.isChecked())
			stream = stream.filter(e -> !(e instanceof PassiveEntity
				&& ((PassiveEntity)e).isBaby()));
		
		if(filterPets.isChecked())
			stream = stream
				.filter(e -> !(e instanceof TameableEntity
					&& ((TameableEntity)e).isTamed()))
				.filter(e -> !(e instanceof HorseBaseEntity
					&& ((HorseBaseEntity)e).isTame()));
		
		if(filterVillagers.isChecked())
			stream = stream.filter(e -> !(e instanceof VillagerEntity));
		
		if(filterGolems.isChecked())
			stream = stream.filter(e -> !(e instanceof GolemEntity));
		
		if(filterInvisible.isChecked())
			stream = stream.filter(e -> !e.isInvisible());
		
		ArrayList<Entity> entities =
			stream.collect(Collectors.toCollection(() -> new ArrayList<>()));
		if(entities.isEmpty())
			return;
		
		WURST.getHax().autoSwordHack.setSlot();
		
		// attack entities
		for(Entity entity : entities)
		{
			RotationUtils.Rotation rotations = RotationUtils
				.getNeededRotations(entity.getBoundingBox().getCenter());
			
			WurstClient.MC.player.networkHandler
				.sendPacket(new PlayerMoveC2SPacket.LookOnly(rotations.getYaw(),
					rotations.getPitch(), MC.player.onGround));
			
			MC.interactionManager.attackEntity(player, entity);
		}
		
		player.swingHand(Hand.MAIN_HAND);
		
		// reset timer
		timer = 0;
	}
}
