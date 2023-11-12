package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"friction", "no friction", "slippery", "slipperiness"})
public final class NoFrictionHack extends Hack
{
  public final SliderSetting friction = new SliderSetting("Friction/Slipperiness", 0.989, 0, 20, 0.001, ValueDisplay.DECIMAL);
	
	public NoFrictionHack()
	{
		super("NoFriction");
		setCategory(Category.MOVEMENT);
		addSetting(friction);
	}
}
