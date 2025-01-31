/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.stream.Stream;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.MouseUpdateListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.AimAtSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.settings.filters.*;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.Rotation;
import net.wurstclient.util.RotationUtils;

public final class AimAssistHack extends Hack
	implements UpdateListener, MouseUpdateListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 4.5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final SliderSetting rotationSpeed =
		new SliderSetting("Rotation Speed", 600, 10, 3600, 10,
			ValueDisplay.DEGREES.withSuffix("/s"));
	
	private final SliderSetting fov =
		new SliderSetting("FOV", "description.wurst.setting.aimassist.fov", 120,
			30, 360, 10, ValueDisplay.DEGREES);
	
	private final AimAtSetting aimAt = new AimAtSetting(
		"What point in the target's hitbox AimAssist should aim at.");
	
	private final SliderSetting ignoreMouseInput =
		new SliderSetting("Ignore mouse input",
			"description.wurst.setting.aimassist.ignore_mouse_input", 0, 0, 1,
			0.01, ValueDisplay.PERCENTAGE);
	
	private final CheckboxSetting checkLOS =
		new CheckboxSetting("Check line of sight",
			"description.wurst.setting.aimassist.check_line_of_sight", true);
	
	private final CheckboxSetting aimWhileBlocking =
		new CheckboxSetting("Aim while blocking",
			"description.wurst.setting.aimassist.aim_while_blocking", false);
	
	private final EntityFilterList entityFilters =
		new EntityFilterList(FilterPlayersSetting.genericCombat(false),
			FilterSleepingSetting.genericCombat(false),
			FilterFlyingSetting.genericCombat(0),
			FilterHostileSetting.genericCombat(false),
			FilterNeutralSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterPassiveSetting.genericCombat(true),
			FilterPassiveWaterSetting.genericCombat(true),
			FilterBabiesSetting.genericCombat(true),
			FilterBatsSetting.genericCombat(true),
			FilterSlimesSetting.genericCombat(true),
			FilterPetsSetting.genericCombat(true),
			FilterVillagersSetting.genericCombat(true),
			FilterZombieVillagersSetting.genericCombat(true),
			FilterGolemsSetting.genericCombat(false),
			FilterPiglinsSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterZombiePiglinsSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterEndermenSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterShulkersSetting.genericCombat(false),
			FilterInvisibleSetting.genericCombat(true),
			FilterNamedSetting.genericCombat(false),
			FilterShulkerBulletSetting.genericCombat(false),
			FilterArmorStandsSetting.genericCombat(true),
			FilterCrystalsSetting.genericCombat(true));
	
	private Entity target;
	private float nextYaw;
	private float nextPitch;
	
	public AimAssistHack()
	{
		super("AimAssist");
		setCategory(Category.COMBAT);
		
		addSetting(range);
		addSetting(rotationSpeed);
		addSetting(fov);
		addSetting(aimAt);
		addSetting(ignoreMouseInput);
		addSetting(checkLOS);
		addSetting(aimWhileBlocking);
		
		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		// disable incompatible hacks
		WURST.getHax().autoFishHack.setEnabled(false);
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().crystalAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(MouseUpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(MouseUpdateListener.class, this);
		target = null;
	}
	
	@Override
	public void onUpdate()
	{
		target = null;
		
		// don't aim when a container/inventory screen is open
		if(MC.currentScreen instanceof HandledScreen)
			return;
		
		if(!aimWhileBlocking.isChecked() && MC.player.isUsingItem())
			return;
		
		chooseTarget();
		if(target == null)
			return;
		
		Vec3d hitVec = aimAt.getAimPoint(target);
		if(checkLOS.isChecked() && !BlockUtils.hasLineOfSight(hitVec))
		{
			target = null;
			return;
		}
		
		WURST.getHax().autoSwordHack.setSlot(target);
		
		// get needed rotation
		Rotation needed = RotationUtils.getNeededRotations(hitVec);
		
		// turn towards center of boundingBox
		Rotation next = RotationUtils.slowlyTurnTowards(needed,
			rotationSpeed.getValueI() / 20F);
		nextYaw = next.yaw();
		nextPitch = next.pitch();
	}
	
	private void chooseTarget()
	{
		Stream<Entity> stream = EntityUtils.getAttackableEntities();
		
		double rangeSq = range.getValueSq();
		stream = stream.filter(e -> MC.player.squaredDistanceTo(e) <= rangeSq);
		
		if(fov.getValue() < 360.0)
			stream = stream.filter(e -> RotationUtils.getAngleToLookVec(
				aimAt.getAimPoint(e)) <= fov.getValue() / 2.0);
		
		stream = entityFilters.applyTo(stream);
		
		target = stream
			.min(Comparator.comparingDouble(
				e -> RotationUtils.getAngleToLookVec(aimAt.getAimPoint(e))))
			.orElse(null);
	}
	
	@Override
	public void onMouseUpdate(MouseUpdateEvent event)
	{
		if(target == null || MC.player == null)
			return;
		
		float curYaw = MC.player.getYaw();
		float curPitch = MC.player.getPitch();
		int diffYaw = (int)(nextYaw - curYaw);
		int diffPitch = (int)(nextPitch - curPitch);
		
		// If we are <1 degree off but still missing the hitbox,
		// slightly exaggerate the difference to fix that.
		if(diffYaw == 0 && diffPitch == 0 && !RotationUtils
			.isFacingBox(target.getBoundingBox(), range.getValue()))
		{
			diffYaw = nextYaw < curYaw ? -1 : 1;
			diffPitch = nextPitch < curPitch ? -1 : 1;
		}
		
		double inputFactor = 1 - ignoreMouseInput.getValue();
		int mouseInputX = (int)(event.getDefaultDeltaX() * inputFactor);
		int mouseInputY = (int)(event.getDefaultDeltaY() * inputFactor);
		
		event.setDeltaX(mouseInputX + diffYaw);
		event.setDeltaY(mouseInputY + diffPitch);
	}
}
