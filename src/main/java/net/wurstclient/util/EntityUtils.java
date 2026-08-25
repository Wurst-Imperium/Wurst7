/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;

public enum EntityUtils
{
	;
	
	protected static final WurstClient WURST = WurstClient.INSTANCE;
	protected static final Minecraft MC = WurstClient.MC;
	
	public static Stream<Entity> getEntities()
	{
		return StreamSupport
			.stream(MC.level.entitiesForRendering().spliterator(), false);
	}
	
	public static <E extends Entity> Stream<E> getEntities(Class<E> entityClass)
	{
		return getEntities().filter(entityClass::isInstance)
			.map(entityClass::cast);
	}
	
	public static Stream<Entity> getAliveEntities()
	{
		return getEntities().filter(Entity::isAlive);
	}
	
	public static <E extends Entity> Stream<E> getAliveEntities(
		Class<E> entityClass)
	{
		return getEntities(entityClass).filter(Entity::isAlive);
	}
	
	public static Stream<Entity> getFollowableEntities()
	{
		return getAliveEntities().filter(IS_NOT_SELF).filter(
			e -> e instanceof LivingEntity || e instanceof AbstractMinecart);
	}
	
	public static final Predicate<Entity> IS_NOT_SELF =
		e -> e != null && e != MC.player && !(e instanceof FakePlayerEntity);
	
	public static Stream<Entity> getAttackableEntities()
	{
		return getEntities().filter(IS_ATTACKABLE);
	}
	
	/**
	 * Same as {@link #getAttackableEntities()} but excludes end crystals and
	 * projectiles.
	 */
	public static Stream<LivingEntity> getExplosionWorthyAttackableEntities()
	{
		return getEntities(LivingEntity.class).filter(IS_ATTACKABLE);
	}
	
	public static final Predicate<Entity> IS_ATTACKABLE =
		e -> e != null && e.isAlive()
			&& (e instanceof LivingEntity || e instanceof EndCrystal
				|| e instanceof ShulkerBullet)
			&& IS_NOT_SELF.test(e) && !WURST.getFriends().isFriend(e);
	
	/**
	 * Interpolates (or "lerps") between the entity's position in the previous
	 * tick and its position in the current tick to get the exact position where
	 * the entity will be rendered in the next frame.
	 *
	 * <p>
	 * This interpolation is important for smooth animations. Using the entity's
	 * current tick position directly would cause animations to look choppy
	 * because that position is only updated 20 times per second.
	 */
	public static Vec3 getLerpedPos(Entity e, float partialTicks)
	{
		// When an entity is removed, it stops moving and its lastRenderX/Y/Z
		// values are no longer updated.
		if(e.isRemoved())
			return e.position();
		
		double x = Mth.lerp(partialTicks, e.xOld, e.getX());
		double y = Mth.lerp(partialTicks, e.yOld, e.getY());
		double z = Mth.lerp(partialTicks, e.zOld, e.getZ());
		return new Vec3(x, y, z);
	}
	
	/**
	 * Interpolates (or "lerps") between the entity's bounding box in the
	 * previous tick and its bounding box in the current tick to get the exact
	 * bounding box that the entity will have in the next frame.
	 *
	 * <p>
	 * This interpolation is important for smooth animations. Using the entity's
	 * current tick bounding box directly would cause animations to look choppy
	 * because that box, just like the position, is only updated 20 times per
	 * second.
	 */
	public static AABB getLerpedBox(Entity e, float partialTicks)
	{
		// When an entity is removed, it stops moving and its lastRenderX/Y/Z
		// values are no longer updated.
		if(e.isRemoved())
			return e.getBoundingBox();
		
		Vec3 offset = getLerpedPos(e, partialTicks).subtract(e.position());
		return e.getBoundingBox().move(offset);
	}
	
	public static double distanceToHitboxSq(Entity e)
	{
		Vec3 start = RotationUtils.getEyesPos();
		AABB box = e.getBoundingBox();
		double x = Mth.clamp(start.x, box.minX, box.maxX);
		double y = Mth.clamp(start.y, box.minY, box.maxY);
		double z = Mth.clamp(start.z, box.minZ, box.maxZ);
		return start.distanceToSqr(new Vec3(x, y, z));
	}
	
	public static EntityHitResult createHitResult(Entity e)
	{
		AABB box = e.getBoundingBox();
		Vec3 start = RotationUtils.getEyesPos();
		Vec3 end = box.getCenter();
		Vec3 hitVec = box.clip(start, end).orElse(start);
		return new EntityHitResult(e, hitVec);
	}
}
