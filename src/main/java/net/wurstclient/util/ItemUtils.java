/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.OptionalDouble;

import net.minecraft.IdentifierException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.wurstclient.WurstClient;

public enum ItemUtils
{
	;
	
	private static final Minecraft MC = WurstClient.MC;
	
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
			// There is no getOptionalValue() for raw IDs, so this detects when
			// the registry defaults and returns null instead
			int id = Integer.parseInt(nameOrId);
			Item item = BuiltInRegistries.ITEM.byId(id);
			if(id != 0 && BuiltInRegistries.ITEM.getId(item) == 0)
				return null;
			
			return item;
		}
		
		try
		{
			// getOptionalValue() returns null instead of Items.AIR if the
			// requested item doesn't exist
			return BuiltInRegistries.ITEM
				.getOptional(Identifier.parse(nameOrId)).orElse(null);
			
		}catch(IdentifierException e)
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
	
	public static double calculateModifiedAttribute(Item item,
		Holder<Attribute> attribute, double base, EquipmentSlot slot)
	{
		ItemAttributeModifiers modifiers = item.components().getOrDefault(
			DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		
		double result = base;
		for(ItemAttributeModifiers.Entry entry : modifiers.modifiers())
		{
			if(entry.attribute() != attribute || !entry.slot().test(slot))
				continue;
			
			double value = entry.modifier().amount();
			result += switch(entry.modifier().operation())
			{
				case ADD_VALUE -> value;
				case ADD_MULTIPLIED_BASE -> value * base;
				case ADD_MULTIPLIED_TOTAL -> value * result;
			};
		}
		
		return result;
	}
	
	public static double getArmorAttribute(Item item,
		Holder<Attribute> attribute)
	{
		Equippable equippable =
			item.components().get(DataComponents.EQUIPPABLE);
		
		double base = MC.player.getAttributeBaseValue(attribute);
		if(equippable == null)
			return base;
		
		return calculateModifiedAttribute(item, attribute, base,
			equippable.slot());
	}
	
	public static double getArmorPoints(Item item)
	{
		return getArmorAttribute(item, Attributes.ARMOR);
	}
	
	public static double getToughness(Item item)
	{
		return getArmorAttribute(item, Attributes.ARMOR_TOUGHNESS);
	}
	
	public static EquipmentSlot getArmorSlot(Item item)
	{
		Equippable equippable =
			item.components().get(DataComponents.EQUIPPABLE);
		
		return equippable != null ? equippable.slot() : null;
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
