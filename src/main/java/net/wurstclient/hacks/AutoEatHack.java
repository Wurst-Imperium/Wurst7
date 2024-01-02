/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.FoodComponents;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"auto eat", "AutoFood", "auto food", "AutoFeeder", "auto feeder",
	"AutoFeeding", "auto feeding", "AutoSoup", "auto soup"})
public final class AutoEatHack extends Hack implements UpdateListener
{
	private final SliderSetting targetHunger = new SliderSetting(
		"Target hunger", "description.wurst.setting.autoeat.target_hunger", 10,
		0, 10, 0.5, ValueDisplay.DECIMAL);
	
	private final SliderSetting minHunger = new SliderSetting("Min hunger",
		"description.wurst.setting.autoeat.min_hunger", 6.5, 0, 10, 0.5,
		ValueDisplay.DECIMAL);
	
	private final SliderSetting injuredHunger = new SliderSetting(
		"Injured hunger", "description.wurst.setting.autoeat.injured_hunger",
		10, 0, 10, 0.5, ValueDisplay.DECIMAL);
	
	private final SliderSetting injuryThreshold =
		new SliderSetting("Injury threshold",
			"description.wurst.setting.autoeat.injury_threshold", 1.5, 0.5, 10,
			0.5, ValueDisplay.DECIMAL);
	
	private final EnumSetting<TakeItemsFrom> takeItemsFrom = new EnumSetting<>(
		"Take items from", "description.wurst.setting.autoeat.take_items_from",
		TakeItemsFrom.values(), TakeItemsFrom.HOTBAR);
	
	private final CheckboxSetting allowOffhand =
		new CheckboxSetting("Allow offhand", true);
	
	private final CheckboxSetting eatWhileWalking =
		new CheckboxSetting("Eat while walking",
			"description.wurst.setting.autoeat.eat_while_walking", false);
	
	private final CheckboxSetting allowHunger =
		new CheckboxSetting("Allow hunger effect",
			"description.wurst.setting.autoeat.allow_hunger", true);
	
	private final CheckboxSetting allowPoison =
		new CheckboxSetting("Allow poison effect",
			"description.wurst.setting.autoeat.allow_poison", false);
	
	private final CheckboxSetting allowChorus =
		new CheckboxSetting("Allow chorus fruit",
			"description.wurst.setting.autoeat.allow_chorus", false);
	
	private int oldSlot = -1;
	
	public AutoEatHack()
	{
		super("AutoEat");
		setCategory(Category.ITEMS);
		
		addSetting(targetHunger);
		addSetting(minHunger);
		addSetting(injuredHunger);
		addSetting(injuryThreshold);
		
		addSetting(takeItemsFrom);
		addSetting(allowOffhand);
		
		addSetting(eatWhileWalking);
		addSetting(allowHunger);
		addSetting(allowPoison);
		addSetting(allowChorus);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		
		if(isEating())
			stopEating();
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		if(!shouldEat())
		{
			if(isEating())
				stopEating();
			
			return;
		}
		
		HungerManager hungerManager = player.getHungerManager();
		int foodLevel = hungerManager.getFoodLevel();
		int targetHungerI = (int)(targetHunger.getValue() * 2);
		int minHungerI = (int)(minHunger.getValue() * 2);
		int injuredHungerI = (int)(injuredHunger.getValue() * 2);
		
		if(isInjured(player) && foodLevel < injuredHungerI)
		{
			eat(-1);
			return;
		}
		
		if(foodLevel < minHungerI)
		{
			eat(-1);
			return;
		}
		
		if(foodLevel < targetHungerI)
		{
			int maxPoints = targetHungerI - foodLevel;
			eat(maxPoints);
		}
	}
	
	private void eat(int maxPoints)
	{
		PlayerInventory inventory = MC.player.getInventory();
		int foodSlot = findBestFoodSlot(maxPoints);
		
		if(foodSlot == -1)
		{
			if(isEating())
				stopEating();
			
			return;
		}
		
		// select food
		if(foodSlot < 9)
		{
			if(!isEating())
				oldSlot = inventory.selectedSlot;
			
			inventory.selectedSlot = foodSlot;
			
		}else if(foodSlot == 40)
		{
			if(!isEating())
				oldSlot = inventory.selectedSlot;
			
			// off-hand slot, no need to select anything
			
		}else
		{
			InventoryUtils.selectItem(foodSlot);
			return;
		}
		
		// eat food
		MC.options.useKey.setPressed(true);
		IMC.getInteractionManager().rightClickItem();
	}
	
	private int findBestFoodSlot(int maxPoints)
	{
		PlayerInventory inventory = MC.player.getInventory();
		FoodComponent bestFood = null;
		int bestSlot = -1;
		
		int maxInvSlot = takeItemsFrom.getSelected().maxInvSlot;
		
		ArrayList<Integer> slots = new ArrayList<>();
		if(maxInvSlot == 0)
			slots.add(inventory.selectedSlot);
		if(allowOffhand.isChecked())
			slots.add(40);
		Stream.iterate(0, i -> i < maxInvSlot, i -> i + 1)
			.forEach(i -> slots.add(i));
		
		Comparator<FoodComponent> comparator =
			Comparator.comparingDouble(FoodComponent::getSaturationModifier);
		
		for(int slot : slots)
		{
			Item item = inventory.getStack(slot).getItem();
			
			// filter out non-food items
			if(!item.isFood())
				continue;
			
			FoodComponent food = item.getFoodComponent();
			if(!isAllowedFood(food))
				continue;
			
			if(maxPoints >= 0 && food.getHunger() > maxPoints)
				continue;
			
			// compare to previously found food
			if(bestFood == null || comparator.compare(food, bestFood) > 0)
			{
				bestFood = food;
				bestSlot = slot;
			}
		}
		
		return bestSlot;
	}
	
	private boolean shouldEat()
	{
		if(MC.player.getAbilities().creativeMode)
			return false;
		
		if(!MC.player.canConsume(false))
			return false;
		
		if(!eatWhileWalking.isChecked()
			&& (MC.player.forwardSpeed != 0 || MC.player.sidewaysSpeed != 0))
			return false;
		
		if(isClickable(MC.crosshairTarget))
			return false;
		
		return true;
	}
	
	private void stopEating()
	{
		MC.options.useKey.setPressed(false);
		MC.player.getInventory().selectedSlot = oldSlot;
		oldSlot = -1;
	}
	
	private boolean isAllowedFood(FoodComponent food)
	{
		if(!allowChorus.isChecked() && food == FoodComponents.CHORUS_FRUIT)
			return false;
		
		for(Pair<StatusEffectInstance, Float> pair : food.getStatusEffects())
		{
			StatusEffect effect = pair.getFirst().getEffectType();
			
			if(!allowHunger.isChecked() && effect == StatusEffects.HUNGER)
				return false;
			
			if(!allowPoison.isChecked() && effect == StatusEffects.POISON)
				return false;
		}
		
		return true;
	}
	
	public boolean isEating()
	{
		return oldSlot != -1;
	}
	
	private boolean isClickable(HitResult hitResult)
	{
		if(hitResult == null)
			return false;
		
		if(hitResult instanceof EntityHitResult)
		{
			Entity entity = ((EntityHitResult)hitResult).getEntity();
			return entity instanceof VillagerEntity
				|| entity instanceof TameableEntity;
		}
		
		if(hitResult instanceof BlockHitResult)
		{
			BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
			if(pos == null)
				return false;
			
			Block block = MC.world.getBlockState(pos).getBlock();
			return block instanceof BlockWithEntity
				|| block instanceof CraftingTableBlock;
		}
		
		return false;
	}
	
	private boolean isInjured(ClientPlayerEntity player)
	{
		int injuryThresholdI = (int)(injuryThreshold.getValue() * 2);
		return player.getHealth() < player.getMaxHealth() - injuryThresholdI;
	}
	
	private enum TakeItemsFrom
	{
		HANDS("Hands", 0),
		
		HOTBAR("Hotbar", 9),
		
		INVENTORY("Inventory", 36);
		
		private final String name;
		private final int maxInvSlot;
		
		private TakeItemsFrom(String name, int maxInvSlot)
		{
			this.name = name;
			this.maxInvSlot = maxInvSlot;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
