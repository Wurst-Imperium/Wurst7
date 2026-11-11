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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.wurstclient.Category;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.events.MouseUpdateListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.AimAtSetting;
import net.wurstclient.settings.AimAtSetting.AimAt;
import net.wurstclient.settings.AttackSpeedSliderSetting;
import net.wurstclient.settings.AttackSwingSetting;
import net.wurstclient.settings.AttackSwingSetting.AttackSwing;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.FovSliderSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.settings.filters.*;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.Rotation;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.text.WText;

public final class KillauraLegitHack extends Hack implements UpdateListener,
	PreMotionListener, HandleInputListener, MouseUpdateListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 4.25, 1, 4.25, 0.05, ValueDisplay.DECIMAL);
	
	private final AttackSpeedSliderSetting speed =
		new AttackSpeedSliderSetting();
	
	private final SliderSetting speedRandMS =
		new SliderSetting("Speed randomization",
			"Helps you bypass anti-cheat plugins by varying the delay between"
				+ " attacks.\n\n" + "\u00b1100ms is recommended for Vulcan.\n\n"
				+ "0 (off) is fine for NoCheat+, AAC, Grim, Verus, Spartan, and"
				+ " vanilla servers.",
			100, 0, 1000, 50, ValueDisplay.INTEGER.withPrefix("\u00b1")
				.withSuffix("ms").withLabel(0, "off"));
	
	private final SliderSetting rotationSpeed =
		new SliderSetting("Rotation Speed", 600, 10, 3600, 10,
			ValueDisplay.DEGREES.withSuffix("/s"));
	
	private final EnumSetting<Priority> priority = new EnumSetting<>("Priority",
		"Determines which entity will be attacked first.\n"
			+ "\u00a7lDistance\u00a7r - Attacks the closest entity.\n"
			+ "\u00a7lAngle\u00a7r - Attacks the entity that requires the least head movement.\n"
			+ "\u00a7lHealth\u00a7r - Attacks the weakest entity.",
		Priority.values(), Priority.ANGLE);
	
	private final FovSliderSetting fov = FovSliderSetting.genericCombat(360);
	
	private final AimAtSetting aimAt =
		new AimAtSetting(AimAtSetting.genericDescription(this), AimAt.AUTO);
	
	private final SliderSetting ignoreMouseInput =
		new SliderSetting("Ignore mouse input",
			WText.translated(
				"description.wurst.setting.generic.ignore_mouse_input",
				getName()),
			0, 0, 1, 0.01, ValueDisplay.PERCENTAGE);
	
	private final AttackSwingSetting attackSwing = AttackSwingSetting
		.withoutOffOption(AttackSwingSetting.genericCombatDescription(this),
			AttackSwing.CLIENT);
	
	private final CheckboxSetting damageIndicator = new CheckboxSetting(
		"Damage indicator",
		"Renders a colored box within the target, inversely proportional to its remaining health.",
		true);
	
	// same filters as in Killaura, but with stricter defaults
	private final EntityFilterList entityFilters =
		new EntityFilterList(FilterPlayersSetting.genericCombat(false),
			FilterCreativeSetting.genericCombat(true),
			FilterSleepingSetting.genericCombat(true),
			FilterFlyingSetting.genericCombat(0.5),
			FilterHostileSetting.genericCombat(false),
			FilterNeutralSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterPassiveSetting.genericCombat(false),
			FilterPassiveWaterSetting.genericCombat(false),
			FilterBabiesSetting.genericCombat(false),
			FilterBatsSetting.genericCombat(false),
			FilterSlimesSetting.genericCombat(false),
			FilterPetsSetting.genericCombat(false),
			FilterVillagersSetting.genericCombat(false),
			FilterZombieVillagersSetting.genericCombat(false),
			FilterGolemsSetting.genericCombat(false),
			FilterPiglinsSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterZombiePiglinsSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterEndermenSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterShulkersSetting.genericCombat(false),
			FilterAllaysSetting.genericCombat(false),
			FilterInvisibleSetting.genericCombat(true),
			FilterNamedSetting.genericCombat(false),
			FilterProjectilesSetting.genericCombat(false),
			FilterArmorStandsSetting.genericCombat(false),
			FilterCrystalsSetting.genericCombat(false),
			FilterInteractionsSetting.genericCombat(true));
	
	private Entity target;
	private Rotation curTickRot;
	private Rotation nextTickRot;
	
	public KillauraLegitHack()
	{
		super("KillauraLegit");
		setCategory(Category.COMBAT);
		
		addSetting(range);
		addSetting(speed);
		addSetting(speedRandMS);
		addSetting(rotationSpeed);
		addSetting(priority);
		addSetting(fov);
		addSetting(aimAt);
		addSetting(ignoreMouseInput);
		addSetting(attackSwing);
		addSetting(damageIndicator);
		
		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		// disable incompatible hacks
		WURST.getHax().aimAssistHack.setEnabled(false);
		WURST.getHax().autoFishHack.setEnabled(false);
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().crystalAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().triggerBotHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
		speed.resetTimer(speedRandMS.getValue());
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PreMotionListener.class, this);
		EVENTS.add(HandleInputListener.class, this);
		EVENTS.add(MouseUpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PreMotionListener.class, this);
		EVENTS.remove(HandleInputListener.class, this);
		EVENTS.remove(MouseUpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		target = null;
		curTickRot = null;
		nextTickRot = null;
	}
	
	@Override
	public void onUpdate()
	{
		target = null;
		
		// don't attack when a container/inventory screen is open
		if(MC.gui.screen() instanceof AbstractContainerScreen)
			return;
		
		Stream<Entity> stream = EntityUtils.getMeleeAttackableEntities();
		double rangeSq = range.getValueSq();
		stream =
			stream.filter(e -> EntityUtils.distanceToHitboxSq(e) <= rangeSq)
				.filter(fov.asEntityPredicate(aimAt::getAimPoint));
		
		stream = entityFilters.applyTo(stream);
		
		target = stream.min(priority.getSelected().getComparator(aimAt))
			.orElse(null);
		if(target == null)
			return;
		
		// check line of sight
		if(!BlockUtils.hasLineOfSight(aimAt.getAimPoint(target)))
		{
			target = null;
			return;
		}
		
		if(!MC.player.isUsingItem())
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
		Rotation needed =
			RotationUtils.getNeededRotations(aimAt.getAimPoint(target));
		
		nextTickRot = curTickRot.slowlyTurnTowards(needed,
			rotationSpeed.getValueI() / 20F);
		
		// Vulcan D/F flag >1.825° yaw changes with unchanged pitch
		if(Math.abs(nextTickRot.yaw() - curTickRot.yaw()) > 1.825F
			&& nextTickRot.pitch() == curTickRot.pitch())
		{
			float pitchOffset = (float)(RotationUtils.getDegreesPerMouseUnit()
				* (Math.random() < 0.5 ? -1 : 1));
			nextTickRot = nextTickRot.withPitch(
				Math.clamp(nextTickRot.pitch() + pitchOffset, -90F, 90F));
		}
	}
	
	@Override
	public void onHandleInput()
	{
		if(target == null || MC.player.isUsingItem())
			return;
		
		speed.updateTimer();
		if(!speed.isTimeToAttack())
			return;
		
		if(!RotationUtils.isFacingBoxServerSide(target.getBoundingBox(),
			range.getValue()))
			return;
		
		// attack entity
		MC.gameMode.attack(MC.player, target);
		attackSwing.swing();
		speed.resetTimer(speedRandMS.getValue());
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
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(target == null || !damageIndicator.isChecked())
			return;
		
		float p = 1;
		if(target instanceof LivingEntity le)
			p = (le.getMaxHealth() - le.getHealth()) / le.getMaxHealth();
		float red = p * 2F;
		float green = 2 - red;
		float[] rgb = {red, green, 0};
		int quadColor = RenderUtils.toIntColor(rgb, 0.25F);
		int lineColor = RenderUtils.toIntColor(rgb, 0.5F);
		
		AABB box = EntityUtils.getLerpedBox(target, partialTicks);
		if(p < 1)
			box = box.deflate((1 - p) * 0.5 * box.getXsize(),
				(1 - p) * 0.5 * box.getYsize(), (1 - p) * 0.5 * box.getZsize());
		
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}
	
	private enum Priority
	{
		DISTANCE("Distance"),
		ANGLE("Angle"),
		HEALTH("Health");
		
		private final String name;
		
		private Priority(String name)
		{
			this.name = name;
		}
		
		public Comparator<Entity> getComparator(AimAtSetting aimAt)
		{
			return switch(this)
			{
				case DISTANCE -> Comparator
					.comparingDouble(EntityUtils::distanceToHitboxSq);
				case ANGLE -> Comparator.comparingDouble(e -> RotationUtils
					.getYawPitchDistanceToLookVec(aimAt.getAimPoint(e)));
				case HEALTH -> Comparator
					.comparingDouble(e -> e instanceof LivingEntity le
						? le.getHealth() : Integer.MAX_VALUE);
			};
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
