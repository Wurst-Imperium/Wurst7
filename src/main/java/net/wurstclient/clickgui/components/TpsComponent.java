/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.hacks.TpsDisplayHack;

import java.awt.Color;

public final class TpsComponent extends Component
{
	private final TpsDisplayHack hack;

	public TpsComponent(TpsDisplayHack hack)
	{
		this.hack = hack;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
					   float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		float opacity = gui.getOpacity();

		TextRenderer fr = WurstClient.MC.textRenderer;

		int x1 = getX();
		int x2 = x1 + getWidth();
		int y1 = getY();
		int y2 = y1 + getHeight();

		int scroll = getParent().isScrollingEnabled()
				? getParent().getScrollOffset() : 0;
		boolean hovering = mouseX >= x1 && mouseY >= y1 && mouseX < x2
				&& mouseY < y2 && mouseY >= -scroll
				&& mouseY < getParent().getHeight() - 13 - scroll;

		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);

		// tooltip
		if(hovering)
			gui.setTooltip("");

		// background
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
				opacity);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
				VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		tessellator.draw();
		// text
		RenderSystem.setShaderColor(1, 1, 1, 1);
		float tps = hack.getServerTps();
		float lastTickSecond = hack.getLastServerTickMs() / 1000.0f;
		if (lastTickSecond < 6.0)
			context.drawText(fr, String.format("TPS: %.2g", tps), x1, y1, tps > 19.0 ? gui.getTxtColor() : Color.RED.getRGB(), false);
		else
			context.drawText(fr, String.format("TPS: < %.2g", 20.0f / lastTickSecond), x1, y1, Color.RED.getRGB(), false);
		context.drawText(fr, String.format("Last Tick: %.1fs", lastTickSecond), x1, y1+fr.fontHeight, lastTickSecond < 2.0 ? gui.getTxtColor() : Color.RED.getRGB(), false);
		context.drawText(fr, String.format("Ping: %dms", hack.getLatencyMs()), x1, y1+fr.fontHeight*2, gui.getTxtColor(), false);
	}

	@Override
	public int getDefaultWidth()
	{
		return 81;
	}

	@Override
	public int getDefaultHeight()
	{
		return 9 * 3;
	}
}
