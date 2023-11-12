package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"friction", "no friction", "slippery", "slipperiness"})
public final class NoFrictionHack extends Hack
{
	public NoFrictionHack()
	{
		super("NoFriction");
		setCategory(Category.MOVEMENT);
	}
}
