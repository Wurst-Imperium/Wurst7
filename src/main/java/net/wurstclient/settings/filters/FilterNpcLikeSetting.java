package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class FilterNpcLikeSetting extends EntityFilterCheckbox
{
	public FilterNpcLikeSetting(String description, boolean checked)
	{
		super("Filter NPC-like", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		if(!(e instanceof PlayerEntity))
			return true;
		
		String name = e.getName().getString();
		if(name.length() != 10)
			return true;
		
		int letters = 0, digits = 0, maxLetters = 0, maxDigits = 0;
		for(int c : name.chars().toArray())
		{
			if(Character.isDigit(c))
			{
				letters = 0;
				digits += 1;
			}else if(Character.isLetter(c) && Character.isLowerCase(c))
			{
				digits = 0;
				letters += 1;
			}else
			{
				return true;
			}
			
			if(letters > maxLetters)
				maxLetters = letters;
			if(digits > maxDigits)
				maxDigits = digits;
		}
		
		if(maxDigits == 0)
			return true;
		if(maxLetters >= 4 && maxDigits >= 4)
			return true;
		if(maxLetters >= 5)
			return true;
		
		return false;
	}
	
	public static FilterNpcLikeSetting genericCombat(boolean checked)
	{
		return new FilterNpcLikeSetting(
			"Won't attack NPC-like players (Sort by Hypixel NPC names.)",
			checked);
	}
	
	public static FilterNpcLikeSetting genericVision(boolean checked)
	{
		return new FilterNpcLikeSetting(
			"Won't show NPC-like players (Sort by Hypixel NPC names.)",
			checked);
	}
}
