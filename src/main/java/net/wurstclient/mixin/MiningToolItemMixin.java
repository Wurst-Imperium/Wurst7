/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.google.common.collect.Multimap;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterial;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMiningToolItem;

@Mixin(MiningToolItem.class)
public class MiningToolItemMixin extends ToolItem implements IMiningToolItem
{
	@Shadow
	@Final
	protected float attackDamage;
	
	@Shadow
	@Final
	private Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers;
	
	private MiningToolItemMixin(WurstClient wurst, ToolMaterial material,
		Settings settings)
	{
		super(material, settings);
	}
	
	@Override
	public float fuckMcAfee1()
	{
		return attackDamage;
	}
	
	@Override
	public float fuckMcAfee2()
	{
		return (float)attributeModifiers
			.get(EntityAttributes.GENERIC_ATTACK_SPEED).iterator().next()
			.getValue();
	}
}
