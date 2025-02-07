/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
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
		if(MC.currentScreen instanceof HandledScreen
			&& !(MC.currentScreen instanceof InventoryScreen))
			return;
		
		ClientPlayerEntity player = MC.player;
		PlayerInventory inventory = player.getInventory();
		
		if(!swapWhileMoving.isChecked()
			&& player.input.getMovementInput().length() > 1e-5F)
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
			
			ItemStack stack = player.getEquippedStack(type);
			if(!MC.player.canEquip(stack, type))
				continue;
			
			bestArmor.put(type, new ArmorData(-1, getArmorValue(stack)));
		}
		
		// search inventory for better armor
		for(int slot = 0; slot < 36; slot++)
		{
			ItemStack stack = inventory.getStack(slot);
			
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
			ItemStack oldArmor = player.getEquippedStack(type);
			if(!oldArmor.isEmpty() && inventory.getEmptySlot() == -1)
				continue;
			
			// swap armor
			if(!oldArmor.isEmpty())
				IMC.getInteractionManager()
					.windowClick_QUICK_MOVE(8 - type.getEntitySlotId());
			IMC.getInteractionManager().windowClick_QUICK_MOVE(
				InventoryUtils.toNetworkSlot(data.invSlot()));
			
			break;
		}
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof ClickSlotC2SPacket)
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
			DynamicRegistryManager drm =
				WurstClient.MC.world.getRegistryManager();
			Registry<Enchantment> registry =
				drm.getOrThrow(RegistryKeys.ENCHANTMENT);
			
			Optional<Reference<Enchantment>> protection =
				registry.getOptional(Enchantments.PROTECTION);
			prtPoints = protection
				.map(entry -> EnchantmentHelper.getLevel(entry, stack))
				.orElse(0);
		}
		
		return armorPoints * 5 + prtPoints * 3 + armorToughness;
	}
	
	private record ArmorData(int invSlot, int armorValue)
	{}
}
