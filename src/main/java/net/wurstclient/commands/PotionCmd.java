/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.ArrayList;
import java.util.Optional;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
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
		
		ItemStack stack = MC.player.getInventory().getSelectedStack();
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
		
		PotionContentsComponent oldContents = stack.getComponents()
			.getOrDefault(DataComponentTypes.POTION_CONTENTS,
				PotionContentsComponent.DEFAULT);
		
		// get effects to start with
		ArrayList<StatusEffectInstance> effects;
		Optional<RegistryEntry<Potion>> potion;
		switch(args[0].toLowerCase())
		{
			case "add":
			effects = new ArrayList<>(oldContents.customEffects());
			potion = oldContents.potion();
			break;
			
			case "set":
			effects = new ArrayList<>();
			potion = Optional.empty();
			break;
			
			default:
			throw new CmdSyntaxError();
		}
		
		// add new effects
		for(int i = 0; i < (args.length - 1) / 3; i++)
		{
			RegistryEntry<StatusEffect> effect = parseEffect(args[1 + i * 3]);
			int amplifier = parseInt(args[2 + i * 3]) - 1;
			int duration = parseInt(args[3 + i * 3]) * 20;
			
			effects.add(new StatusEffectInstance(effect, duration, amplifier));
		}
		
		stack.set(DataComponentTypes.POTION_CONTENTS,
			new PotionContentsComponent(potion, oldContents.customColor(),
				effects, oldContents.customName()));
		ChatUtils.message("Potion modified.");
	}
	
	private void remove(ItemStack stack, String[] args) throws CmdSyntaxError
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		RegistryEntry<StatusEffect> targetEffect = parseEffect(args[1]);
		
		PotionContentsComponent oldContents = stack.getComponents()
			.getOrDefault(DataComponentTypes.POTION_CONTENTS,
				PotionContentsComponent.DEFAULT);
		
		boolean mainPotionContainsTargetEffect =
			oldContents.potion().isPresent()
				&& oldContents.potion().get().value().getEffects().stream()
					.anyMatch(effect -> effect.getEffectType() == targetEffect);
		
		ArrayList<StatusEffectInstance> newEffects = new ArrayList<>();
		if(mainPotionContainsTargetEffect)
			oldContents.getEffects().forEach(newEffects::add);
		else
			oldContents.customEffects().forEach(newEffects::add);
		newEffects.removeIf(effect -> effect.getEffectType() == targetEffect);
		
		Optional<RegistryEntry<Potion>> newPotion =
			mainPotionContainsTargetEffect ? Optional.empty()
				: oldContents.potion();
		stack.set(DataComponentTypes.POTION_CONTENTS,
			new PotionContentsComponent(newPotion, oldContents.customColor(),
				newEffects, oldContents.customName()));
		
		ChatUtils.message("Effect removed.");
	}
	
	private RegistryEntry<StatusEffect> parseEffect(String input)
		throws CmdSyntaxError
	{
		StatusEffect effect;
		
		if(MathUtils.isInteger(input))
			effect = Registries.STATUS_EFFECT.get(Integer.parseInt(input));
		else
			try
			{
				Identifier identifier = Identifier.of(input);
				effect = Registries.STATUS_EFFECT.get(identifier);
				
			}catch(InvalidIdentifierException e)
			{
				throw new CmdSyntaxError("Invalid effect: " + input);
			}
		
		if(effect == null)
			throw new CmdSyntaxError("Invalid effect: " + input);
		
		return Registries.STATUS_EFFECT.getEntry(effect);
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
