/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.Random;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;
import net.minecraft.util.hit.EntityHitResult;

public final class AntiFireballHack extends Hack
		implements UpdateListener {
	private final SliderSetting range = new SliderSetting("Range", 5.25, 1, 6, 0.05, ValueDisplay.DECIMAL);

	private int ticks = 0;

	private Entity target;

	public AntiFireballHack() {
		super("AntiFireball");
		setCategory(Category.COMBAT);

		addSetting(range);
	}

	@Override
	protected void onEnable() {
		EVENTS.add(UpdateListener.class, this);
	}

	@Override
	protected void onDisable() {
		EVENTS.remove(UpdateListener.class, this);
		target = null;
	}

	@Override
	public void onUpdate() {
		if (ticks > 0) {
			ticks--;
		}

		// don't attack when a container/inventory screen is open
		if (MC.currentScreen instanceof HandledScreen)
			return;

		ClientPlayerEntity player = MC.player;

		double rangeSq = Math.pow(range.getValue() * 2, 2);
		Stream<Entity> stream = StreamSupport.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> !e.isRemoved())
				.filter(e -> e instanceof AbstractFireballEntity)
				.filter(e -> player.squaredDistanceTo(e) <= rangeSq);

		target = stream.min(Comparator.comparingDouble(e -> MC.player.squaredDistanceTo(e))).orElse(null);
		if (target == null)
			return;

		// check if fireball is heading in direction of player
		Vec3d currentVel = target.getVelocity();
		Vec3d currentPos = target.getBoundingBox().getCenter();
		Vec3d futurePos = currentPos.add(currentVel);

		if (MC.player.getBoundingBox().getCenter().distanceTo(currentPos) <= MC.player.getBoundingBox().getCenter()
				.distanceTo(futurePos))
			return;

		// face entity and check if it's within reach
		if (faceEntityClient(target) && ticks == 0 && player.squaredDistanceTo(target) <= Math.pow(range.getValue(), 2)) {
			// attack entity
			MC.interactionManager.attackEntity(player, target);
			player.swingHand(Hand.MAIN_HAND);

			ticks = 4;
		}
	}

	private boolean faceEntityClient(Entity entity) {
		// get position & rotation
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d lookVec = RotationUtils.getServerLookVec();

		// try to face center of boundingBox
		Box bb = entity.getBoundingBox();

		if (MC.crosshairTarget != null && (MC.crosshairTarget instanceof EntityHitResult)) {
			Entity target = ((EntityHitResult) MC.crosshairTarget).getEntity();

			if (target == entity)
				return true;
		}

		return faceVectorClient(bb.getCenter());
	}

	private boolean faceVectorClient(Vec3d vec) {
		Rotation rotation = RotationUtils.getNeededRotations(vec);

		float oldYaw = MC.player.prevYaw;
		float oldPitch = MC.player.prevPitch;

		float speed = 25;

		MC.player.setYaw(limitAngleChange(oldYaw, rotation.getYaw()));
		MC.player.setPitch(limitAngleChange(oldPitch, rotation.getPitch()));

		return Math.abs(oldYaw - rotation.getYaw())
				+ Math.abs(oldPitch - rotation.getPitch()) < 1F;
	}
	private float limitAngleChange(float current, float intended) {
		float maxChange = Math.min(Math.abs(current - intended) / 2, 45) * (float)(new Random().nextFloat() / 4 + 0.875);

		float changeOld = MathHelper.wrapDegrees(intended - current);
		float change = MathHelper.clamp(changeOld, -maxChange, maxChange);
		if (changeOld == change) {
			change *= 0.8;
		}
		return MathHelper.wrapDegrees(current + change);
	}
}
