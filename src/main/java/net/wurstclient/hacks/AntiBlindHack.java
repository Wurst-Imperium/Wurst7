/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.world.effect.MobEffects;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.MobEffectListener;
import net.wurstclient.hack.Hack;

@SearchTags({"AntiBlindness", "NoBlindness", "anti blindness", "no blindness",
	"AntiDarkness", "NoDarkness", "anti darkness", "no darkness",
	"AntiWardenEffect", "anti warden effect", "NoWardenEffect",
	"no warden effect"})
public final class AntiBlindHack extends Hack implements MobEffectListener
{
	public AntiBlindHack()
	{
		super("AntiBlind");
		setCategory(Category.RENDER);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(MobEffectListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(MobEffectListener.class, this);
	}
	
	@Override
	public void onMobEffect(MobEffectEvent event)
	{
		if(event.getEffect() == MobEffects.BLINDNESS
			|| event.getEffect() == MobEffects.DARKNESS)
			event.setInstance(null);
	}
}
