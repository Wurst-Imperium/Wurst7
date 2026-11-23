/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.AABB;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PlayerAttacksEntityListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.EntityUtils;

@SearchTags({"mace dmg", "MaceDamage", "mace damage"})
public final class MaceDmgHack extends Hack
	implements PlayerAttacksEntityListener
{
	public MaceDmgHack()
	{
		super("MaceDMG");
		setCategory(Category.COMBAT);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(PlayerAttacksEntityListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PlayerAttacksEntityListener.class, this);
	}
	
	@Override
	public void onPlayerAttacksEntity(Entity target)
	{
		if(!MC.player.getMainHandItem().is(Items.MACE)
			|| MC.player.isFallFlying()
			|| !EntityUtils.IS_ATTACKABLE_MELEE.test(target))
			return;
		
		double fakeFallDistance = findFakeFallDistance();
		if(fakeFallDistance <= 1.5)
			return;
		
		if(MaceItem.canSmashAttack(MC.player)
			&& MC.player.fallDistance > fakeFallDistance)
			return;
		
		for(int i = 0; i < 4; i++)
			sendFakeY(0);
		sendFakeY(fakeFallDistance);
		sendFakeY(0);
	}
	
	private double findFakeFallDistance()
	{
		// Limited by ServerGamePacketListenerImpl.handleMovePlayer()
		double maxDistance = Math.sqrt(500);
		
		AABB playerBox = MC.player.getBoundingBox();
		if(!MC.level.noBlockCollision(MC.player, playerBox)
			|| !MC.level.noEntityCollision(MC.player, playerBox))
			return 0;
		
		for(double distance = maxDistance; distance > 1.5; distance -= 0.25)
		{
			AABB box = playerBox.move(0, distance, 0);
			
			if(!BlockPos.betweenClosedStream(box.deflate(1e-7))
				.allMatch(pos -> MC.level.isOutsideBuildHeight(pos)
					|| MC.level.isLoaded(pos)))
				continue;
			
			if(MC.level.noBlockCollision(MC.player, box)
				&& MC.level.noEntityCollision(MC.player, box))
				return distance;
		}
		
		return 0;
	}
	
	private void sendFakeY(double offset)
	{
		MC.player.connection
			.send(new Pos(MC.player.getX(), MC.player.getY() + offset,
				MC.player.getZ(), false, MC.player.horizontalCollision));
	}
}
