/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.wurstclient.WurstClient;

public class FakePlayerEntity extends RemotePlayer
{
	private final LocalPlayer player = WurstClient.MC.player;
	private final ClientLevel world = WurstClient.MC.level;
	private PlayerInfo playerListEntry;
	
	public FakePlayerEntity()
	{
		super(WurstClient.MC.level, WurstClient.MC.player.getGameProfile());
		setUUID(UUID.randomUUID());
		copyPosition(player);
		
		copyInventory();
		copyRotation();
		
		spawn();
	}
	
	@Override
	protected @Nullable PlayerInfo getPlayerInfo()
	{
		if(playerListEntry == null)
			playerListEntry = Minecraft.getInstance().getConnection()
				.getPlayerInfo(getGameProfile().id());
		
		return playerListEntry;
	}
	
	@Override
	protected void doPush(Entity entity)
	{
		// Prevents pushing the real player away
	}
	
	private void copyInventory()
	{
		getInventory().replaceWith(player.getInventory());
	}
	
	private void copyRotation()
	{
		yHeadRot = player.yHeadRot;
		yBodyRot = player.yBodyRot;
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
		player.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
	}
}
