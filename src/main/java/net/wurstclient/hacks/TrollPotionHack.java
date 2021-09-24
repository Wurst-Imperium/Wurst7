/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.LiteralText;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.ChatUtils;

@SearchTags({"troll potion", "TrollingPotion", "trolling potion"})
public final class TrollPotionHack extends Hack
{
	private final EnumSetting<PotionType> potionType =
		new EnumSetting<>("药水类型", "要生成的药水类型.",
			PotionType.values(), PotionType.SPLASH);
	
	public TrollPotionHack()
	{
		super("巨魔药水","生成具有许多恼人效果的药水");
		
		setCategory(Category.ITEMS);
		addSetting(potionType);
	}
	
	@Override
	public void onEnable()
	{
		// check gamemode
		if(!MC.player.getAbilities().creativeMode)
		{
			ChatUtils.error("仅限创造模式");
			setEnabled(false);
			return;
		}
		
		// generate potion
		ItemStack stack = potionType.getSelected().createPotionStack();
		
		// give potion
		if(placeStackInHotbar(stack))
			ChatUtils.message("Potion created.");
		else
			ChatUtils.error("请清除您的快捷栏中的一个插槽");
		
		setEnabled(false);
	}
	
	private boolean placeStackInHotbar(ItemStack stack)
	{
		for(int i = 0; i < 9; i++)
		{
			if(!MC.player.getInventory().getStack(i).isEmpty())
				continue;
			
			MC.player.networkHandler.sendPacket(
				new CreativeInventoryActionC2SPacket(36 + i, stack));
			return true;
		}
		
		return false;
	}
	
	private enum PotionType
	{
		NORMAL("正常", "药水", Items.POTION),
		
		SPLASH("飞溅", "喷溅药水", Items.SPLASH_POTION),
		
		LINGERING("挥之不去", "滞留药水", Items.LINGERING_POTION),
		
		ARROW("箭头", "箭头", Items.TIPPED_ARROW);
		
		private final String name;
		private final String itemName;
		private final Item item;
		
		private PotionType(String name, String itemName, Item item)
		{
			this.name = name;
			this.itemName = itemName;
			this.item = item;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		public ItemStack createPotionStack()
		{
			ItemStack stack = new ItemStack(item);
			
			NbtList effects = new NbtList();
			for(int i = 1; i <= 23; i++)
			{
				NbtCompound effect = new NbtCompound();
				effect.putInt("Amplifier", Integer.MAX_VALUE);
				effect.putInt("Duration", Integer.MAX_VALUE);
				effect.putInt("Id", i);
				effects.add(effect);
			}
			
			NbtCompound nbt = new NbtCompound();
			nbt.put("自定义药水效果", effects);
			stack.setNbt(nbt);
			
			String name = "\u00a7f" + itemName + " of Trolling";
			stack.setCustomName(new LiteralText(name));
			
			return stack;
		}
	}
}
