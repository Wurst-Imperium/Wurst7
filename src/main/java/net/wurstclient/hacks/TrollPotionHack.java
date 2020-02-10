/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.LiteralText;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;

@SearchTags({"troll potion"})
public final class TrollPotionHack extends Hack
{
	public TrollPotionHack()
	{
		super("TrollPotion",
			"Generates a potion with many annoying effects on it.");
		setCategory(Category.ITEMS);
	}
	
	@Override
	public void onEnable()
	{
		// check gamemode
		if(!MC.player.abilities.creativeMode)
		{
			ChatUtils.error("Creative mode only.");
			setEnabled(false);
			return;
		}
		
		// generate potion
		ItemStack stack = new ItemStack(Items.SPLASH_POTION);
		ListTag effects = new ListTag();
		for(int i = 1; i <= 23; i++)
		{
			CompoundTag effect = new CompoundTag();
			effect.putInt("Amplifier", Integer.MAX_VALUE);
			effect.putInt("Duration", Integer.MAX_VALUE);
			effect.putInt("Id", i);
			effects.add(effect);
		}
		CompoundTag nbt = new CompoundTag();
		nbt.put("CustomPotionEffects", effects);
		stack.setTag(nbt);
		String name = "\u00a7rSplash Potion of Trolling";
		stack.setCustomName(new LiteralText(name));
		
		// give potion
		if(placeStackInHotbar(stack))
			ChatUtils.message("Potion created.");
		else
			ChatUtils.error("Please clear a slot in your hotbar.");
		
		setEnabled(false);
	}
	
	private boolean placeStackInHotbar(ItemStack stack)
	{
		for(int i = 0; i < 9; i++)
		{
			if(!MC.player.inventory.getInvStack(i).isEmpty())
				continue;
			
			MC.player.networkHandler.sendPacket(
				new CreativeInventoryActionC2SPacket(36 + i, stack));
			return true;
		}
		
		return false;
	}
}
