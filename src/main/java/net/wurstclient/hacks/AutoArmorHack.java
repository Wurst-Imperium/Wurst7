/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Optional;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.util.ItemUtils;

@SearchTags({"auto armor"})
public final class AutoArmorHack extends Hack
	implements UpdateListener, PacketOutputListener
{
	private final CheckboxSetting useEnchantments = new CheckboxSetting(
		"Use enchantments",
		"Whether or not to consider the Protection enchantment when calculating armor strength.",
		true);
	
	private final CheckboxSetting swapWhileMoving = new CheckboxSetting(
		"Swap while moving",
		"Whether or not to swap armor pieces while the player is moving.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r This would not be possible without cheats. It may raise suspicion.",
		false);
	
	private final SliderSetting delay = new SliderSetting("Delay",
		"Amount of ticks to wait before swapping the next piece of armor.", 2,
		0, 20, 1, ValueDisplay.INTEGER);
	
	private int timer;
	
	public AutoArmorHack()
	{
		super("AutoArmor");
		setCategory(Category.COMBAT);
		addSetting(useEnchantments);
		addSetting(swapWhileMoving);
		addSetting(delay);
	}
	
	@Override
	protected void onEnable()
	{
		timer = 0;
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
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
		if(MC.screen instanceof AbstractContainerScreen
			&& !(MC.screen instanceof InventoryScreen))
			return;
		
		LocalPlayer player = MC.player;
		Inventory inventory = player.getInventory();
		
		if(!swapWhileMoving.isChecked()
			&& player.input.getMoveVector().length() > 1e-5F)
			return;
		
		// store slots and values of best armor pieces
		EnumMap<EquipmentSlot, ArmorData> bestArmor =
			new EnumMap<>(EquipmentSlot.class);
		ArrayList<EquipmentSlot> armorTypes =
			new ArrayList<>(Arrays.asList(EquipmentSlot.FEET,
				EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD));
		
		// initialize with currently equipped armor
		for(EquipmentSlot type : armorTypes)
		{
			bestArmor.put(type, new ArmorData(-1, 0));
			
			ItemStack stack = player.getItemBySlot(type);
			if(!MC.player.isEquippableInSlot(stack, type))
				continue;
			
			bestArmor.put(type, new ArmorData(-1, getArmorValue(stack)));
		}
		
		// search inventory for better armor
		for(int slot = 0; slot < 36; slot++)
		{
			ItemStack stack = inventory.getItem(slot);
			
			EquipmentSlot armorType = ItemUtils.getArmorSlot(stack.getItem());
			if(armorType == null)
				continue;
			
			int armorValue = getArmorValue(stack);
			ArmorData data = bestArmor.get(armorType);
			
			if(data == null || armorValue > data.armorValue())
				bestArmor.put(armorType, new ArmorData(slot, armorValue));
		}
		
		// equip better armor in random order
		Collections.shuffle(armorTypes);
		for(EquipmentSlot type : armorTypes)
		{
			// check if better armor was found
			ArmorData data = bestArmor.get(type);
			if(data == null || data.invSlot() == -1)
				continue;
				
			// check if armor can be swapped
			// needs 1 free slot where it can put the old armor
			ItemStack oldArmor = player.getItemBySlot(type);
			if(!oldArmor.isEmpty() && inventory.getFreeSlot() == -1)
				continue;
			
			// swap armor
			if(!oldArmor.isEmpty())
				IMC.getInteractionManager()
					.windowClick_QUICK_MOVE(8 - type.getIndex());
			IMC.getInteractionManager().windowClick_QUICK_MOVE(
				InventoryUtils.toNetworkSlot(data.invSlot()));
			
			break;
		}
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof ServerboundContainerClickPacket)
			timer = delay.getValueI();
	}
	
	private int getArmorValue(ItemStack stack)
	{
		Item item = stack.getItem();
		int armorPoints = (int)ItemUtils.getArmorPoints(item);
		int prtPoints = 0;
		int armorToughness = (int)ItemUtils.getToughness(item);
		
		if(useEnchantments.isChecked())
		{
			RegistryAccess drm = WurstClient.MC.level.registryAccess();
			Registry<Enchantment> registry =
				drm.lookupOrThrow(Registries.ENCHANTMENT);
			
			Optional<Reference<Enchantment>> protection =
				registry.get(Enchantments.PROTECTION);
			prtPoints = protection.map(entry -> EnchantmentHelper
				.getItemEnchantmentLevel(entry, stack)).orElse(0);
		}
		
		return armorPoints * 5 + prtPoints * 3 + armorToughness;
	}
	
	private record ArmorData(int invSlot, int armorValue)
	{}
}
