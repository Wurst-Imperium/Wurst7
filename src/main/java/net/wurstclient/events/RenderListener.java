/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface RenderListener extends Listener
{
	public void onRender(float partialTicks);
	
	public static class RenderEvent extends Event<RenderListener>
	{
		private final float partialTicks;
		
		public RenderEvent(float partialTicks)
		{
			this.partialTicks = partialTicks;
		}
		
		@Override
		public void fire(ArrayList<RenderListener> listeners)
		{
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			
			for(RenderListener listener : listeners)
				listener.onRender(partialTicks);
			
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
		}
		
		@Override
		public Class<RenderListener> getListenerType()
		{
			return RenderListener.class;
		}
	}
}
