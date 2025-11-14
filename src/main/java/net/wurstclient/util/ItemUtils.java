/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.OptionalDouble;

import net.minecraft.ResourceLocationException;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public enum ItemUtils
{
	;
	
	/**
	 * @param nameOrId
	 *            a String containing the item's name ({@link ResourceLocation})
	 *            or
	 *            numeric ID.
	 * @return the requested item, or null if the item doesn't exist.
	 */
	public static Item getItemFromNameOrID(String nameOrId)
	{
		if(MathUtils.isInteger(nameOrId))
		{
			// There is no getOrEmpty() for raw IDs, so this detects when the
			// Registry defaults and returns null instead
			int id = Integer.parseInt(nameOrId);
			Item item = BuiltInRegistries.ITEM.byId(id);
			if(id != 0 && BuiltInRegistries.ITEM.getId(item) == 0)
				return null;
			
			return item;
		}
		
		try
		{
			// getOrEmpty() returns null instead of Items.AIR if the
			// requested item doesn't exist
			return BuiltInRegistries.ITEM
				.getOptional(ResourceLocation.parse(nameOrId)).orElse(null);
			
		}catch(ResourceLocationException e)
		{
			return null;
		}
	}
	
	// TODO: Update AutoSword to use calculateModifiedAttribute() instead,
	// then remove this method.
	public static OptionalDouble getAttribute(Item item,
		Holder<Attribute> attribute)
	{
		return item.components()
			.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
				ItemAttributeModifiers.EMPTY)
			.modifiers().stream()
			.filter(modifier -> modifier.attribute() == attribute)
			.mapToDouble(modifier -> modifier.modifier().amount()).findFirst();
	}
	
	public static boolean hasEffect(ItemStack stack, Holder<MobEffect> effect)
	{
		PotionContents potionContents = stack.getComponents()
			.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		
		for(MobEffectInstance effectInstance : potionContents.getAllEffects())
			if(effectInstance.getEffect() == effect)
				return true;
			
		return false;
	}
}
