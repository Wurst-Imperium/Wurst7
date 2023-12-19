/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.potion.PotionUtil;
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
		NbtList effects;
		switch(args[0].toLowerCase())
		{
			case "add":
			effects = convertEffectsToNbt(stack);
			break;
			
			case "set":
			effects = new NbtList();
			break;
			
			default:
			throw new CmdSyntaxError();
		}
		
		// add new effects
		for(int i = 0; i < (args.length - 1) / 3; i++)
		{
			NbtCompound effect = new NbtCompound();
			
			effect.putString("id", parseEffectId(args[1 + i * 3]));
			effect.putInt("amplifier", parseInt(args[2 + i * 3]) - 1);
			effect.putInt("duration", parseInt(args[3 + i * 3]) * 20);
			
			effects.add(effect);
		}
		
		NbtCompound nbt = new NbtCompound();
		nbt.put("custom_potion_effects", effects);
		stack.setNbt(nbt);
		ChatUtils.message("Potion modified.");
	}
	
	private NbtList convertEffectsToNbt(ItemStack stack)
	{
		NbtList nbt = new NbtList();
		List<StatusEffectInstance> effects =
			PotionUtil.getCustomPotionEffects(stack);
		
		for(StatusEffectInstance effect : effects)
		{
			NbtCompound tag = new NbtCompound();
			
			String id = Registries.STATUS_EFFECT
				.getId(effect.getEffectType().value()).toString();
			tag.putString("id", id);
			tag.putInt("amplifier", effect.getAmplifier());
			tag.putInt("duration", effect.getDuration());
			
			nbt.add(tag);
		}
		
		return nbt;
	}
	
	private void remove(ItemStack stack, String[] args) throws CmdSyntaxError
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String id = parseEffectId(args[1]);
		
		List<StatusEffectInstance> oldEffects =
			PotionUtil.getCustomPotionEffects(stack);
		
		NbtList newEffects = new NbtList();
		for(StatusEffectInstance oldEffect : oldEffects)
		{
			String oldId = Registries.STATUS_EFFECT
				.getId(oldEffect.getEffectType().value()).toString();
			
			if(oldId.equals(id))
				continue;
			
			NbtCompound effect = new NbtCompound();
			effect.putString("id", oldId);
			effect.putInt("amplifier", oldEffect.getAmplifier());
			effect.putInt("duration", oldEffect.getDuration());
			newEffects.add(effect);
		}
		
		NbtCompound nbt = new NbtCompound();
		nbt.put("custom_potion_effects", newEffects);
		stack.setNbt(nbt);
		ChatUtils.message("Effect removed.");
	}
	
	private String parseEffectId(String input) throws CmdSyntaxError
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
		
		return Registries.STATUS_EFFECT.getId(effect).toString();
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
