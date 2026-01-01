/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.ArrayList;
import java.util.Optional;

import net.minecraft.IdentifierException;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
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
		
		if(!MC.player.getAbilities().instabuild)
			throw new CmdError("Creative mode only.");
		
		ItemStack stack = MC.player.getInventory().getSelectedItem();
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
		
		PotionContents oldContents = stack.getComponents()
			.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		
		// get effects to start with
		ArrayList<MobEffectInstance> effects;
		Optional<Holder<Potion>> potion;
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
			Holder<MobEffect> effect = parseEffect(args[1 + i * 3]);
			int amplifier = parseInt(args[2 + i * 3]) - 1;
			int duration = parseInt(args[3 + i * 3]) * 20;
			
			effects.add(new MobEffectInstance(effect, duration, amplifier));
		}
		
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion,
			oldContents.customColor(), effects, oldContents.customName()));
		ChatUtils.message("Potion modified.");
	}
	
	private void remove(ItemStack stack, String[] args) throws CmdSyntaxError
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		Holder<MobEffect> targetEffect = parseEffect(args[1]);
		
		PotionContents oldContents = stack.getComponents()
			.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		
		boolean mainPotionContainsTargetEffect =
			oldContents.potion().isPresent()
				&& oldContents.potion().get().value().getEffects().stream()
					.anyMatch(effect -> effect.getEffect() == targetEffect);
		
		ArrayList<MobEffectInstance> newEffects = new ArrayList<>();
		if(mainPotionContainsTargetEffect)
			oldContents.getAllEffects().forEach(newEffects::add);
		else
			oldContents.customEffects().forEach(newEffects::add);
		newEffects.removeIf(effect -> effect.getEffect() == targetEffect);
		
		Optional<Holder<Potion>> newPotion = mainPotionContainsTargetEffect
			? Optional.empty() : oldContents.potion();
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(newPotion,
			oldContents.customColor(), newEffects, oldContents.customName()));
		
		ChatUtils.message("Effect removed.");
	}
	
	private Holder<MobEffect> parseEffect(String input) throws CmdSyntaxError
	{
		MobEffect effect;
		
		if(MathUtils.isInteger(input))
			effect = BuiltInRegistries.MOB_EFFECT.byId(Integer.parseInt(input));
		else
			try
			{
				Identifier identifier = Identifier.parse(input);
				effect = BuiltInRegistries.MOB_EFFECT.getValue(identifier);
				
			}catch(IdentifierException e)
			{
				throw new CmdSyntaxError("Invalid effect: " + input);
			}
		
		if(effect == null)
			throw new CmdSyntaxError("Invalid effect: " + input);
		
		return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
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
