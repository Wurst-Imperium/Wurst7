/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.List;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;
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
		
		if(!MC.player.abilities.creativeMode)
			throw new CmdError("Creative mode only.");
		
		ItemStack stack = MC.player.inventory.getMainHandStack();
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
		ListTag effects;
		switch(args[0].toLowerCase())
		{
			case "add":
			effects = convertEffectsToNbt(stack);
			break;
			
			case "set":
			effects = new ListTag();
			break;
			
			default:
			throw new CmdSyntaxError();
		}
		
		// add new effects
		for(int i = 0; i < (args.length - 1) / 3; i++)
		{
			CompoundTag effect = new CompoundTag();
			
			effect.putInt("Id", parseEffectId(args[1 + i * 3]));
			effect.putInt("Amplifier", parseInt(args[2 + i * 3]) - 1);
			effect.putInt("Duration", parseInt(args[3 + i * 3]) * 20);
			
			effects.add(effect);
		}
		
		CompoundTag nbt = new CompoundTag();
		nbt.put("CustomPotionEffects", effects);
		stack.setTag(nbt);
		ChatUtils.message("Potion modified.");
	}
	
	private ListTag convertEffectsToNbt(ItemStack stack)
	{
		ListTag nbt = new ListTag();
		List<StatusEffectInstance> effects =
			PotionUtil.getCustomPotionEffects(stack);
		
		for(StatusEffectInstance effect : effects)
		{
			CompoundTag tag = new CompoundTag();
			
			int id = StatusEffect.getRawId(effect.getEffectType());
			tag.putInt("Id", id);
			tag.putInt("Amplifier", effect.getAmplifier());
			tag.putInt("Duration", effect.getDuration());
			
			nbt.add(tag);
		}
		
		return nbt;
	}
	
	private void remove(ItemStack stack, String[] args) throws CmdSyntaxError
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		int id = parseEffectId(args[1]);
		
		List<StatusEffectInstance> oldEffects =
			PotionUtil.getCustomPotionEffects(stack);
		
		ListTag newEffects = new ListTag();
		for(StatusEffectInstance oldEffect : oldEffects)
		{
			int oldId = StatusEffect.getRawId(oldEffect.getEffectType());
			
			if(oldId == id)
				continue;
			
			CompoundTag effect = new CompoundTag();
			effect.putInt("Id", oldId);
			effect.putInt("Amplifier", oldEffect.getAmplifier());
			effect.putInt("Duration", oldEffect.getDuration());
			newEffects.add(effect);
		}
		
		CompoundTag nbt = new CompoundTag();
		nbt.put("CustomPotionEffects", newEffects);
		stack.setTag(nbt);
		ChatUtils.message("Effect removed.");
	}
	
	private int parseEffectId(String input) throws CmdSyntaxError
	{
		int id = 0;
		
		if(MathUtils.isInteger(input))
			id = Integer.parseInt(input);
		else
			try
			{
				Identifier identifier = new Identifier(input);
				StatusEffect effect = Registry.STATUS_EFFECT.get(identifier);
				
				id = StatusEffect.getRawId(effect);
				
			}catch(InvalidIdentifierException e)
			{
				throw new CmdSyntaxError("Invalid effect: " + input);
			}
		
		if(id < 1)
			throw new CmdSyntaxError();
		
		return id;
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
