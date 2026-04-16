/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PlayerAttacksEntityListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"attribute swap", "weapon swap", "auto breach swap", "mace swap",
	"auto swap", "auto shield break", "auto mace", "disable shield",
	"item swap", "hotbar swap", "item saver", "durability saver"})
public final class AttributeSwapHack extends Hack
	implements PlayerAttacksEntityListener, UpdateListener
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"Simple: swaps to a fixed hotbar slot on attack.\n"
			+ "Smart: checks target and picks the best item automatically.",
		Mode.values(), Mode.SIMPLE);
	
	private final SliderSetting targetSlot = new SliderSetting("Target slot",
		"Hotbar slot to swap to (Simple mode).", 1, 1, 9, 1,
		ValueDisplay.INTEGER);
	
	private final CheckboxSetting swapBack = new CheckboxSetting("Swap back",
		"Swap back to the original slot after delay.", true);
	
	private final SliderSetting swapBackDelay =
		new SliderSetting("Swap-back delay",
			"How many ticks to wait before swapping back.", 2, 0, 20, 1,
			ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(0, "instant"));
	
	private final CheckboxSetting breachSwapping = new CheckboxSetting(
		"Breach swapping",
		"Swaps to a mace with Breach to deal more damage (Smart mode).", true);
	
	private final CheckboxSetting shieldBreaker =
		new CheckboxSetting("Shield breaker",
			"Swaps to an axe when target is blocking (Smart mode).", true);
	
	private final CheckboxSetting itemSaver = new CheckboxSetting("Item saver",
		"Swaps to a non-damageable item to save main weapon durability (Smart mode).",
		true);
	
	private final CheckboxSetting onlyWithKillAura = new CheckboxSetting(
		"Only with Killaura", "Only activate when Killaura is enabled.", false);
	
	private int backTimer;
	private boolean awaitingBack;
	private int originalSlot;
	
	public AttributeSwapHack()
	{
		super("AttributeSwap");
		setCategory(Category.COMBAT);
		addSetting(mode);
		addSetting(targetSlot);
		addSetting(swapBack);
		addSetting(swapBackDelay);
		addSetting(shieldBreaker);
		addSetting(itemSaver);
		addSetting(breachSwapping);
		addSetting(onlyWithKillAura);
	}
	
	@Override
	protected void onEnable()
	{
		backTimer = 0;
		awaitingBack = false;
		originalSlot = -1;
		
		EVENTS.add(PlayerAttacksEntityListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PlayerAttacksEntityListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
		
		if(awaitingBack)
		{
			doSwapBack();
			awaitingBack = false;
		}
		backTimer = 0;
		originalSlot = -1;
	}
	
	@Override
	public void onPlayerAttacksEntity(Entity target)
	{
		if(onlyWithKillAura.isChecked()
			&& !WURST.getHax().killauraHack.isEnabled())
			return;
		
		performSwap(target);
	}
	
	@Override
	public void onUpdate()
	{
		if(!awaitingBack)
			return;
		if(backTimer-- > 0)
			return;
		
		doSwapBack();
		awaitingBack = false;
	}
	
	private void performSwap(Entity target)
	{
		if(awaitingBack)
			return;
		
		if(mode.getSelected() == Mode.SIMPLE)
		{
			doSwap(targetSlot.getValueI() - 1);
			return;
		}
		
		doSwap(getSmartSlot(target));
	}
	
	private void doSwap(int slotIndex)
	{
		if(awaitingBack)
			return;
		if(slotIndex < 0 || slotIndex > 8)
			return;
		
		int current = MC.player.getInventory().getSelectedSlot();
		if(slotIndex == current)
			return;
		
		originalSlot = current;
		MC.player.getInventory().setSelectedSlot(slotIndex);
		
		if(swapBack.isChecked())
		{
			awaitingBack = true;
			backTimer = swapBackDelay.getValueI();
		}
	}
	
	private void doSwapBack()
	{
		if(originalSlot >= 0 && originalSlot <= 8)
			MC.player.getInventory().setSelectedSlot(originalSlot);
		
		originalSlot = -1;
	}
	
	private int getSmartSlot(Entity target)
	{
		ItemStack current = MC.player.getMainHandItem();
		
		if(target instanceof LivingEntity living && shieldBreaker.isChecked()
			&& living.isBlocking())
		{
			if(current.getItem() instanceof AxeItem)
				return -1;
			
			int axeSlot =
				InventoryUtils.indexOf(s -> s.getItem() instanceof AxeItem, 9);
			if(axeSlot != -1)
				return axeSlot;
		}
		
		if(breachSwapping.isChecked() && target instanceof LivingEntity le
			&& le.getAttributes().getValue(Attributes.ARMOR) > 0)
		{
			int breachSlot =
				InventoryUtils.indexOf(s -> s.getItem() instanceof MaceItem
					&& getEnchantLevel(s, Enchantments.BREACH) > 0, 9);
			
			if(breachSlot != -1)
				return breachSlot;
		}
		
		int bestSlot = -1;
		int bestScore = getDurabilityScore(current);
		
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == MC.player.getInventory().getSelectedSlot())
				continue;
			
			ItemStack stack = MC.player.getInventory().getItem(slot);
			if(stack.isEmpty())
				continue;
			
			int score = getDurabilityScore(stack);
			if(score > bestScore)
			{
				bestScore = score;
				bestSlot = slot;
			}
		}
		
		return bestSlot;
	}
	
	private int getDurabilityScore(ItemStack stack)
	{
		if(!itemSaver.isChecked())
			return 0;
		if(!stack.isDamageableItem())
			return 4;
		
		return 0;
	}
	
	private int getEnchantLevel(ItemStack stack, ResourceKey<Enchantment> key)
	{
		if(MC.level == null)
			return 0;
		
		return MC.level.registryAccess().lookup(Registries.ENCHANTMENT)
			.flatMap(reg -> reg.get(key)).map(holder -> EnchantmentHelper
				.getItemEnchantmentLevel(holder, stack))
			.orElse(0);
	}
	
	private enum Mode
	{
		SIMPLE("Simple"),
		SMART("Smart");
		
		private final String name;
		
		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
