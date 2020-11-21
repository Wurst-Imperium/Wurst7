/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.stream.Stream;

import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

public final class ParkourHack extends Hack implements UpdateListener
{
	public ParkourHack()
	{
		super("Parkour",
			"Makes you jump automatically when reaching\n"
				+ "the edge of a block.\n"
				+ "Useful for parkours and jump'n'runs.");
		
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().safeWalkHack.setEnabled(false);
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
		if(!MC.player.isOnGround() || MC.options.keyJump.isPressed())
			return;
		
		if(MC.player.isSneaking() || MC.options.keySneak.isPressed())
			return;
		
		Box box = MC.player.getBoundingBox();
		Box adjustedBox = box.offset(0, -0.5, 0).expand(-0.001, 0, -0.001);
		
		Stream<VoxelShape> blockCollisions =
			MC.world.getBlockCollisions(MC.player, adjustedBox);
		
		if(blockCollisions.findAny().isPresent())
			return;
		
		MC.player.jump();
	}
}
