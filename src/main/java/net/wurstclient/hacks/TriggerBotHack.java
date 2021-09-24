/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.stream.Stream;

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
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.FakePlayerEntity;

@SearchTags({"trigger bot"})
public final class TriggerBotHack extends Hack implements UpdateListener
{
	private final SliderSetting range =
		new SliderSetting("范围", 4.25, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting filterPlayers = new CheckboxSetting(
		"排除玩家", "", false);
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"排除睡眠", "", false);
	private final SliderSetting filterFlying =
		new SliderSetting("排除飞行","",
			0, 0, 2, 0.05,
			v -> v == 0 ? "off" : ValueDisplay.DECIMAL.getValueString(v));
	
	private final CheckboxSetting filterMonsters = new CheckboxSetting(
		"排除怪物", "", false);
	private final CheckboxSetting filterPigmen = new CheckboxSetting(
		"排除猪人", "", false);
	private final CheckboxSetting filterEndermen =
		new CheckboxSetting("排除末影人", "", false);
	
	private final CheckboxSetting filterAnimals = new CheckboxSetting(
		"排除动物", "", false);
	private final CheckboxSetting filterBabies =
		new CheckboxSetting("排除幼年",
			"", false);
	private final CheckboxSetting filterPets =
		new CheckboxSetting("排除宠物","", false);
	
	private final CheckboxSetting filterTraders =
		new CheckboxSetting("排除商人","", false);
	
	private final CheckboxSetting filterGolems =
		new CheckboxSetting("排除魔物","", false);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"排除隐形", "", false);
	private final CheckboxSetting filterNamed = new CheckboxSetting(
		"排除命名", "", false);
	
	private final CheckboxSetting filterStands = new CheckboxSetting(
		"排除盔甲架", "", false);
	private final CheckboxSetting filterCrystals = new CheckboxSetting(
		"排除末影水晶", "", false);
	
	public TriggerBotHack()
	{
		super("触发机器人","自动攻击您正在查看的实体.");
		setCategory(Category.COMBAT);
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
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().crystalAuraHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
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
		if(player.getAttackCooldownProgress(0) < 1)
			return;
		
		if(MC.crosshairTarget == null
			|| !(MC.crosshairTarget instanceof EntityHitResult))
			return;
		
		Entity target = ((EntityHitResult)MC.crosshairTarget).getEntity();
		if(!isCorrectEntity(target))
			return;
		
		WURST.getHax().autoSwordHack.setSlot();
		
		WURST.getHax().criticalsHack.doCritical();
		MC.interactionManager.attackEntity(player, target);
		player.swingHand(Hand.MAIN_HAND);
	}
	
	private boolean isCorrectEntity(Entity entity)
	{
		ClientPlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		double rangeSq = Math.pow(range.getValue(), 2);
		Stream<Entity> stream = Stream.of(entity).filter(e -> !e.isRemoved())
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
				return !world.isSpaceEmpty(box);
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
		
		return stream.findFirst().isPresent();
	}
}
