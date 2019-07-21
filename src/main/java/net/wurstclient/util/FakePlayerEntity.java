/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.wurstclient.WurstClient;

public class FakePlayerEntity extends OtherClientPlayerEntity
{
	private final ClientPlayerEntity player = WurstClient.MC.player;
	private final ClientWorld world = WurstClient.MC.world;
	
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
		Byte playerModel = fromTracker.get(PlayerEntity.PLAYER_MODEL_BIT_MASK);
		toTracker.set(PlayerEntity.PLAYER_MODEL_BIT_MASK, playerModel);
	}
	
	private void copyRotation()
	{
		headYaw = player.headYaw;
		field_6283 = player.field_6283;
	}
	
	private void resetCapeMovement()
	{
		field_7500 = x;
		field_7521 = y;
		field_7499 = z;
	}
	
	private void spawn()
	{
		world.addEntity(getEntityId(), this);
	}
	
	public void despawn()
	{
		world.removeEntity(getEntityId());
	}
	
	public void resetPlayerPosition()
	{
		player.setPositionAndAngles(x, y, z, yaw, pitch);
	}
}
