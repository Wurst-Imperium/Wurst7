/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.CommonColors;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.TemplateToolHack;

public abstract class TemplateToolState
{
	protected static final WurstClient WURST = WurstClient.INSTANCE;
	protected static final Minecraft MC = WurstClient.MC;
	
	public void onEnter(TemplateToolHack hack)
	{
		
	}
	
	public void onExit(TemplateToolHack hack)
	{
		
	}
	
	public void onUpdate(TemplateToolHack hack)
	{
		
	}
	
	public void onRender(TemplateToolHack hack, PoseStack matrixStack,
		float partialTicks)
	{
		
	}
	
	public final void onRenderGUI(TemplateToolHack hack, GuiGraphics context,
		float partialTicks)
	{
		String message = getMessage(hack);
		Font tr = MC.font;
		int msgWidth = tr.width(message);
		
		int msgX1 = context.guiWidth() / 2 - msgWidth / 2;
		int msgX2 = msgX1 + msgWidth + 2;
		int msgY1 = context.guiHeight() / 2 + 1;
		int msgY2 = msgY1 + 10;
		
		context.fill(msgX1, msgY1, msgX2, msgY2, 0x80000000);
		context.drawString(tr, message, msgX1 + 2, msgY1 + 1,
			CommonColors.WHITE, false);
	}
	
	protected abstract String getMessage(TemplateToolHack hack);
}
