/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.wurstclient.WurstClient;

public class FakePlayerEntity extends OtherClientPlayerEntity
{
	private final ClientPlayerEntity player = WurstClient.MC.player;
	private final ClientWorld world = WurstClient.MC.world;
	private PlayerListEntry playerListEntry;
	
	public FakePlayerEntity()
	{
		super(WurstClient.MC.world, WurstClient.MC.player.getGameProfile());
		setUuid(UUID.randomUUID());
		copyPositionAndRotation(player);
		
		copyInventory();
		copyRotation();
		
		spawn();
	}
	
	@Override
	protected @Nullable PlayerListEntry getPlayerListEntry()
	{
		if(playerListEntry == null)
			playerListEntry = MinecraftClient.getInstance().getNetworkHandler()
				.getPlayerListEntry(getGameProfile().id());
		
		return playerListEntry;
	}
	
	@Override
	protected void pushAway(Entity entity)
	{
		// Prevents pushing the real player away
	}
	
	private void copyInventory()
	{
		getInventory().clone(player.getInventory());
	}
	
	private void copyRotation()
	{
		headYaw = player.headYaw;
		bodyYaw = player.bodyYaw;
	}
	
	private void spawn()
	{
		world.addEntity(this);
	}
	
	public void despawn()
	{
		discard();
	}
	
	public void resetPlayerPosition()
	{
		player.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(),
			getPitch());
	}
}
