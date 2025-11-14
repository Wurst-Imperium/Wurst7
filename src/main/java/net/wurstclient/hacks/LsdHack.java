/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.wurstclient.Category;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;

@DontSaveState
public final class LsdHack extends Hack
{
	public LsdHack()
	{
		super("LSD");
		setCategory(Category.FUN);
	}
	
	@Override
	protected void onEnable()
	{
		if(!(MC.getCameraEntity() instanceof Player))
		{
			setEnabled(false);
			return;
		}
		
		if(MC.gameRenderer.currentEffect() != null)
			MC.gameRenderer.shutdownEffect();
		
		MC.gameRenderer
			.loadEffect(ResourceLocation.parse("shaders/post/lsd.json"));
	}
	
	@Override
	protected void onDisable()
	{
		if(MC.gameRenderer.currentEffect() != null)
			MC.gameRenderer.shutdownEffect();
	}
}
