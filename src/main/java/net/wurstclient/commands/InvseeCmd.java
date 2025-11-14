/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.RenderListener;
import net.wurstclient.util.ChatUtils;

public final class InvseeCmd extends Command implements RenderListener
{
	private String targetName;
	
	public InvseeCmd()
	{
		super("invsee",
			"Allows you to see parts of another player's inventory.",
			".invsee <player>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 1)
			throw new CmdSyntaxError();
		
		if(MC.player.getAbilities().instabuild)
		{
			ChatUtils.error("Survival mode only.");
			return;
		}
		
		targetName = args[0];
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		boolean found = false;
		
		for(Entity entity : MC.level.entitiesForRendering())
		{
			if(!(entity instanceof RemotePlayer))
				continue;
			
			RemotePlayer player = (RemotePlayer)entity;
			
			String otherPlayerName = player.getName().getString();
			if(!otherPlayerName.equalsIgnoreCase(targetName))
				continue;
			
			ChatUtils.message("Showing inventory of " + otherPlayerName + ".");
			MC.setScreen(new InventoryScreen(player));
			found = true;
			break;
		}
		
		if(!found)
			ChatUtils.error("Player not found.");
		
		targetName = null;
		EVENTS.remove(RenderListener.class, this);
	}
}
