/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface RenderListener extends Listener
{
	public void onRender(MatrixStack matrixStack, float partialTicks);
	
	public static class RenderEvent extends Event<RenderListener>
	{
		private final MatrixStack matrixStack;
		private final float partialTicks;
		
		public RenderEvent(MatrixStack matrixStack, float partialTicks)
		{
			this.matrixStack = matrixStack;
			this.partialTicks = partialTicks;
		}
		
		@Override
		public void fire(ArrayList<RenderListener> listeners)
		{
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			
			for(RenderListener listener : listeners)
				listener.onRender(matrixStack, partialTicks);
			
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
		}
		
		@Override
		public Class<RenderListener> getListenerType()
		{
			return RenderListener.class;
		}
	}
}
