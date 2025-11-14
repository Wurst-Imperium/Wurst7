/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.MathUtils;

public final class VClipCmd extends Command
{
	public VClipCmd()
	{
		super("vclip",
			"Lets you clip through blocks vertically.\n"
				+ "The maximum distance is 10 blocks.",
			".vclip <height>", ".vclip (up|down)");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 1)
			throw new CmdSyntaxError();
		
		if(MathUtils.isDouble(args[0]))
		{
			vclip(Double.parseDouble(args[0]));
			return;
		}
		
		switch(args[0].toLowerCase())
		{
			case "up":
			vclip(calculateHeight(Direction.UP));
			break;
			
			case "down":
			vclip(calculateHeight(Direction.DOWN));
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
	
	private double calculateHeight(Direction direction) throws CmdError
	{
		AABB box = MC.player.getBoundingBox();
		
		AABB maxOffsetBox = box.move(0, direction.getStepY() * 10, 0);
		if(!hasCollisions(box.minmax(maxOffsetBox)))
			throw new CmdError("There is nothing to clip through!");
		
		for(int i = 1; i <= 10; i++)
		{
			double height = direction.getStepY() * i;
			AABB offsetBox = box.move(0, height, 0);
			
			if(hasCollisions(offsetBox))
			{
				double subBlockOffset = getSubBlockOffset(offsetBox);
				if(subBlockOffset >= 1 || height + subBlockOffset > 10)
					continue;
				
				AABB newOffsetBox = offsetBox.move(0, subBlockOffset, 0);
				if(hasCollisions(newOffsetBox))
					continue;
				
				height += subBlockOffset;
				offsetBox = newOffsetBox;
			}
			
			if(!hasCollisions(box.minmax(offsetBox)))
				continue;
			
			return height;
		}
		
		throw new CmdError("There are no free blocks where you can fit!");
	}
	
	private boolean hasCollisions(AABB box)
	{
		return BlockUtils.getBlockCollisions(box).findAny().isPresent();
	}
	
	private double getSubBlockOffset(AABB offsetBox)
	{
		return BlockUtils.getBlockCollisions(offsetBox)
			.mapToDouble(box -> box.maxY).max().getAsDouble() - offsetBox.minY;
	}
	
	private void vclip(double height)
	{
		LocalPlayer p = MC.player;
		p.setPos(p.getX(), p.getY() + height, p.getZ());
	}
}
