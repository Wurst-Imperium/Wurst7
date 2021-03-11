/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Matrix4f;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.hacks.RadarHack;

public final class RadarComponent extends Component
{
	private final RadarHack hack;
	
	public RadarComponent(RadarHack hack)
	{
		this.hack = hack;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		float opacity = gui.getOpacity();
		
		int x1 = getX();
		int x2 = x1 + getWidth();
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		int scroll = getParent().isScrollingEnabled()
			? getParent().getScrollOffset() : 0;
		boolean hovering = mouseX >= x1 && mouseY >= y1 && mouseX < x2
			&& mouseY < y2 && mouseY >= -scroll
			&& mouseY < getParent().getHeight() - 13 - scroll;
		
		Matrix4f matrix = matrixStack.peek().getModel();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.setShader(GameRenderer::method_34539);
		
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
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		
		float middleX = (x1 + x2) / 2.0F;
		float middleY = (y1 + y2) / 2.0F;
		
		matrixStack.push();
		matrixStack.translate(middleX, middleY, 0);
		ClientPlayerEntity player = WurstClient.MC.player;
		if(!hack.isRotateEnabled())
			GL11.glRotated(180 + player.yaw, 0, 0, 1);
		
		float xa1 = 0;
		float xa2 = 2;
		float xa3 = -2;
		float ya1 = -2;
		float ya2 = 2;
		float ya3 = 1;
		
		// arrow
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2],
			opacity);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa2, ya2, 0).next();
		bufferBuilder.vertex(matrix, xa1, ya3, 0).next();
		bufferBuilder.vertex(matrix, xa3, ya2, 0).next();
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		
		// outline
		RenderSystem.setShaderColor(0.0625F, 0.0625F, 0.0625F, 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa2, ya2, 0).next();
		bufferBuilder.vertex(matrix, xa1, ya3, 0).next();
		bufferBuilder.vertex(matrix, xa3, ya2, 0).next();
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		
		matrixStack.pop();
		
		// points
		GL11.glEnable(GL11.GL_POINT_SMOOTH);
		GL11.glPointSize(2);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		for(Entity e : hack.getEntities())
		{
			double diffX =
				e.lastRenderX + (e.getX() - e.lastRenderX) * partialTicks
					- (player.lastRenderX
						+ (player.getX() - player.lastRenderX) * partialTicks);
			double diffZ =
				e.lastRenderZ + (e.getZ() - e.lastRenderZ) * partialTicks
					- (player.lastRenderZ
						+ (player.getZ() - player.lastRenderZ) * partialTicks);
			double distance = Math.sqrt(diffX * diffX + diffZ * diffZ)
				* (getWidth() * 0.5 / hack.getRadius());
			double neededRotation = Math.toDegrees(Math.atan2(diffZ, diffX));
			double angle;
			if(hack.isRotateEnabled())
				angle = Math.toRadians(player.yaw - neededRotation - 90);
			else
				angle = Math.toRadians(180 - neededRotation - 90);
			double renderX = Math.sin(angle) * distance;
			double renderY = Math.cos(angle) * distance;
			
			if(Math.abs(renderX) > getWidth() / 2.0
				|| Math.abs(renderY) > getHeight() / 2.0)
				continue;
			
			int color;
			if(e instanceof PlayerEntity)
				color = 0xFF0000;
			else if(e instanceof Monster)
				color = 0xFF8000;
			else if(e instanceof AnimalEntity || e instanceof AmbientEntity
				|| e instanceof WaterCreatureEntity)
				color = 0x00FF00;
			else
				color = 0x808080;
			
			RenderSystem.setShaderColor((color >> 16 & 255) / 255F,
				(color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
			bufferBuilder.vertex(matrix, middleX + (float)renderX,
				middleY + (float)renderY, 0).next();
		}
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
	}
	
	@Override
	public int getDefaultWidth()
	{
		return 96;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 96;
	}
}
