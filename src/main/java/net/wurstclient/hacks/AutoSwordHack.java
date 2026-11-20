/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.OptionalDouble;
import java.util.function.Predicate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.EntityHitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.ItemUtils;

@SearchTags({"auto sword"})
public final class AutoSwordHack extends Hack implements UpdateListener
{
	private final EnumSetting<WeaponType> preferredType = new EnumSetting<>(
		"Preferred type", "description.wurst.setting.autosword.preferred_type",
		WeaponType.values(), WeaponType.ANY);
	
	private final SliderSetting maxDpsLoss = new SliderSetting("Max DPS loss",
		"description.wurst.setting.autosword.max_dps_loss", 0.2, 0, 1, 0.01,
		ValueDisplay.PERCENTAGE);
	
	private final CheckboxSetting switchBack = new CheckboxSetting(
		"Switch back", "description.wurst.setting.autosword.switch_back", true);
	
	private final SliderSetting releaseTime = new SliderSetting("Release time",
		"description.wurst.setting.autosword.release_time", 10, 1, 200, 1,
		ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(1, "1 tick"));
	
	private int oldSlot;
	private int timer;
	
	public AutoSwordHack()
	{
		super("AutoSword");
		setCategory(Category.COMBAT);
		
		addSetting(preferredType);
		addSetting(maxDpsLoss);
		addSetting(switchBack);
		addSetting(releaseTime);
	}
	
	@Override
	protected void onEnable()
	{
		oldSlot = -1;
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		resetSlot();
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.hitResult instanceof EntityHitResult entityHitResult)
		{
			Entity entity = entityHitResult.getEntity();
			
			if(entity instanceof LivingEntity
				&& EntityUtils.IS_ATTACKABLE_MELEE.test(entity))
				setSlot(entity);
		}
		
		// update timer
		if(timer > 0)
		{
			timer--;
			return;
		}
		
		resetSlot();
	}
	
	public void setSlot(Entity entity)
	{
		if(!isEnabled())
			return;
		
		if(WURST.getHax().autoEatHack.isEating())
			return;
		
		Inventory inventory = MC.player.getInventory();
		int bestSlot = getBestSlot(entity);
		if(bestSlot == -1)
			return;
		
		if(oldSlot == -1)
			oldSlot = inventory.getSelectedSlot();
		
		inventory.setSelectedSlot(bestSlot);
		timer = releaseTime.getValueI();
	}
	
	private int getBestSlot(Entity entity)
	{
		Inventory inventory = MC.player.getInventory();
		WeaponType type = preferredType.getSelected();
		int bestSlot = -1;
		double bestDps = -1;
		int preferredSlot = -1;
		double preferredDps = -1;
		
		// Start with the held slot so ties don't cause an unnecessary switch
		int selected = inventory.getSelectedSlot();
		for(int i = 0; i < Inventory.SELECTION_SIZE; i++)
		{
			int slot = (selected + i) % Inventory.SELECTION_SIZE;
			ItemStack stack = inventory.getItem(slot);
			double dps = getDps(stack, entity);
			if(dps > bestDps)
			{
				bestSlot = slot;
				bestDps = dps;
			}
			if(dps > preferredDps && type.prefers.test(stack))
			{
				preferredSlot = slot;
				preferredDps = dps;
			}
		}
		
		if(preferredSlot != -1
			&& preferredDps >= bestDps * (1 - maxDpsLoss.getValue()))
			return preferredSlot;
		
		return bestSlot;
	}
	
	private double getDps(ItemStack stack, Entity entity)
	{
		if(stack.isEmpty() || !stack.has(DataComponents.TOOL)
			&& !stack.has(DataComponents.WEAPON))
			return -1;
		
		OptionalDouble attackDamage = ItemUtils.calculateModifiedAttribute(
			stack, Attributes.ATTACK_DAMAGE, EquipmentSlot.MAINHAND);
		OptionalDouble attackSpeed = ItemUtils.calculateModifiedAttribute(stack,
			Attributes.ATTACK_SPEED, EquipmentSlot.MAINHAND);
		if(attackDamage.isEmpty() && attackSpeed.isEmpty())
			return -1;
		
		double damage = attackDamage.orElseGet(
			() -> MC.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
		double speed = attackSpeed.orElseGet(
			() -> MC.player.getAttributeBaseValue(Attributes.ATTACK_SPEED));
		if(stack.getItem() instanceof MaceItem mace)
			damage += mace.getAttackDamageBonus(entity, (float)damage,
				entity.damageSources().playerAttack(MC.player));
		
		return damage * speed;
	}
	
	private void resetSlot()
	{
		if(!switchBack.isChecked())
		{
			oldSlot = -1;
			return;
		}
		
		if(oldSlot != -1)
		{
			MC.player.getInventory().setSelectedSlot(oldSlot);
			oldSlot = -1;
		}
	}
	
	private enum WeaponType
	{
		ANY("Any", stack -> false),
		SWORD("Sword", stack -> stack.is(ItemTags.SWORDS)),
		AXE("Axe", stack -> stack.is(ItemTags.AXES)),
		SPEAR("Spear", stack -> stack.is(ItemTags.SPEARS)),
		TRIDENT("Trident", stack -> stack.getItem() instanceof TridentItem),
		MACE("Mace", stack -> stack.getItem() instanceof MaceItem),
		PICKAXE("Pickaxe", stack -> stack.is(ItemTags.PICKAXES)),
		SHOVEL("Shovel", stack -> stack.is(ItemTags.SHOVELS)),
		HOE("Hoe", stack -> stack.is(ItemTags.HOES));
		
		private final String name;
		private final Predicate<ItemStack> prefers;
		
		private WeaponType(String name, Predicate<ItemStack> prefers)
		{
			this.name = name;
			this.prefers = prefers;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
