/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import net.wurstclient.core.MatrixUtils;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.Window;
import net.wurstclient.clickgui.screens.EditColorScreen;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.ColorUtils;

public final class ColorComponent extends Component
{
	private final MinecraftClient MC = WurstClient.MC;
	private final ClickGui GUI = WurstClient.INSTANCE.getGui();
	
	private final ColorSetting setting;
	
	public ColorComponent(ColorSetting setting)
	{
		this.setting = setting;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseY < getY() + 11)
			return;
		
		if(mouseButton == 0)
			WurstClient.setScreen(new EditColorScreen(MC.currentScreen, setting));
		else if(mouseButton == 1)
			setting.setColor(setting.getDefaultColor());
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int y1 = getY();
		int y2 = y1 + getHeight();
		int y3 = y1 + 11;
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		if(hovering)
			if(mouseY < y3)
				GUI.setTooltip(setting.getWrappedDescription(200));
			else
			{
				String tooltip = "§cR:§r" + setting.getRed();
				tooltip += " §aG:§r" + setting.getGreen();
				tooltip += " §9B:§r" + setting.getBlue();
				tooltip += "\n\n§e[left-click]§r to edit";
				tooltip += "\n§e[right-click]§r to reset";
				GUI.setTooltip(tooltip);
			}
		
		drawBackground(matrixStack, x1, x2, y1, y3);
		drawBox(matrixStack, x1, x2, y2, y3, hovering && mouseY >= y3);
		
		drawNameAndValue(matrixStack, x1, x2, y1 + 2);
	}
	
	private boolean isHovering(int mouseX, int mouseY, int x1, int x2, int y1,
		int y2)
	{
		Window parent = getParent();
		boolean scrollEnabled = parent.isScrollingEnabled();
		int scroll = scrollEnabled ? parent.getScrollOffset() : 0;
		
		return mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2
			&& mouseY >= -scroll && mouseY < parent.getHeight() - 13 - scroll;
	}
	
	private void drawBackground(MatrixStack matrixStack, int x1, int x2, int y1,
		int y2)
	{
		float[] bgColor = GUI.getBgColor();
		float opacity = GUI.getOpacity();
		
		Matrix4f matrix = MatrixUtils.getPositionMatrix(matrixStack);
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			opacity);
		
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.end();
		
		BufferRenderer.draw(bufferBuilder);
	}
	
	private void drawBox(MatrixStack matrixStack, int x1, int x2, int y2,
		int y3, boolean hovering)
	{
		float[] color = setting.getColorF();
		float[] acColor = GUI.getAcColor();
		float opacity = GUI.getOpacity();
		
		Matrix4f matrix = MatrixUtils.getPositionMatrix(matrixStack);
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		
		RenderSystem.setShaderColor(color[0], color[1], color[2],
			hovering ? 1F : opacity);
		
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x1, y3, 0).next();
		bufferBuilder.vertex(matrix, x2, y3, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x1, y3, 0).next();
		bufferBuilder.vertex(matrix, x2, y3, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.end();
		
		BufferRenderer.draw(bufferBuilder);
	}
	
	private void drawNameAndValue(MatrixStack matrixStack, int x1, int x2,
		int y1)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		TextRenderer tr = MC.textRenderer;
		
		tr.draw(matrixStack, setting.getName(), x1, y1, txtColor);
		
		String value = ColorUtils.toHex(setting.getColor());
		int valueWidth = tr.getWidth(value);
		tr.draw(matrixStack, value, x2 - valueWidth, y1, txtColor);
		
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		return MC.textRenderer.getWidth(setting.getName() + "#FFFFFF") + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 22;
	}
}
