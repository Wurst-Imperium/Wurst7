/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"FastClimb", "fast ladder", "fast climb"})
public final class FastLadderHack extends Hack implements UpdateListener
{
	public FastLadderHack()
	{
		super("FastLadder", "Allows you to climb up ladders faster.");
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		if(!player.isClimbing() || !player.horizontalCollision)
			return;
		
		if(player.input.movementForward == 0
			&& player.input.movementSideways == 0)
			return;
		
		Vec3d velocity = player.getVelocity();
		player.setVelocity(velocity.x, 0.2872, velocity.z);
	}
}
