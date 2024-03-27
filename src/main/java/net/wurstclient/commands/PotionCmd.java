/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.ArrayList;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.MathUtils;

public final class PotionCmd extends Command
{
	public PotionCmd()
	{
		super("potion", "Changes the effects of the held potion.",
			".potion add (<effect> <amplifier> <duration>)...",
			".potion set (<effect> <amplifier> <duration>)...",
			".potion remove <effect>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length == 0)
			throw new CmdSyntaxError();
		
		if(!MC.player.getAbilities().creativeMode)
			throw new CmdError("Creative mode only.");
		
		ItemStack stack = MC.player.getInventory().getMainHandStack();
		if(!(stack.getItem() instanceof PotionItem))
			throw new CmdError("You must hold a potion in your main hand.");
		
		// remove
		if(args[0].equalsIgnoreCase("remove"))
		{
			remove(stack, args);
			return;
		}
		
		if((args.length - 1) % 3 != 0)
			throw new CmdSyntaxError();
		
		// get effects to start with
		ArrayList<StatusEffectInstance> effects;
		Potion potion;
		switch(args[0].toLowerCase())
		{
			case "add":
			effects = new ArrayList<>(PotionUtil.getCustomPotionEffects(stack));
			potion = PotionUtil.getPotion(stack);
			break;
			
			case "set":
			effects = new ArrayList<>();
			potion = Potions.EMPTY;
			break;
			
			default:
			throw new CmdSyntaxError();
		}
		
		// add new effects
		for(int i = 0; i < (args.length - 1) / 3; i++)
		{
			StatusEffect effect = parseEffect(args[1 + i * 3]);
			int amplifier = parseInt(args[2 + i * 3]) - 1;
			int duration = parseInt(args[3 + i * 3]) * 20;
			
			effects.add(new StatusEffectInstance(effect, duration, amplifier));
		}
		
		PotionUtil.setPotion(stack, potion);
		setCustomPotionEffects(stack, effects);
		ChatUtils.message("Potion modified.");
	}
	
	private void remove(ItemStack stack, String[] args) throws CmdSyntaxError
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		StatusEffect targetEffect = parseEffect(args[1]);
		
		Potion oldPotion = PotionUtil.getPotion(stack);
		boolean mainPotionContainsTargetEffect = oldPotion.getEffects().stream()
			.anyMatch(effect -> effect.getEffectType() == targetEffect);
		
		ArrayList<StatusEffectInstance> newEffects = new ArrayList<>();
		if(mainPotionContainsTargetEffect)
			PotionUtil.getPotionEffects(stack).forEach(newEffects::add);
		else
			PotionUtil.getCustomPotionEffects(stack).forEach(newEffects::add);
		newEffects.removeIf(effect -> effect.getEffectType() == targetEffect);
		
		Potion newPotion =
			mainPotionContainsTargetEffect ? Potions.EMPTY : oldPotion;
		
		PotionUtil.setPotion(stack, newPotion);
		setCustomPotionEffects(stack, newEffects);
		ChatUtils.message("Effect removed.");
	}
	
	private StatusEffect parseEffect(String input) throws CmdSyntaxError
	{
		StatusEffect effect;
		
		if(MathUtils.isInteger(input))
			effect = Registries.STATUS_EFFECT.get(Integer.parseInt(input));
		else
			try
			{
				Identifier identifier = new Identifier(input);
				effect = Registries.STATUS_EFFECT.get(identifier);
				
			}catch(InvalidIdentifierException e)
			{
				throw new CmdSyntaxError("Invalid effect: " + input);
			}
		
		if(effect == null)
			throw new CmdSyntaxError("Invalid effect: " + input);
		
		return Registries.STATUS_EFFECT.getEntry(effect).value();
	}
	
	private void setCustomPotionEffects(ItemStack stack,
		ArrayList<StatusEffectInstance> effects)
	{
		// PotionUtil doesn't remove effects when passing an empty list to it
		if(effects.isEmpty())
			stack.removeSubNbt("custom_potion_effects");
		else
			PotionUtil.setCustomPotionEffects(stack, effects);
	}
	
	private int parseInt(String s) throws CmdSyntaxError
	{
		try
		{
			return Integer.parseInt(s);
			
		}catch(NumberFormatException e)
		{
			throw new CmdSyntaxError("Not a number: " + s);
		}
	}
}
