/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Comparator;
import java.util.stream.StreamSupport;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.MathUtils;

public final class TpCmd extends Command
{
	private final CheckboxSetting disableFreecam = new CheckboxSetting(
		"Disable Freecam", "Disables Freecam just before teleporting.", false);
	
	public TpCmd()
	{
		super("tp", "Teleports you up to 22 blocks away.", ".tp <x> <y> <z>",
			".tp <entity>");
		addSetting(disableFreecam);
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		BlockPos pos = argsToPos(args);
		LocalPlayer player = MC.player;
		
		if(disableFreecam.isChecked() && WURST.getHax().freecamHack.isEnabled())
			WURST.getHax().freecamHack.setEnabled(false);
		
		// Simple teleport at low distances for better stability
		if(player.distanceToSqr(pos.getBottomCenter()) < 100)
		{
			player.setPos(pos.getX(), pos.getY(), pos.getZ());
			return;
		}
		
		// See ServerGamePacketListenerImpl.handleMovePlayer()
		// for why it's using these numbers.
		// Also, let me know if you find a way to bypass that check.
		for(int i = 0; i < 4; i++)
			sendPos(player.getX(), player.getY(), player.getZ(), true);
		sendPos(pos.getX(), pos.getY(), pos.getZ(), true);
		sendPos(pos.getX(), pos.getY(), pos.getZ(), false);
	}
	
	private void sendPos(double x, double y, double z, boolean onGround)
	{
		ClientPacketListener netHandler = MC.player.connection;
		netHandler.send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround,
			MC.player.horizontalCollision));
	}
	
	private BlockPos argsToPos(String... args) throws CmdException
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
	
	private BlockPos argsToEntityPos(String name) throws CmdError
	{
		LivingEntity entity = StreamSupport
			.stream(MC.level.entitiesForRendering().spliterator(), true)
			.filter(LivingEntity.class::isInstance).map(e -> (LivingEntity)e)
			.filter(e -> !e.isRemoved() && e.getHealth() > 0)
			.filter(e -> e != MC.player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> name.equalsIgnoreCase(e.getDisplayName().getString()))
			.min(Comparator.comparingDouble(e -> MC.player.distanceToSqr(e)))
			.orElse(null);
		
		if(entity == null)
			throw new CmdError("Entity \"" + name + "\" could not be found.");
		
		return BlockPos.containing(entity.position());
	}
	
	private BlockPos argsToXyzPos(String... xyz) throws CmdSyntaxError
	{
		BlockPos playerPos = BlockPos.containing(MC.player.position());
		int[] player = {playerPos.getX(), playerPos.getY(), playerPos.getZ()};
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
			
		return new BlockPos(pos[0], pos[1], pos[2]);
	}
}
