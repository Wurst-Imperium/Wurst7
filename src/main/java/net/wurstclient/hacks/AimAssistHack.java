/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.stream.Stream;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.Entity;
import net.wurstclient.Category;
import net.wurstclient.events.MouseUpdateListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.AimAtSetting;
import net.wurstclient.settings.AimAtSetting.AimAt;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FovSliderSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.settings.filters.*;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.MouseAimController;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.text.WText;

public final class AimAssistHack extends Hack
	implements UpdateListener, PreMotionListener, MouseUpdateListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 4.5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final SliderSetting rotationSpeed =
		new SliderSetting("Rotation Speed", 300, 10, 3600, 10,
			ValueDisplay.DEGREES.withSuffix("/s"));
	
	private final FovSliderSetting fov = new FovSliderSetting(this, 120);
	
	private final AimAtSetting aimAt =
		new AimAtSetting(AimAtSetting.genericDescription(this), AimAt.CENTER);
	
	private final SliderSetting ignoreMouseInput =
		new SliderSetting("Ignore mouse input",
			WText.translated(
				"description.wurst.setting.generic.ignore_mouse_input",
				getName()),
			0, 0, 1, 0.01, ValueDisplay.PERCENTAGE);
	
	private final CheckboxSetting checkLOS =
		new CheckboxSetting("Check line of sight",
			"description.wurst.setting.aimassist.check_line_of_sight", true);
	
	private final CheckboxSetting aimWhileBlocking =
		new CheckboxSetting("Aim while blocking",
			"description.wurst.setting.aimassist.aim_while_blocking", true);
	
	private final EntityFilterList entityFilters =
		new EntityFilterList(FilterPlayersSetting.genericCombat(false),
			FilterCreativeSetting.genericCombat(false),
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
			FilterProjectilesSetting.genericCombat(false),
			FilterArmorStandsSetting.genericCombat(true),
			FilterCrystalsSetting.genericCombat(true),
			FilterInteractionsSetting.genericCombat(true));
	
	private final MouseAimController mouseAim = new MouseAimController();
	private Entity target;
	
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
		EVENTS.add(PreMotionListener.class, this);
		EVENTS.add(MouseUpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PreMotionListener.class, this);
		EVENTS.remove(MouseUpdateListener.class, this);
		target = null;
		mouseAim.reset();
	}
	
	@Override
	public void onUpdate()
	{
		target = null;
		
		// don't aim when a container/inventory screen is open
		if(MC.gui.screen() instanceof AbstractContainerScreen)
			return;
		
		if(!aimWhileBlocking.isChecked() && MC.player.isUsingItem())
			return;
		
		chooseTarget();
		if(target == null)
			return;
		
		WURST.getHax().autoSwordHack.setSlot(target);
	}
	
	private void chooseTarget()
	{
		double rangeSq = range.getValueSq();
		Stream<Entity> stream = EntityUtils.getMeleeAttackableEntities()
			.filter(e -> EntityUtils.distanceToHitboxSq(e) <= rangeSq)
			.filter(fov.asEntityPredicate(aimAt::getAimPoint))
			.filter(e -> !checkLOS.isChecked()
				|| BlockUtils.hasLineOfSight(aimAt.getAimPoint(e)));
		
		stream = entityFilters.applyTo(stream);
		
		target = stream
			.min(Comparator.comparingDouble(e -> RotationUtils
				.getYawPitchDistanceToLookVec(aimAt.getAimPoint(e))))
			.orElse(null);
	}
	
	@Override
	public void onPreMotion()
	{
		if(target == null)
		{
			mouseAim.reset();
			return;
		}
		
		mouseAim.tick(aimAt.getAimPoint(target), rotationSpeed.getValueI());
	}
	
	@Override
	public void onMouseUpdate(MouseUpdateEvent event)
	{
		if(target != null)
			mouseAim.applyTo(event, ignoreMouseInput.getValue());
	}
}
