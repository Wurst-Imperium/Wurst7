/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;

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
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"auto eat", "AutoFood", "auto food", "AutoFeeder", "auto feeder",
	"AutoFeeding", "auto feeding", "AutoSoup", "auto soup"})
public final class AutoEatHack extends Hack implements UpdateListener
{
	private final SliderSetting targetHunger = new SliderSetting(
		"Target hunger",
		"Tries to keep the hunger bar at or above this level, but only if it doesn't waste any hunger points.",
		10, 0, 10, 0.5, ValueDisplay.DECIMAL);
	
	private final SliderSetting minHunger = new SliderSetting("Min hunger",
		"Always keeps the hunger bar at or above this level, even if it wastes some hunger points.\n\n"
			+ "A value of 10 always allows for fast healing, but wastes the most hunger points. Recommended if you have lots of food and/or lots of combat.\n\n"
			+ "A value of 6.5 cannot cause any waste with vanilla food items, but still gives a good amount of points for sprinting. Recommended if you have if you don't have much food.",
		6.5, 0, 10, 0.5, ValueDisplay.DECIMAL);
	
	private final SliderSetting injuredHunger = new SliderSetting(
		"Injured hunger",
		"Fills the hunger bar to at least this level when you are injured, even if it wastes some hunger points.\n"
			+ "10.0 - fastest healing\n" + "9.0 - slowest healing\n"
			+ "8.5 or lower - no healing\n" + "3.0 or lower - no sprinting",
		10, 0, 10, 0.5, ValueDisplay.DECIMAL);
	
	private final SliderSetting injuryThreshold =
		new SliderSetting("Injury threshold",
			"Prevents small injuries from wasting all your food.", 1.5, 0, 10,
			0.5, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting eatWhileWalking = new CheckboxSetting(
		"Eat while walking", "Slows you down, not recommended.", false);
	
	private final CheckboxSetting allowHunger =
		new CheckboxSetting("Allow hunger effect",
			"Rotten flesh applies a harmless 'hunger' effect.\n"
				+ "It is safe to eat and useful as emergency food.",
			true);
	
	private final CheckboxSetting allowPoison =
		new CheckboxSetting("Allow poison effect",
			"Poisoned food applies damage over time.\n" + "Not recommended.",
			false);
	
	private final CheckboxSetting allowChorus =
		new CheckboxSetting("Allow chorus fruit",
			"Eating chorus fruit teleports you to a random location.\n"
				+ "Not recommended.",
			false);
	
	// TODO: "Take items from" setting (like AnchorAura)
	
	private int oldSlot = -1;
	
	public AutoEatHack()
	{
		super("AutoEat");
		setCategory(Category.ITEMS);
		
		addSetting(targetHunger);
		addSetting(minHunger);
		addSetting(injuredHunger);
		addSetting(injuryThreshold);
		
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
		
		// save old slot
		if(!isEating())
			oldSlot = inventory.selectedSlot;
		
		// select food
		inventory.selectedSlot = foodSlot;
		
		// eat food
		MC.options.useKey.setPressed(true);
		IMC.getInteractionManager().rightClickItem();
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
	
	private int findBestFoodSlot(int maxPoints)
	{
		int bestSlot = -1;
		FoodComponent bestFood = null;
		Comparator<FoodComponent> comparator =
			Comparator.<FoodComponent> comparingDouble(
				FoodComponent::getSaturationModifier);
		
		for(int i = 0; i < 9; i++)
		{
			// filter out non-food items
			Item item = MC.player.getInventory().getStack(i).getItem();
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
				bestSlot = i;
			}
		}
		
		return bestSlot;
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
}
