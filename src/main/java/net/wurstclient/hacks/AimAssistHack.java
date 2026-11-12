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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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
import net.wurstclient.util.Rotation;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.text.WText;

public final class AimAssistHack extends Hack
	implements UpdateListener, PreMotionListener, MouseUpdateListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 4.5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final SliderSetting rotationSpeed =
		new SliderSetting("Rotation Speed", 600, 10, 3600, 10,
			ValueDisplay.DEGREES.withSuffix("/s"));
	
	private final FovSliderSetting fov = new FovSliderSetting(this, 120);
	
	private final AimAtSetting aimAt =
		new AimAtSetting(AimAtSetting.genericDescription(this), AimAt.AUTO);
	
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
	
	private Entity target;
	private Rotation curTickRot;
	private Rotation nextTickRot;
	
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
		curTickRot = null;
		nextTickRot = null;
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
		
		Vec3 hitVec = aimAt.getAimPoint(target);
		if(checkLOS.isChecked() && !BlockUtils.hasLineOfSight(hitVec))
		{
			target = null;
			return;
		}
		
		WURST.getHax().autoSwordHack.setSlot(target);
	}
	
	@Override
	public void onPreMotion()
	{
		if(target == null)
		{
			nextTickRot = null;
			return;
		}
		
		curTickRot = nextTickRot != null ? nextTickRot
			: new Rotation(MC.player.getYRot(), MC.player.getXRot());
		
		// get needed rotation
		Rotation needed =
			RotationUtils.getNeededRotations(aimAt.getAimPoint(target));
		
		// turn towards center of boundingBox
		nextTickRot = curTickRot.slowlyTurnTowards(needed,
			rotationSpeed.getValueI() / 20F);
		
		// Vulcan D/F flag >1.825° yaw changes with unchanged pitch
		if(Math.abs(nextTickRot.yaw() - curTickRot.yaw()) > 1.825F
			&& nextTickRot.pitch() == curTickRot.pitch())
		{
			float pitchOffset = (float)(RotationUtils.getDegreesPerMouseUnit()
				* (Math.random() < 0.5 ? -1 : 1));
			nextTickRot = nextTickRot.add(0, pitchOffset).clampPitch();
		}
	}
	
	private void chooseTarget()
	{
		Stream<Entity> stream = EntityUtils.getMeleeAttackableEntities();
		
		double rangeSq = range.getValueSq();
		stream =
			stream.filter(e -> EntityUtils.distanceToHitboxSq(e) <= rangeSq)
				.filter(fov.asEntityPredicate(aimAt::getAimPoint));
		
		stream = entityFilters.applyTo(stream);
		
		target = stream
			.min(Comparator.comparingDouble(e -> RotationUtils
				.getYawPitchDistanceToLookVec(aimAt.getAimPoint(e))))
			.orElse(null);
	}
	
	@Override
	public void onMouseUpdate(MouseUpdateEvent event)
	{
		if(target == null || MC.player == null || nextTickRot == null)
			return;
		
		Rotation curFrameRot =
			new Rotation(MC.player.getYRot(), MC.player.getXRot());
		
		float partialTicks =
			MC.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Rotation nextFrameRot = new Rotation(
			Mth.lerp(partialTicks, curTickRot.yaw(), nextTickRot.yaw()),
			Mth.lerp(partialTicks, curTickRot.pitch(), nextTickRot.pitch()));
		
		double inputFactor = 1 - ignoreMouseInput.getValue();
		double inputX = Math.rint(event.getDefaultDeltaX() * inputFactor);
		double inputY = Math.rint(event.getDefaultDeltaY() * inputFactor);
		
		float yawDiff = nextFrameRot.yaw() - curFrameRot.yaw();
		float pitchDiff = nextFrameRot.pitch() - curFrameRot.pitch();
		double degPerMouseUnit = RotationUtils.getDegreesPerMouseUnit();
		double aimX = Math.rint(yawDiff / degPerMouseUnit);
		double aimY = Math.rint(pitchDiff / degPerMouseUnit);
		
		event.setDeltaX(inputX + aimX);
		event.setDeltaY(inputY + aimY);
		
		if(!MC.mouseHandler.isMouseGrabbed())
			return;
		
		// Carry manual movement into the aim path so later frames don't undo it
		float inputYaw = (float)(inputX * degPerMouseUnit);
		float inputPitch = (float)(inputY * degPerMouseUnit);
		curTickRot = curTickRot.add(inputYaw, inputPitch).clampPitch();
		nextTickRot = nextTickRot.add(inputYaw, inputPitch).clampPitch();
	}
}
