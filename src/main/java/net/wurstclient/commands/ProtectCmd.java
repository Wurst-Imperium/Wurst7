/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Comparator;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hacks.ProtectHack;
import net.wurstclient.util.EntityUtils;

public final class ProtectCmd extends Command
{
	public ProtectCmd()
	{
		super("protect", "Protects the given entity from other entities.",
			".protect <entity>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 1)
			throw new CmdSyntaxError();
		
		ProtectHack protectHack = WURST.getHax().protectHack;
		
		if(protectHack.isEnabled())
			protectHack.setEnabled(false);
		
		Entity entity = EntityUtils.getAliveEntities(LivingEntity.class)
			.filter(EntityUtils.IS_NOT_SELF)
			.filter(e -> args[0].equalsIgnoreCase(e.getName().getString()))
			.min(Comparator.comparingDouble(EntityUtils::distanceToHitboxSq))
			.orElse(null);
		
		if(entity == null)
			throw new CmdError(
				"Entity \"" + args[0] + "\" could not be found.");
		
		protectHack.setFriend(entity);
		protectHack.setEnabled(true);
	}
}
