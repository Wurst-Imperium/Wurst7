package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;

public final class FilterFireballSetting extends EntityFilterCheckbox
{
	public FilterFireballSetting(String description, boolean checked)
	{
		super("Filter fireballs", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof FireballEntity);
	}
	
	public static FilterFireballSetting genericCombat(boolean checked)
	{
		return new FilterFireballSetting("Won't attack fireballs.", checked);
	}
}
