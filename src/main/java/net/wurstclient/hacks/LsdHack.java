/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.wurstclient.Category;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;

@DontSaveState
public final class LsdHack extends Hack
{
	private static final Identifier LSD_POST_EFFECT =
		Identifier.fromNamespaceAndPath("wurst", "lsd");
	
	public LsdHack()
	{
		super("LSD");
		setCategory(Category.FUN);
	}
	
	@Override
	protected void onEnable()
	{
		if(!(MC.getCameraEntity() instanceof Player) || MC.player == null)
		{
			setEnabled(false);
			return;
		}
		
		List<Identifier> activePostEffects = MC.player.getActivePostEffects();
		if(!activePostEffects.contains(LSD_POST_EFFECT))
			activePostEffects.add(LSD_POST_EFFECT);
	}
	
	@Override
	protected void onDisable()
	{
		if(MC.player != null)
			MC.player.getActivePostEffects().remove(LSD_POST_EFFECT);
	}
}
