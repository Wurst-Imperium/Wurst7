/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.List;
import java.util.Optional;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"kill potion", "KillerPotion", "killer potion", "KillingPotion",
	"killing potion", "InstantDeathPotion", "instant death potion"})
public final class KillPotionHack extends Hack
{
	private final EnumSetting<PotionType> potionType =
		new EnumSetting<>("Potion type", "The type of potion to generate.",
			PotionType.values(), PotionType.SPLASH);
	
	public KillPotionHack()
	{
		super("KillPotion");
		
		setCategory(Category.ITEMS);
		addSetting(potionType);
	}
	
	@Override
	protected void onEnable()
	{
		// check gamemode
		if(!MC.player.getAbilities().creativeMode)
		{
			ChatUtils.error("Creative mode only.");
			setEnabled(false);
			return;
		}
		
		// generate potion
		ItemStack stack = potionType.getSelected().createPotionStack();
		
		// give potion
		PlayerInventory inventory = MC.player.getInventory();
		int slot = inventory.getEmptySlot();
		if(slot < 0)
			ChatUtils.error("Cannot give potion. Your inventory is full.");
		else
		{
			InventoryUtils.setCreativeStack(slot, stack);
			ChatUtils.message("Potion created.");
		}
		
		setEnabled(false);
	}
	
	private enum PotionType
	{
		NORMAL("Normal", "Potion", Items.POTION),
		
		SPLASH("Splash", "Splash Potion", Items.SPLASH_POTION),
		
		LINGERING("Lingering", "Lingering Potion", Items.LINGERING_POTION);
		
		// does not work
		// ARROW("Arrow", "Arrow", Items.TIPPED_ARROW);
		
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
			
			StatusEffectInstance effect = new StatusEffectInstance(
				StatusEffects.INSTANT_HEALTH, 2000, 125);
			
			PotionContentsComponent potionContents =
				new PotionContentsComponent(Optional.empty(), Optional.empty(),
					List.of(effect), Optional.empty());
			
			stack.set(DataComponentTypes.POTION_CONTENTS, potionContents);
			
			String name =
				"\u00a7f" + itemName + " of \u00a74\u00a7lINSTANT DEATH";
			stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
			
			return stack;
		}
	}
}
