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
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.BlinkHack;

public class FakePlayerEntity extends OtherClientPlayerEntity
{
	private final ClientPlayerEntity player = WurstClient.MC.player;
	private final ClientWorld world = WurstClient.MC.world;
	public String wurstName;

	public FakePlayerEntity(FakePlayerEntity fakePlayer) {
		super(WurstClient.MC.world, WurstClient.MC.player.getGameProfile());
		copyPositionAndRotation(fakePlayer);

		copyInventory();
		copyPlayerModel(player, this);
		copyRotation(fakePlayer);
		resetCapeMovement();

		setName(this.getGameProfile().getName());
		spawn();



	}

	public FakePlayerEntity()
	{
		super(WurstClient.MC.world, WurstClient.MC.player.getGameProfile());
		copyPositionAndRotation(player);
		
		copyInventory();
		copyPlayerModel(player, this);
		copyRotation();
		resetCapeMovement();
		
		spawn();
	}
	
	private void copyInventory()
	{
		inventory.clone(player.inventory);
	}
	
	private void copyPlayerModel(Entity from, Entity to)
	{
		DataTracker fromTracker = from.getDataTracker();
		DataTracker toTracker = to.getDataTracker();
		Byte playerModel = fromTracker.get(PlayerEntity.PLAYER_MODEL_PARTS);
		toTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
	}

	private void copyRotation(FakePlayerEntity fakePlayer) {
		headYaw = fakePlayer.headYaw;
		bodyYaw = fakePlayer.bodyYaw;
	}
	
	private void copyRotation()
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

	public void animateLimbs() {
		this.lastLimbDistance = this.limbDistance;
		double d = this.getX() - this.prevX;
		double z = this.getZ() - this.prevZ;
		float g = MathHelper.sqrt(d * d + z * z) * 4.0F;
		if (g > 1.0F) {
			g = 1.0F;
		}

		this.limbDistance += (g - this.limbDistance) * 0.4F;
		this.limbAngle += this.limbDistance / 100;
	}

	@Environment(EnvType.CLIENT)
	public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
		double d = this.getX() - cameraX;
		double e = this.getY() - cameraY;
		double f = this.getZ() - cameraZ;
		double g = d * d + e * e + f * f;
		return this.shouldRender(g);
	}

	@Environment(EnvType.CLIENT)
	public boolean shouldRender(double distance) {
		boolean inRange = super.shouldRender(distance);
		if (!inRange)
			return false;
		return distance > 3.3D; // 3 block so it does not block your view
	}

	// Override name rendering
	public void setName(String name) {
		this.wurstName = name;
	}

	public Text getName() {
		return new LiteralText(this.wurstName);
	}

	public String getEntityName() { return this.wurstName; }

	public Text getDisplayName() { return new LiteralText(this.wurstName); }

	protected void tickCramming() {
		// Dont interact with the player
	}
	
	private void spawn()
	{
		world.addEntity(getEntityId(), this);
	}
	
	public void despawn()
	{
		removed = true;
	}
	
	public void resetPlayerPosition()
	{
		player.refreshPositionAndAngles(getX(), getY(), getZ(), yaw, pitch);
	}
}
