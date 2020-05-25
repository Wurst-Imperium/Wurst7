/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import net.wurstclient.WurstClient;

import java.util.List;
import java.util.function.Predicate;

public class FakePlayerEntity extends OtherClientPlayerEntity
{
	private final ClientWorld world = WurstClient.MC.world;
	public String fakeName = "";
	private boolean shouldRender = false;

	public FakePlayerEntity() { this(WurstClient.MC.player); }
	public FakePlayerEntity(PlayerEntity player) {
		super(WurstClient.MC.world, player.getGameProfile());

		copyPositionAndRotation(player);
		copyInventory(player);
		copyPlayerModel(player);
		copyRotation(player);
		resetCapeMovement();

		spawn();
	}

	private void copyInventory(PlayerEntity player) { inventory.clone(player.inventory); }

	private void copyPlayerModel(Entity from)
	{
		DataTracker fromTracker = from.getDataTracker();
		DataTracker toTracker = this.getDataTracker();
		Byte playerModel = fromTracker.get(PlayerEntity.PLAYER_MODEL_PARTS);
		toTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
	}

	private void copyRotation(PlayerEntity player)
	{
		headYaw = player.headYaw;
		bodyYaw = player.bodyYaw;
	}

	private void resetCapeMovement()
	{
		field_7500 = getX();
		field_7521 = getY();
		field_7499 = getZ();
	}

	// The limbs don't render normally for some reason
//	public void animateLimbs()
//	{
//		this.lastLimbDistance = this.limbDistance;
//		double d = this.getX() - this.prevX;
//		double z = this.getZ() - this.prevZ;
//		float g = MathHelper.sqrt(d * d + z * z) * 4.0F;
//		if (g > 1.0F) {
//			g = 1.0F;
//		}
//
//		this.limbDistance += (g - this.limbDistance) * 0.4F;
//		this.limbAngle += this.limbDistance / 100;
//	}

	// Don't render if touching to the real player (see FakePlayerEntity.tickCramming())
//	@Environment(EnvType.CLIENT)
//	public boolean shouldRender(double distance) { return super.shouldRender(distance) && shouldRender; }

	// Override name rendering
	public void setName(String name) { this.fakeName = name; }
	public Text getName() { return new LiteralText(this.fakeName); }
	public String getEntityName() { return this.fakeName; }
	public Text getDisplayName() { return new LiteralText(this.fakeName); }

	// This stops it from pushing other entities and also tells the shouldRender function
	protected void tickCramming() // to not render if the real player is intersecting it
	{
		List<Entity> list = this.world.getEntities((Entity)this, this.getBoundingBox(), (p -> p == WurstClient.MC.player));
		if (!list.isEmpty())
			this.shouldRender = false;
	}

	// Public version of identical Protected method
	public void setRotation(float yaw, float pitch) {
		this.yaw = yaw % 360.0F;
		this.pitch = pitch % 360.0F;
	}
	
	private void spawn() { world.addEntity(getEntityId(), this); }
	public void despawn() { removed = true; }

	// Teleport the real player to the fake player
	public void resetPlayerPosition(PlayerEntity player) { player.refreshPositionAndAngles(getX(), getY(), getZ(), yaw, pitch); }
	public void resetPlayerPosition() { resetPlayerPosition(WurstClient.MC.player); }
}
