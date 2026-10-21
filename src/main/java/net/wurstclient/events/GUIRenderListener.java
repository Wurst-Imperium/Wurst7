/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface GUIRenderListener extends Listener
{
	public void onRenderGUI(GuiGraphicsExtractor extractor, float partialTicks);
	
	public static class GUIRenderEvent extends Event<GUIRenderListener>
	{
		private final float partialTicks;
		private final GuiGraphicsExtractor extractor;
		
		public GUIRenderEvent(GuiGraphicsExtractor extractor,
			float partialTicks)
		{
			this.extractor = extractor;
			this.partialTicks = partialTicks;
		}
		
		@Override
		public void fire(ArrayList<GUIRenderListener> listeners)
		{
			for(GUIRenderListener listener : listeners)
				listener.onRenderGUI(extractor, partialTicks);
		}
		
		@Override
		public Class<GUIRenderListener> getListenerType()
		{
			return GUIRenderListener.class;
		}
	}
}
