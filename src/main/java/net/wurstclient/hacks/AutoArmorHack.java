/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.TextFormat;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.packet.ClickWindowC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IArmorItem;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"auto armor"})
public final class AutoArmorHack extends Hack
	implements UpdateListener, PacketOutputListener
{
	private final CheckboxSetting useEnchantments = new CheckboxSetting(
		"Use enchantments", "Whether or not to consider the Protection\n"
			+ "enchantment when calculating armor strength.",
		true);
	
	private final CheckboxSetting swapWhileMoving =
		new CheckboxSetting("Swap while moving",
			"Whether or not to swap armor pieces\n"
				+ "while the player is moving.\n\n" + TextFormat.RED
				+ TextFormat.BOLD + "WARNING:" + TextFormat.RESET
				+ " This would not be possible\n"
				+ "without cheats. It may raise suspicion.",
			false);
	
	private final SliderSetting delay =
		new SliderSetting("Delay",
			"Amount of ticks to wait before swapping\n"
				+ "the next piece of armor.",
			2, 0, 20, 1, ValueDisplay.INTEGER);
	
	private int timer;
	
	public AutoArmorHack()
	{
		super("AutoArmor", "Manages your armor automatically.");
		setCategory(Category.COMBAT);
		addSetting(useEnchantments);
		addSetting(swapWhileMoving);
		addSetting(delay);
	}
	
	@Override
	public void onEnable()
	{
		timer = 0;
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// wait for timer
		if(timer > 0)
		{
			timer--;
			return;
		}
		
		// check screen
		if(MC.currentScreen instanceof AbstractContainerScreen
			&& !(MC.currentScreen instanceof InventoryScreen))
			return;
		
		ClientPlayerEntity player = MC.player;
		PlayerInventory inventory = player.inventory;
		
		if(!swapWhileMoving.isChecked() && (player.input.movementForward != 0
			|| player.input.movementSideways != 0))
			return;
		
		// store slots and values of best armor pieces
		int[] bestArmorSlots = new int[4];
		int[] bestArmorValues = new int[4];
		
		// initialize with currently equipped armor
		for(int type = 0; type < 4; type++)
		{
			bestArmorSlots[type] = -1;
			
			ItemStack stack = inventory.getArmorStack(type);
			if(stack.isEmpty() || !(stack.getItem() instanceof ArmorItem))
				continue;
			
			ArmorItem item = (ArmorItem)stack.getItem();
			bestArmorValues[type] = getArmorValue(item, stack);
		}
		
		// search inventory for better armor
		for(int slot = 0; slot < 36; slot++)
		{
			ItemStack stack = inventory.getInvStack(slot);
			
			if(stack.isEmpty() || !(stack.getItem() instanceof ArmorItem))
				continue;
			
			ArmorItem item = (ArmorItem)stack.getItem();
			int armorType = item.getSlotType().getEntitySlotId();
			int armorValue = getArmorValue(item, stack);
			
			if(armorValue > bestArmorValues[armorType])
			{
				bestArmorSlots[armorType] = slot;
				bestArmorValues[armorType] = armorValue;
			}
		}
		
		// equip better armor in random order
		ArrayList<Integer> types = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
		Collections.shuffle(types);
		for(int type : types)
		{
			// check if better armor was found
			int slot = bestArmorSlots[type];
			if(slot == -1)
				continue;
				
			// check if armor can be swapped
			// needs 1 free slot where it can put the old armor
			ItemStack oldArmor = inventory.getArmorStack(type);
			if(!oldArmor.isEmpty() && inventory.getEmptySlot() == -1)
				continue;
			
			// hotbar fix
			if(slot < 9)
				slot += 36;
			
			// swap armor
			if(!oldArmor.isEmpty())
				IMC.getInteractionManager().windowClick_QUICK_MOVE(8 - type);
			IMC.getInteractionManager().windowClick_QUICK_MOVE(slot);
			
			break;
		}
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof ClickWindowC2SPacket)
			timer = delay.getValueI();
	}
	
	private int getArmorValue(ArmorItem item, ItemStack stack)
	{
		int armorPoints = item.getProtection();
		int prtPoints = 0;
		int armorToughness = (int)((IArmorItem)item).getToughness();
		int armorType =
			item.getMaterial().getProtectionAmount(EquipmentSlot.LEGS);
		
		if(useEnchantments.isChecked())
		{
			Enchantment protection = Enchantments.PROTECTION;
			int prtLvl = EnchantmentHelper.getLevel(protection, stack);
			
			ClientPlayerEntity player = MC.player;
			DamageSource dmgSource = DamageSource.player(player);
			prtPoints = protection.getProtectionAmount(prtLvl, dmgSource);
		}
		
		return armorPoints * 5 + prtPoints * 3 + armorToughness + armorType;
	}
}
