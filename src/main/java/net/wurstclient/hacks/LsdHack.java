/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
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
		if(!(MC.getCameraEntity() instanceof PlayerEntity))
		{
			setEnabled(false);
			return;
		}
		
		if(MC.gameRenderer.getPostProcessorId() != null)
			MC.gameRenderer.clearPostProcessor();
		
		MC.gameRenderer.setPostProcessor(Identifier.of("wurst", "lsd"));
	}
	
	@Override
	protected void onDisable()
	{
		if(MC.gameRenderer.getPostProcessorId() != null)
			MC.gameRenderer.clearPostProcessor();
	}
}
