/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;

@SearchTags({"click aura", "ClickAimbot", "click aimbot"})
public final class ClickAuraHack extends Hack
	implements UpdateListener, LeftClickListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Priority> priority = new EnumSetting<>("Priority",
		"Determines which entity will be attacked first.\n"
			+ "\u00a7lDistance\u00a7r - Attacks the closest entity.\n"
			+ "\u00a7lAngle\u00a7r - Attacks the entity that requires\n"
			+ "the least head movement.\n"
			+ "\u00a7lHealth\u00a7r - Attacks the weakest entity.",
		Priority.values(), Priority.ANGLE);
	
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
	
	private final CheckboxSetting filterTraders =
		new CheckboxSetting("Filter traders",
			"Won't attack villagers, wandering traders, etc.", false);
	
	private final CheckboxSetting filterGolems =
		new CheckboxSetting("Filter golems",
			"Won't attack iron golems,\n" + "snow golems and shulkers.", false);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"Filter invisible", "Won't attack invisible entities.", false);
	private final CheckboxSetting filterNamed = new CheckboxSetting(
		"Filter named", "Won't attack name-tagged entities.", false);
	
	private final CheckboxSetting filterStands = new CheckboxSetting(
		"Filter armor stands", "Won't attack armor stands.", false);
	private final CheckboxSetting filterCrystals = new CheckboxSetting(
		"Filter end crystals", "Won't attack end crystals.", false);
	
	public ClickAuraHack()
	{
		super("ClickAura", "Automatically attacks the closest valid entity\n"
			+ "whenever you click.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r ClickAuras generally look more suspicious\n"
			+ "than Killauras and are easier for plugins to detect.\n"
			+ "It is recommended to use Killaura or TriggerBot instead.");
		
		setCategory(Category.COMBAT);
		addSetting(range);
		addSetting(priority);
		addSetting(filterPlayers);
		addSetting(filterSleeping);
		addSetting(filterFlying);
		addSetting(filterMonsters);
		addSetting(filterPigmen);
		addSetting(filterEndermen);
		addSetting(filterAnimals);
		addSetting(filterBabies);
		addSetting(filterPets);
		addSetting(filterTraders);
		addSetting(filterGolems);
		addSetting(filterInvisible);
		addSetting(filterNamed);
		addSetting(filterStands);
		addSetting(filterCrystals);
	}
	
	@Override
	public void onEnable()
	{
		// disable other killauras
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().triggerBotHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(LeftClickListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(LeftClickListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(!MC.options.keyAttack.isPressed())
			return;
		
		if(MC.player.getAttackCooldownProgress(0) < 1)
			return;
		
		attack();
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		attack();
	}
	
	private void attack()
	{
		// set entity
		ClientPlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		if(player.getAttackCooldownProgress(0) < 1)
			return;
		
		double rangeSq = Math.pow(range.getValue(), 2);
		Stream<Entity> stream =
			StreamSupport.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> !e.removed)
				.filter(e -> e instanceof LivingEntity
					&& ((LivingEntity)e).getHealth() > 0
					|| e instanceof EndCrystalEntity)
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
				return world.isSpaceEmpty(box);
			});
		
		if(filterMonsters.isChecked())
			stream = stream.filter(e -> !(e instanceof Monster));
		
		if(filterPigmen.isChecked())
			stream = stream.filter(e -> !(e instanceof ZombifiedPiglinEntity));
		
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
		
		if(filterTraders.isChecked())
			stream = stream.filter(e -> !(e instanceof MerchantEntity));
		
		if(filterGolems.isChecked())
			stream = stream.filter(e -> !(e instanceof GolemEntity));
		
		if(filterInvisible.isChecked())
			stream = stream.filter(e -> !e.isInvisible());
		
		if(filterNamed.isChecked())
			stream = stream.filter(e -> !e.hasCustomName());
		
		if(filterStands.isChecked())
			stream = stream.filter(e -> !(e instanceof ArmorStandEntity));
		
		if(filterCrystals.isChecked())
			stream = stream.filter(e -> !(e instanceof EndCrystalEntity));
		
		Entity target =
			stream.min(priority.getSelected().comparator).orElse(null);
		if(target == null)
			return;
		
		WURST.getHax().autoSwordHack.setSlot();
		
		// face entity
		Rotation rotation = RotationUtils
			.getNeededRotations(target.getBoundingBox().getCenter());
		PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(
			rotation.getYaw(), rotation.getPitch(), MC.player.isOnGround());
		MC.player.networkHandler.sendPacket(packet);
		
		// attack entity
		WURST.getHax().criticalsHack.doCritical();
		MC.interactionManager.attackEntity(player, target);
		player.swingHand(Hand.MAIN_HAND);
	}
	
	private enum Priority
	{
		DISTANCE("Distance", e -> MC.player.squaredDistanceTo(e)),
		
		ANGLE("Angle",
			e -> RotationUtils
				.getAngleToLookVec(e.getBoundingBox().getCenter())),
		
		HEALTH("Health", e -> e instanceof LivingEntity
			? ((LivingEntity)e).getHealth() : Integer.MAX_VALUE);
		
		private final String name;
		private final Comparator<Entity> comparator;
		
		private Priority(String name, ToDoubleFunction<Entity> keyExtractor)
		{
			this.name = name;
			comparator = Comparator.comparingDouble(keyExtractor);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
