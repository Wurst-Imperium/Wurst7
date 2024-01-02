/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import java.util.Objects;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.SettingsWindow;
import net.wurstclient.clickgui.Window;
import net.wurstclient.hacks.TooManyHaxHack;
import net.wurstclient.util.ChatUtils;

public final class FeatureButton extends Component
{
	private final MinecraftClient MC = WurstClient.MC;
	private final ClickGui GUI = WurstClient.INSTANCE.getGui();
	
	private final Feature feature;
	private final boolean hasSettings;
	
	private Window settingsWindow;
	
	public FeatureButton(Feature feature)
	{
		this.feature = Objects.requireNonNull(feature);
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
		hasSettings = !feature.getSettings().isEmpty();
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseButton != 0)
			return;
		
		if(hasSettings && (mouseX > getX() + getWidth() - 12
			|| feature.getPrimaryAction().isEmpty()))
		{
			if(isSettingsWindowOpen())
				closeSettingsWindow();
			else
				openSettingsWindow();
			
			return;
		}
		
		TooManyHaxHack tooManyHax =
			WurstClient.INSTANCE.getHax().tooManyHaxHack;
		if(tooManyHax.isEnabled() && tooManyHax.isBlocked(feature))
		{
			ChatUtils.error(feature.getName() + " is blocked by TooManyHax.");
			return;
		}
		
		feature.doPrimaryAction();
	}
	
	private boolean isSettingsWindowOpen()
	{
		return settingsWindow != null && !settingsWindow.isClosing();
	}
	
	private void openSettingsWindow()
	{
		settingsWindow = new SettingsWindow(feature, getParent(), getY());
		GUI.addWindow(settingsWindow);
	}
	
	private void closeSettingsWindow()
	{
		settingsWindow.close();
		settingsWindow = null;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = hasSettings ? x2 - 11 : x2;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		boolean hHack = hovering && mouseX < x3;
		boolean hSettings = hovering && mouseX >= x3;
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		if(hHack)
			setTooltip();
		
		drawButtonBackground(matrixStack, x1, x3, y1, y2, hHack);
		
		if(hasSettings)
			drawSettingsBackground(matrixStack, x2, x3, y1, y2, hSettings);
		
		drawOutline(matrixStack, x1, x2, y1, y2);
		
		if(hasSettings)
		{
			drawSeparator(matrixStack, x3, y1, y2);
			drawSettingsArrow(matrixStack, x2, x3, y1, y2, hSettings);
		}
		
		drawName(context, x1, x3, y1);
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
	
	private void setTooltip()
	{
		String tooltip = feature.getWrappedDescription(200);
		
		// if(feature.isBlocked())
		// {
		// if(tooltip == null)
		// tooltip = "";
		// else
		// tooltip += "\n\n";
		// tooltip +=
		// "Your current YesCheat+ profile is blocking this feature.";
		// }
		
		GUI.setTooltip(tooltip);
	}
	
	private void drawButtonBackground(MatrixStack matrixStack, int x1, int x3,
		int y1, int y2, boolean hHack)
	{
		float[] bgColor = GUI.getBgColor();
		float opacity = GUI.getOpacity();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		
		if(feature.isEnabled())
			// if(feature.isBlocked())
			// glColor4f(1, 0, 0, hHack ? opacity * 1.5F : opacity);
			// else
			RenderSystem.setShaderColor(0, 1, 0,
				hHack ? opacity * 1.5F : opacity);
		else
			RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
				hHack ? opacity * 1.5F : opacity);
		
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		
		tessellator.draw();
	}
	
	private void drawSettingsBackground(MatrixStack matrixStack, int x2, int x3,
		int y1, int y2, boolean hSettings)
	{
		float[] bgColor = GUI.getBgColor();
		float opacity = GUI.getOpacity();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			hSettings ? opacity * 1.5F : opacity);
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		tessellator.draw();
	}
	
	private void drawOutline(MatrixStack matrixStack, int x1, int x2, int y1,
		int y2)
	{
		float[] acColor = GUI.getAcColor();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		tessellator.draw();
	}
	
	private void drawSeparator(MatrixStack matrixStack, int x3, int y1, int y2)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		// separator
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		tessellator.draw();
	}
	
	private void drawSettingsArrow(MatrixStack matrixStack, int x2, int x3,
		int y1, int y2, boolean hSettings)
	{
		float xa1 = x3 + 1;
		float xa2 = (x3 + x2) / 2.0F;
		float xa3 = x2 - 1;
		float ya1;
		float ya2;
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		if(isSettingsWindowOpen())
		{
			ya1 = y2 - 3.5F;
			ya2 = y1 + 3;
			RenderSystem.setShaderColor(hSettings ? 1 : 0.85F, 0, 0, 1);
			
		}else
		{
			ya1 = y1 + 3.5F;
			ya2 = y2 - 3;
			RenderSystem.setShaderColor(0, hSettings ? 1 : 0.85F, 0, 1);
		}
		
		// arrow
		bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLES,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa3, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa2, ya2, 0).next();
		tessellator.draw();
		
		// outline
		RenderSystem.setShaderColor(0.0625F, 0.0625F, 0.0625F, 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa3, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa2, ya2, 0).next();
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		tessellator.draw();
	}
	
	private void drawName(DrawContext context, int x1, int x3, int y1)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		TextRenderer tr = MC.textRenderer;
		String name = feature.getName();
		int nameWidth = tr.getWidth(name);
		int tx = x1 + (x3 - x1 - nameWidth) / 2;
		int ty = y1 + 2;
		
		context.drawText(tr, name, tx, ty, txtColor, false);
		
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		String name = feature.getName();
		TextRenderer tr = MC.textRenderer;
		int width = tr.getWidth(name) + 4;
		if(hasSettings)
			width += 11;
		
		return width;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
