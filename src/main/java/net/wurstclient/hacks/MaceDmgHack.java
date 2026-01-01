/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PlayerAttacksEntityListener;
import net.wurstclient.hack.Hack;

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
		if(MC.hitResult == null
			|| MC.hitResult.getType() != HitResult.Type.ENTITY)
			return;
		
		if(!MC.player.getMainHandItem().is(Items.MACE))
			return;
			
		// See ServerPlayNetworkHandler.onPlayerMove()
		// for why it's using these numbers.
		// Also, let me know if you find a way to bypass that check in 1.21.
		for(int i = 0; i < 4; i++)
			sendFakeY(0);
		sendFakeY(Math.sqrt(500));
		sendFakeY(0);
	}
	
	private void sendFakeY(double offset)
	{
		MC.player.connection
			.send(new Pos(MC.player.getX(), MC.player.getY() + offset,
				MC.player.getZ(), false, MC.player.horizontalCollision));
	}
}
