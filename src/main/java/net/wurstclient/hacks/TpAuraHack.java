/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.Random;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.AttackSpeedSliderSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.PauseAttackOnContainersSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"TpAura", "tp aura", "EnderAura", "Ender-Aura", "ender aura"})
public final class TpAuraHack extends Hack implements UpdateListener
{
	private final Random random = new Random();
	
	private final SliderSetting range =
		new SliderSetting("Range", 4.25, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final AttackSpeedSliderSetting speed =
		new AttackSpeedSliderSetting();
	
	private final EnumSetting<Priority> priority = new EnumSetting<>("Priority",
		"Determines which entity will be attacked first.\n"
			+ "\u00a7lDistance\u00a7r - Attacks the closest entity.\n"
			+ "\u00a7lAngle\u00a7r - Attacks the entity that requires the least head movement.\n"
			+ "\u00a7lHealth\u00a7r - Attacks the weakest entity.",
		Priority.values(), Priority.ANGLE);
	
	private final PauseAttackOnContainersSetting pauseOnContainers =
		new PauseAttackOnContainersSetting(true);
	
	private final EntityFilterList entityFilters =
		EntityFilterList.genericCombat();
	
	public TpAuraHack()
	{
		super("TP-Aura");
		setCategory(Category.COMBAT);
		
		addSetting(range);
		addSetting(speed);
		addSetting(priority);
		addSetting(pauseOnContainers);
		
		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	public void onEnable()
	{
		// disable other killauras
		WURST.getHax().aimAssistHack.setEnabled(false);
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().crystalAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().triggerBotHack.setEnabled(false);
		
		speed.resetTimer();
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
		speed.updateTimer();
		if(!speed.isTimeToAttack())
			return;
		
		if(pauseOnContainers.shouldPause())
			return;
		
		ClientPlayerEntity player = MC.player;
		
		// set entity
		Stream<Entity> stream = EntityUtils.getAttackableEntities();
		double rangeSq = Math.pow(range.getValue(), 2);
		stream = stream.filter(e -> MC.player.squaredDistanceTo(e) <= rangeSq);
		
		stream = entityFilters.applyTo(stream);
		
		Entity entity =
			stream.min(priority.getSelected().comparator).orElse(null);
		if(entity == null)
			return;
		
		WURST.getHax().autoSwordHack.setSlot(entity);
		
		// teleport
		player.setPosition(entity.getX() + random.nextInt(3) * 2 - 2,
			entity.getY(), entity.getZ() + random.nextInt(3) * 2 - 2);
		
		// check cooldown
		if(player.getAttackCooldownProgress(0) < 1)
			return;
		
		// attack entity
		RotationUtils.getNeededRotations(entity.getBoundingBox().getCenter())
			.sendPlayerLookPacket();
		
		WURST.getHax().criticalsHack.doCritical();
		MC.interactionManager.attackEntity(player, entity);
		player.swingHand(Hand.MAIN_HAND);
		speed.resetTimer();
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
