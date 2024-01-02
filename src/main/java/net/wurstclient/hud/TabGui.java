/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.Category;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.events.KeyPressListener;
import net.wurstclient.hacks.TooManyHaxHack;
import net.wurstclient.other_features.TabGuiOtf;
import net.wurstclient.util.ChatUtils;

public final class TabGui implements KeyPressListener
{
	private final ArrayList<Tab> tabs = new ArrayList<>();
	private final TabGuiOtf tabGuiOtf =
		WurstClient.INSTANCE.getOtfs().tabGuiOtf;
	
	private int width;
	private int height;
	private int selected;
	private boolean tabOpened;
	
	public TabGui()
	{
		WurstClient.INSTANCE.getEventManager().add(KeyPressListener.class,
			this);
		
		LinkedHashMap<Category, Tab> tabMap = new LinkedHashMap<>();
		for(Category category : Category.values())
			tabMap.put(category, new Tab(category.getName()));
		
		ArrayList<Feature> features = new ArrayList<>();
		features.addAll(WurstClient.INSTANCE.getHax().getAllHax());
		features.addAll(WurstClient.INSTANCE.getCmds().getAllCmds());
		features.addAll(WurstClient.INSTANCE.getOtfs().getAllOtfs());
		
		for(Feature feature : features)
			if(feature.getCategory() != null)
				tabMap.get(feature.getCategory()).add(feature);
			
		tabs.addAll(tabMap.values());
		tabs.forEach(Tab::updateSize);
		updateSize();
	}
	
	private void updateSize()
	{
		width = 64;
		for(Tab tab : tabs)
		{
			int tabWidth = WurstClient.MC.textRenderer.getWidth(tab.name) + 10;
			if(tabWidth > width)
				width = tabWidth;
		}
		height = tabs.size() * 10;
	}
	
	@Override
	public void onKeyPress(KeyPressEvent event)
	{
		if(event.getAction() != GLFW.GLFW_PRESS)
			return;
		
		if(tabGuiOtf.isHidden())
			return;
		
		if(tabOpened)
			switch(event.getKeyCode())
			{
				case GLFW.GLFW_KEY_LEFT:
				tabOpened = false;
				break;
				
				default:
				tabs.get(selected).onKeyPress(event.getKeyCode());
				break;
			}
		else
			switch(event.getKeyCode())
			{
				case GLFW.GLFW_KEY_DOWN:
				if(selected < tabs.size() - 1)
					selected++;
				else
					selected = 0;
				break;
				
				case GLFW.GLFW_KEY_UP:
				if(selected > 0)
					selected--;
				else
					selected = tabs.size() - 1;
				break;
				
				case GLFW.GLFW_KEY_RIGHT:
				tabOpened = true;
				break;
			}
	}
	
	public void render(DrawContext context, float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		if(tabGuiOtf.isHidden())
			return;
		
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		matrixStack.push();
		Window sr = WurstClient.MC.getWindow();
		
		int x = 2;
		int y = 23;
		
		matrixStack.translate(x, y, 0);
		drawBox(matrixStack, 0, 0, width, height);
		
		double factor = sr.getScaleFactor();
		GL11.glScissor((int)(x * factor),
			(int)((sr.getScaledHeight() - height - y) * factor),
			(int)(width * factor), (int)(height * factor));
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		
		int textY = 1;
		
		for(int i = 0; i < tabs.size(); i++)
		{
			String tabName = tabs.get(i).name;
			if(i == selected)
				tabName = (tabOpened ? "<" : ">") + tabName;
			
			context.drawText(WurstClient.MC.textRenderer, tabName, 2, textY,
				txtColor, false);
			textY += 10;
		}
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		
		if(tabOpened)
		{
			matrixStack.push();
			
			Tab tab = tabs.get(selected);
			int tabX = x + width + 2;
			int tabY = y;
			
			matrixStack.translate(width + 2, 0, 0);
			drawBox(matrixStack, 0, 0, tab.width, tab.height);
			
			GL11.glScissor((int)(tabX * factor),
				(int)((sr.getScaledHeight() - tab.height - tabY) * factor),
				(int)(tab.width * factor), (int)(tab.height * factor));
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
			
			int tabTextY = 1;
			for(int i = 0; i < tab.features.size(); i++)
			{
				Feature feature = tab.features.get(i);
				String fName = feature.getName();
				
				if(feature.isEnabled())
					fName = "\u00a7a" + fName + "\u00a7r";
				
				if(i == tab.selected)
					fName = ">" + fName;
				
				context.drawText(WurstClient.MC.textRenderer, fName, 2,
					tabTextY, txtColor, false);
				tabTextY += 10;
			}
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			
			matrixStack.pop();
		}
		
		matrixStack.pop();
		GL11.glEnable(GL11.GL_CULL_FACE);
	}
	
	private void drawBox(MatrixStack matrixStack, int x1, int y1, int x2,
		int y2)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		float opacity = gui.getOpacity();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		// color
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			opacity);
		
		// box
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		{
			bufferBuilder.vertex(matrix, x1, y1, 0).next();
			bufferBuilder.vertex(matrix, x2, y1, 0).next();
			bufferBuilder.vertex(matrix, x2, y2, 0).next();
			bufferBuilder.vertex(matrix, x1, y2, 0).next();
		}
		tessellator.draw();
		
		// outline positions
		float xi1 = x1 - 0.1F;
		float xi2 = x2 + 0.1F;
		float yi1 = y1 - 0.1F;
		float yi2 = y2 + 0.1F;
		
		// outline
		GL11.glLineWidth(1);
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		{
			bufferBuilder.vertex(matrix, xi1, yi1, 0).next();
			bufferBuilder.vertex(matrix, xi2, yi1, 0).next();
			bufferBuilder.vertex(matrix, xi2, yi2, 0).next();
			bufferBuilder.vertex(matrix, xi1, yi2, 0).next();
			bufferBuilder.vertex(matrix, xi1, yi1, 0).next();
		}
		tessellator.draw();
		
		// shadow positions
		xi1 -= 0.9;
		xi2 += 0.9;
		yi1 -= 0.9;
		yi2 += 0.9;
		
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		// top left
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION_COLOR);
		
		// top
		bufferBuilder.vertex(matrix, x1, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x2, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, xi2, yi1, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, xi1, yi1, 0).color(0, 0, 0, 0).next();
		
		// left
		bufferBuilder.vertex(matrix, xi1, yi1, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, xi1, yi2, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x1, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		
		// right
		bufferBuilder.vertex(matrix, x2, y2, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x2, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, xi2, yi1, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, xi2, yi2, 0).color(0, 0, 0, 0).next();
		
		// bottom
		bufferBuilder.vertex(matrix, xi2, yi2, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, xi1, yi2, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x2, y2, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		
		tessellator.draw();
	}
	
	private static final class Tab
	{
		private final String name;
		private final ArrayList<Feature> features = new ArrayList<>();
		
		private int width;
		private int height;
		private int selected;
		
		public Tab(String name)
		{
			this.name = name;
		}
		
		public void updateSize()
		{
			width = 64;
			for(Feature feature : features)
			{
				int fWidth =
					WurstClient.MC.textRenderer.getWidth(feature.getName())
						+ 10;
				if(fWidth > width)
					width = fWidth;
			}
			height = features.size() * 10;
		}
		
		public void onKeyPress(int keyCode)
		{
			switch(keyCode)
			{
				case GLFW.GLFW_KEY_DOWN:
				if(selected < features.size() - 1)
					selected++;
				else
					selected = 0;
				break;
				
				case GLFW.GLFW_KEY_UP:
				if(selected > 0)
					selected--;
				else
					selected = features.size() - 1;
				break;
				
				case GLFW.GLFW_KEY_ENTER:
				onEnter();
				break;
			}
		}
		
		private void onEnter()
		{
			Feature feature = features.get(selected);
			
			TooManyHaxHack tooManyHax =
				WurstClient.INSTANCE.getHax().tooManyHaxHack;
			if(tooManyHax.isEnabled() && tooManyHax.isBlocked(feature))
			{
				ChatUtils
					.error(feature.getName() + " is blocked by TooManyHax.");
				return;
			}
			
			feature.doPrimaryAction();
		}
		
		public void add(Feature feature)
		{
			features.add(feature);
		}
	}
}
