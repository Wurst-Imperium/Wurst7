package net.wurstclient.commands;

import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.*;
import net.minecraft.enchantment.Enchantment;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class EnchantCmd extends Command
{
	public EnchantCmd()
	{
		super("enchant", "Enchants an item with everything, up to level 127.", ".enchant <slot> <level>");
	}
	
	@Override
	public void call (String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		if(!MC.player.abilities.creativeMode)
			throw new CmdError("Creative mode only.");
		
		ItemStack item = getItem(args[0]);
		
		Enchantment[] ench = new Enchantment[] {Enchantments.AQUA_AFFINITY, Enchantments.BANE_OF_ARTHROPODS, Enchantments.BLAST_PROTECTION, Enchantments.CHANNELING, Enchantments.DEPTH_STRIDER, Enchantments.EFFICIENCY, Enchantments.FEATHER_FALLING, Enchantments.FIRE_ASPECT, Enchantments.FIRE_PROTECTION, Enchantments.FLAME, Enchantments.FORTUNE, Enchantments.FROST_WALKER,Enchantments.IMPALING, Enchantments.INFINITY, Enchantments.KNOCKBACK, Enchantments.LOOTING, Enchantments.LOYALTY, Enchantments.LUCK_OF_THE_SEA, Enchantments.LURE, Enchantments.MENDING, Enchantments.MULTISHOT, Enchantments.PIERCING, Enchantments.POWER, Enchantments.PROJECTILE_PROTECTION, Enchantments.PROTECTION, Enchantments.PUNCH, Enchantments.QUICK_CHARGE, Enchantments.RESPIRATION, Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.SILK_TOUCH, Enchantments.SWEEPING, Enchantments.THORNS, Enchantments.UNBREAKING};
		
		if(item == null)
			throw new CmdError("There is no item in this slot");
		
		int level = Integer.parseInt(args[1]);
		
		for(Enchantment enchantment : ench)
			try
			{
				if(enchantment == Enchantments.QUICK_CHARGE)
					item.addEnchantment(enchantment, 5);
				else
					item.addEnchantment(enchantment, level);
			}catch(Exception e)
		{
				
		}
		
		ChatUtils.message("Item enchanted.");
	}
	
	private ItemStack getItem(String slot)
			throws CmdSyntaxError
		{
			switch(slot.toLowerCase())
			{
				case "hand":
				return MC.player.inventory.getMainHandStack();
				
				case "head":
				return MC.player.inventory.getArmorStack(3);
				
				case "chest":
				return MC.player.inventory.getArmorStack(2);
				
				case "legs":
				return MC.player.inventory.getArmorStack(1);
				
				case "feet":
				return MC.player.inventory.getArmorStack(0);
				
				default:
				throw new CmdSyntaxError();
			}
		}
}
