/*
 * Copyright (C) 2014 - 2020 | Jakiki6 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Comparator;
import java.util.stream.StreamSupport;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;

public final class FaceCmd extends Command
{
	public FaceCmd()
	{
		super("face", "Rotates you to a block", ".face <x> <y> <z>",
			".face <entity>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		Rotation pos = argsToPos(args);
		
		ClientPlayerEntity player = MC.player;
		player.yaw = pos.getYaw();
		player.pitch = pos.getPitch();
	}
	
	private Rotation argsToPos(String... args) throws CmdException
	{
		switch(args.length)
		{
			default:
			throw new CmdSyntaxError("Invalid coordinates.");
			
			case 1:
			return argsToEntityPos(args[0]);
			
			case 3:
			return argsToXyzPos(args);
		}
	}
	
	private Rotation argsToEntityPos(String name) throws CmdError
	{
		LivingEntity entity = StreamSupport
			.stream(MC.world.getEntities().spliterator(), true)
			.filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity)e)
			.filter(e -> !e.removed && e.getHealth() > 0)
			.filter(e -> e != MC.player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> name.equalsIgnoreCase(e.getDisplayName().asString()))
			.min(
				Comparator.comparingDouble(e -> MC.player.squaredDistanceTo(e)))
			.orElse(null);
		
		if(entity == null)
			throw new CmdError("Entity \"" + name + "\" could not be found.");
		
		BlockPos pos = new BlockPos(entity.getPos());
		return RotationUtils.getNeededRotations(new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
	}
	
	private Rotation argsToXyzPos(String... xyz) throws CmdSyntaxError
	{
		BlockPos playerPos = new BlockPos(MC.player.getPos());
		int[] player =
			new int[]{playerPos.getX(), playerPos.getY(), playerPos.getZ()};
		int[] pos = new int[3];
		
		for(int i = 0; i < 3; i++)
			if(MathUtils.isInteger(xyz[i]))
				pos[i] = Integer.parseInt(xyz[i]);
			else if(xyz[i].equals("~"))
				pos[i] = player[i];
			else if(xyz[i].startsWith("~")
				&& MathUtils.isInteger(xyz[i].substring(1)))
				pos[i] = player[i] + Integer.parseInt(xyz[i].substring(1));
			else
				throw new CmdSyntaxError("Invalid coordinates.");
			
		return RotationUtils.getNeededRotations(new Vec3d(pos[0], pos[1], pos[2]));
	}
}
