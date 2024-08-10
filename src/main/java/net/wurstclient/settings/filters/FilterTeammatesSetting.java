package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.wurstclient.WurstClient;

public final class FilterTeammatesSetting extends EntityFilterCheckbox
{
	public FilterTeammatesSetting(String description, boolean checked)
	{
		super("Filter teammates", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		Text theirName = e.getDisplayName();
		if(theirName == null)
			return false;
		TextColor theirColor = theirName.getStyle().getColor();
		if(theirColor == null)
			return false;
		
		assert WurstClient.MC.player != null;
		Text myName = WurstClient.MC.player.getDisplayName();
		if(myName == null)
			return false;
		TextColor myColor = myName.getStyle().getColor();
		if(myColor == null)
			return false;
		
		return !theirColor.equals(myColor);
	}
	
	public static FilterTeammatesSetting genericCombat(boolean checked)
	{
		return new FilterTeammatesSetting(
			"Won't attack players with the same name tag color.", checked);
	}
	
	public static FilterTeammatesSetting genericVision(boolean checked)
	{
		return new FilterTeammatesSetting(
			"Won't show players with the same name tag color.", checked);
	}
}
