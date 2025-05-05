/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.OptionalDouble;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

public enum ItemUtils
{
	;
	
	/**
	 * @param nameOrId
	 *            a String containing the item's name ({@link Identifier}) or
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
			Item item = Registries.ITEM.get(id);
			if(id != 0 && Registries.ITEM.getRawId(item) == 0)
				return null;
			
			return item;
		}
		
		try
		{
			// getOrEmpty() returns null instead of Items.AIR if the
			// requested item doesn't exist
			return Registries.ITEM.getOrEmpty(Identifier.of(nameOrId))
				.orElse(null);
			
		}catch(InvalidIdentifierException e)
		{
			return null;
		}
	}
	
	// TODO: Update AutoSword to use calculateModifiedAttribute() instead,
	// then remove this method.
	public static OptionalDouble getAttribute(Item item,
		RegistryEntry<EntityAttribute> attribute)
	{
		return item.getComponents()
			.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS,
				AttributeModifiersComponent.DEFAULT)
			.modifiers().stream()
			.filter(modifier -> modifier.attribute() == attribute)
			.mapToDouble(modifier -> modifier.modifier().value()).findFirst();
	}
	
	public static boolean hasEffect(ItemStack stack,
		RegistryEntry<StatusEffect> effect)
	{
		PotionContentsComponent potionContents = stack.getComponents()
			.getOrDefault(DataComponentTypes.POTION_CONTENTS,
				PotionContentsComponent.DEFAULT);
		
		for(StatusEffectInstance effectInstance : potionContents.getEffects())
			if(effectInstance.getEffectType() == effect)
				return true;
			
		return false;
	}
}
