/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.potion.PotionUtil;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"AutoPotion", "auto potion", "AutoSplashPotion",
	"auto splash potion"})
public final class AutoPotionHack extends Hack implements UpdateListener
{
	private final SliderSetting health = new SliderSetting("Health",
		"Throws a potion when your health\n"
			+ "reaches this value or falls below it.",
		6, 0.5, 9.5, 0.5,
		v -> ValueDisplay.DECIMAL.getValueString(v) + " hearts");
	
	private int timer;
	
	public AutoPotionHack()
	{
		super("AutoPotion", "Automatically throws splash potions of\n"
			+ "instant health when your health is low.");
		
		setCategory(Category.COMBAT);
		addSetting(health);
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
		timer = 0;
	}
	
	@Override
	public void onUpdate()
	{
		// search potion in hotbar
		int potionInHotbar = findPotion(0, 9);
		
		// check if any potion was found
		if(potionInHotbar != -1)
		{
			// check timer
			if(timer > 0)
			{
				timer--;
				return;
			}
			
			// check health
			if(MC.player.getHealth() > health.getValueF() * 2F)
				return;
			
			// save old slot
			int oldSlot = MC.player.inventory.selectedSlot;
			
			// throw potion in hotbar
			MC.player.inventory.selectedSlot = potionInHotbar;
			MC.player.networkHandler
				.sendPacket(new PlayerMoveC2SPacket.LookOnly(MC.player.yaw, 90,
					MC.player.isOnGround()));
			IMC.getInteractionManager().rightClickItem();
			
			// reset slot and rotation
			MC.player.inventory.selectedSlot = oldSlot;
			MC.player.networkHandler
				.sendPacket(new PlayerMoveC2SPacket.LookOnly(MC.player.yaw,
					MC.player.pitch, MC.player.isOnGround()));
			
			// reset timer
			timer = 10;
			
			return;
		}
		
		// search potion in inventory
		int potionInInventory = findPotion(9, 36);
		
		// move potion in inventory to hotbar
		if(potionInInventory != -1)
			IMC.getInteractionManager()
				.windowClick_QUICK_MOVE(potionInInventory);
	}
	
	private int findPotion(int startSlot, int endSlot)
	{
		for(int i = startSlot; i < endSlot; i++)
		{
			ItemStack stack = MC.player.inventory.getStack(i);
			
			// filter out non-splash potion items
			if(stack.getItem() != Items.SPLASH_POTION)
				continue;
			
			// search for instant health effects
			if(hasEffect(stack, StatusEffects.INSTANT_HEALTH))
				return i;
		}
		
		return -1;
	}
	
	private boolean hasEffect(ItemStack stack, StatusEffect effect)
	{
		for(StatusEffectInstance effectInstance : PotionUtil
			.getPotionEffects(stack))
		{
			if(effectInstance.getEffectType() != effect)
				continue;
			
			return true;
		}
		
		return false;
	}
}
