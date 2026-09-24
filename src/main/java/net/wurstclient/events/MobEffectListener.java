/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface MobEffectListener extends Listener
{
	public void onMobEffect(MobEffectEvent event);
	
	/**
	 * Modifies the local player's mob effect queries, but not the active
	 * effects map. A null instance means both getEffect() == null and
	 * hasEffect() == false.
	 */
	public static class MobEffectEvent extends Event<MobEffectListener>
	{
		private final Holder<MobEffect> effect;
		private MobEffectInstance instance;
		
		public MobEffectEvent(Holder<MobEffect> effect,
			MobEffectInstance instance)
		{
			this.effect = effect;
			this.instance = instance;
		}
		
		public Holder<MobEffect> getEffect()
		{
			return effect;
		}
		
		public MobEffectInstance getInstance()
		{
			return instance;
		}
		
		public void setInstance(MobEffectInstance instance)
		{
			this.instance = instance;
		}
		
		@Override
		public void fire(ArrayList<MobEffectListener> listeners)
		{
			for(MobEffectListener listener : listeners)
				listener.onMobEffect(this);
		}
		
		@Override
		public Class<MobEffectListener> getListenerType()
		{
			return MobEffectListener.class;
		}
	}
}
