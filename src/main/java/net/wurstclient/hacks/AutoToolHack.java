/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.BlockBreakingProgressListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"auto tool", "AutoSwitch", "auto switch"})
public final class AutoToolHack extends Hack
	implements BlockBreakingProgressListener, UpdateListener
{
	private final CheckboxSetting useSwords = new CheckboxSetting("Use swords",
		"Uses swords to break leaves, cobwebs, etc.", false);
	
	private final CheckboxSetting useHands = new CheckboxSetting("Use hands",
		"Uses an empty hand or a non-damageable item when no applicable tool is"
			+ " found.",
		true);
	
	private final SliderSetting repairMode = new SliderSetting("Repair mode",
		"Prevents tools from being used when their durability reaches the given"
			+ " threshold, so you can repair them before they break.\n"
			+ "Can be adjusted from 0 (off) to 100 remaining uses.",
		0, 0, 100, 1, ValueDisplay.INTEGER.withLabel(0, "off"));
	
	private final CheckboxSetting switchBack = new CheckboxSetting(
		"Switch back", "After using a tool, automatically switches back to the"
			+ " previously selected slot.",
		false);
	
	private int prevSelectedSlot;
	
	public AutoToolHack()
	{
		super("AutoTool");
		
		setCategory(Category.BLOCKS);
		addSetting(useSwords);
		addSetting(useHands);
		addSetting(repairMode);
		addSetting(switchBack);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(BlockBreakingProgressListener.class, this);
		EVENTS.add(UpdateListener.class, this);
		prevSelectedSlot = -1;
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(BlockBreakingProgressListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onBlockBreakingProgress(BlockBreakingProgressEvent event)
	{
		BlockPos pos = event.getBlockPos();
		if(!BlockUtils.canBeClicked(pos))
			return;
		
		if(prevSelectedSlot == -1)
			prevSelectedSlot = MC.player.getInventory().getSelectedSlot();
		
		equipBestTool(pos, useSwords.isChecked(), useHands.isChecked(),
			repairMode.getValueI());
	}
	
	@Override
	public void onUpdate()
	{
		if(prevSelectedSlot == -1 || MC.gameMode.isDestroying())
			return;
		
		HitResult hitResult = MC.hitResult;
		if(hitResult != null && hitResult.getType() == HitResult.Type.BLOCK)
			return;
		
		if(switchBack.isChecked())
			MC.player.getInventory().setSelectedSlot(prevSelectedSlot);
		
		prevSelectedSlot = -1;
	}
	
	public void equipIfEnabled(BlockPos pos)
	{
		if(!isEnabled())
			return;
		
		equipBestTool(pos, useSwords.isChecked(), useHands.isChecked(),
			repairMode.getValueI());
	}
	
	public void equipBestTool(BlockPos pos, boolean useSwords, boolean useHands,
		int repairMode)
	{
		LocalPlayer player = MC.player;
		if(player.getAbilities().instabuild)
			return;
		
		ItemStack heldItem = player.getMainHandItem();
		boolean heldItemDamageable = isDamageable(heldItem);
		if(heldItemDamageable && isTooDamaged(heldItem, repairMode))
			putAwayDamagedTool(repairMode);
		
		BlockState state = BlockUtils.getState(pos);
		int bestSlot = getBestSlot(state, useSwords, repairMode);
		if(bestSlot == -1)
		{
			if(useHands && heldItemDamageable && isWrongTool(heldItem, state))
				selectFallbackSlot();
			
			return;
		}
		
		player.getInventory().setSelectedSlot(bestSlot);
	}
	
	private int getBestSlot(BlockState state, boolean useSwords, int repairMode)
	{
		LocalPlayer player = MC.player;
		Inventory inventory = player.getInventory();
		ItemStack heldItem = MC.player.getMainHandItem();
		
		float bestSpeed = getMiningSpeed(heldItem, state);
		if(isTooDamaged(heldItem, repairMode))
			bestSpeed = 1;
		int bestSlot = -1;
		
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == inventory.getSelectedSlot())
				continue;
			
			ItemStack stack = inventory.getItem(slot);
			
			float speed = getMiningSpeed(stack, state);
			if(speed <= bestSpeed)
				continue;
			
			if(!useSwords && stack.is(ItemTags.SWORDS))
				continue;
			
			if(isTooDamaged(stack, repairMode))
				continue;
			
			bestSpeed = speed;
			bestSlot = slot;
		}
		
		return bestSlot;
	}
	
	private float getMiningSpeed(ItemStack stack, BlockState state)
	{
		float speed = stack.getDestroySpeed(state);
		
		if(speed > 1)
		{
			RegistryAccess drm = WurstClient.MC.level.registryAccess();
			Registry<Enchantment> registry =
				drm.lookupOrThrow(Registries.ENCHANTMENT);
			
			Optional<Reference<Enchantment>> efficiency =
				registry.get(Enchantments.EFFICIENCY);
			int effLvl = efficiency.map(entry -> EnchantmentHelper
				.getItemEnchantmentLevel(entry, stack)).orElse(0);
			
			if(effLvl > 0 && !stack.isEmpty())
				speed += effLvl * effLvl + 1;
		}
		
		return speed;
	}
	
	private boolean isDamageable(ItemStack stack)
	{
		return !stack.isEmpty() && stack.isDamageableItem();
	}
	
	private boolean isTooDamaged(ItemStack stack, int repairMode)
	{
		return stack.getMaxDamage() - stack.getDamageValue() <= repairMode;
	}
	
	private void putAwayDamagedTool(int repairMode)
	{
		Inventory inv = MC.player.getInventory();
		int selectedSlot = inv.getSelectedSlot();
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		
		// If there's an empty slot in the main inventory,
		// shift-click the damaged item out of the hotbar
		OptionalInt emptySlot = IntStream.range(9, 36)
			.filter(i -> !inv.getItem(i).isEmpty()).findFirst();
		if(emptySlot.isPresent())
		{
			im.windowClick_QUICK_MOVE(
				InventoryUtils.toNetworkSlot(selectedSlot));
			return;
		}
		
		// Failing that, swap with a non-damageable item
		OptionalInt nonDamageableSlot = IntStream.range(9, 36)
			.filter(i -> !isDamageable(inv.getItem(i))).findFirst();
		if(nonDamageableSlot.isPresent())
		{
			im.windowClick_SWAP(nonDamageableSlot.getAsInt(), selectedSlot);
			return;
		}
		
		// Failing that, swap with a less damaged item
		OptionalInt notTooDamagedSlot = IntStream.range(9, 36)
			.filter(i -> !isTooDamaged(inv.getItem(i), repairMode)).findFirst();
		if(notTooDamagedSlot.isPresent())
		{
			im.windowClick_SWAP(notTooDamagedSlot.getAsInt(), selectedSlot);
			return;
		}
		
		// Failing all of the above (whole inventory full of damaged tools),
		// just swap with the top-left slot
		im.windowClick_SWAP(0, selectedSlot);
	}
	
	private boolean isWrongTool(ItemStack heldItem, BlockState state)
	{
		return getMiningSpeed(heldItem, state) <= 1;
	}
	
	private void selectFallbackSlot()
	{
		int fallbackSlot = getFallbackSlot();
		Inventory inventory = MC.player.getInventory();
		
		if(fallbackSlot == -1)
		{
			int prevSlot = inventory.getSelectedSlot();
			if(prevSlot == 8)
				inventory.setSelectedSlot(0);
			else
				inventory.setSelectedSlot(prevSlot + 1);
			
			return;
		}
		
		inventory.setSelectedSlot(fallbackSlot);
	}
	
	private int getFallbackSlot()
	{
		Inventory inventory = MC.player.getInventory();
		
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == inventory.getSelectedSlot())
				continue;
			
			ItemStack stack = inventory.getItem(slot);
			
			if(!isDamageable(stack))
				return slot;
		}
		
		return -1;
	}
}
