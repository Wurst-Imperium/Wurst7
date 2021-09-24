/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class RepairCmd extends Command
{
	public RepairCmd()
	{
		super("repair", "修理持有的物品 仅创造模式",
			".repair");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length > 0)
			throw new CmdSyntaxError();
		
		ClientPlayerEntity player = MC.player;
		
		if(!player.getAbilities().creativeMode)
			throw new CmdError("仅限创造模式.");
		
		ItemStack stack = getHeldStack(player);
		stack.setDamage(0);
		MC.player.networkHandler
			.sendPacket(new CreativeInventoryActionC2SPacket(
				36 + player.getInventory().selectedSlot, stack));
		
		ChatUtils.message("物品已修复.");
	}
	
	private ItemStack getHeldStack(ClientPlayerEntity player) throws CmdError
	{
		ItemStack stack = player.getInventory().getMainHandStack();
		
		if(stack.isEmpty())
			throw new CmdError("您需要手上的物品.");
		
		if(!stack.isDamageable())
			throw new CmdError("此物品不会受到损坏.");
		
		if(!stack.isDamaged())
			throw new CmdError("此商品未损坏.");
		
		return stack;
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "Repair Current Item";
	}
	
	@Override
	public void doPrimaryAction()
	{
		WURST.getCmdProcessor().process("repair");
	}
}
