/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Random;
import java.util.stream.Stream;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.AttackPrioritySetting;
import net.wurstclient.settings.AttackPrioritySetting.AttackPriority;
import net.wurstclient.settings.AttackSpeedSliderSetting;
import net.wurstclient.settings.PauseAttackOnContainersSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
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
	
	private final AttackPrioritySetting priority =
		AttackPrioritySetting.withoutAngleDistOption(
			AttackPrioritySetting.genericDescription(), AttackPriority.ANGLE);
	
	private final SwingHandSetting swingHand = new SwingHandSetting(
		SwingHandSetting.genericCombatDescription(this), SwingHand.CLIENT);
	
	private final PauseAttackOnContainersSetting pauseOnContainers =
		new PauseAttackOnContainersSetting(true);
	
	private final EntityFilterList entityFilters =
		EntityFilterList.genericMeleeCombat();
	
	public TpAuraHack()
	{
		super("TP-Aura");
		setCategory(Category.COMBAT);
		
		addSetting(range);
		addSetting(speed);
		addSetting(priority);
		addSetting(swingHand);
		addSetting(pauseOnContainers);
		
		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
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
	protected void onDisable()
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
		
		LocalPlayer player = MC.player;
		
		// set entity
		double rangeSq = range.getValueSq();
		Stream<Entity> stream = EntityUtils.getMeleeAttackableEntities()
			.filter(e -> EntityUtils.distanceToHitboxSq(e) <= rangeSq);
		
		stream = entityFilters.applyTo(stream);
		
		Entity entity = stream
			.min(priority.getComparator(e -> e.getBoundingBox().getCenter()))
			.orElse(null);
		if(entity == null)
			return;
		
		WURST.getHax().autoSwordHack.setSlot(entity);
		
		// teleport
		player.setPos(entity.getX() + random.nextInt(3) * 2 - 2, entity.getY(),
			entity.getZ() + random.nextInt(3) * 2 - 2);
		
		// attack entity
		RotationUtils.getNeededRotations(entity.getBoundingBox().getCenter())
			.sendPlayerLookPacket();
		
		MC.gameMode.attack(player, entity);
		swingHand.swing(InteractionHand.MAIN_HAND);
		speed.resetTimer();
	}
}
