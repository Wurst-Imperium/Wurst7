/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.MathUtils;

public final class VClipCmd extends Command
{
	public VClipCmd()
	{
		super("vclip", "Lets you clip through blocks vertically.\n"
			+ "The maximum distance is 10 blocks.", ".vclip <height> or .vclip <above | below>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 1)
			throw new CmdSyntaxError();
		
		ClientPlayerEntity player = MC.player;
		
		if(!MathUtils.isInteger(args[0])) {
			Stream<BlockPos> blockStream = null;
			boolean above;
			switch (args[0].toLowerCase()) 
			{
				case "above":
					blockStream = BlockUtils.getAllInBoxStream(player.getBlockPos().up(2), player.getBlockPos().up(10));
					above = true;
					break;
				
				case "below":
					blockStream = BlockUtils.getAllInBoxStream(player.getBlockPos().down(10), player.getBlockPos().down());
					above = false;
					break;
				
				default:
					throw new CmdSyntaxError();
			}
			List<BlockPos> blockList = blockStream.filter(pos -> above ? BlockUtils.getState(pos.down()).getMaterial().isSolid() : true)
						.filter(pos -> player.getPose() == EntityPose.SWIMMING ? 
								BlockUtils.getState(pos.up()).isAir() :
								BlockUtils.getState(pos).isAir() && BlockUtils.getState(pos.up()).isAir()
							)
						.sorted(Comparator.comparingDouble(p -> player.squaredDistanceTo(Vec3d.of(p))))
						.limit(1).toList();
			if (!blockList.isEmpty()) {
				player.updatePosition(player.getX(),blockList.get(0).getY(),player.getZ());
			}
			else {ChatUtils.error("There are no free blocks where you can fit!");}
			return;
		}
		
		player.setPosition(player.getX(),
			player.getY() + Integer.parseInt(args[0]), player.getZ());
		
	}
}
