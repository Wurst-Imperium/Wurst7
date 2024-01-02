/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.hacks.RadarHack;
import net.wurstclient.util.EntityUtils;

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
	public void render(DrawContext context, int mouseX, int mouseY,
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
		
		MatrixStack matrixStack = context.getMatrices();
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
		
		float middleX = (x1 + x2) / 2.0F;
		float middleY = (y1 + y2) / 2.0F;
		
		matrixStack.push();
		matrixStack.translate(middleX, middleY, 0);
		matrix = matrixStack.peek().getPositionMatrix();
		
		ClientPlayerEntity player = WurstClient.MC.player;
		if(!hack.isRotateEnabled())
			matrixStack.multiply(new Quaternionf().rotationZ(
				(180 + player.getYaw()) * MathHelper.RADIANS_PER_DEGREE));
		
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
		tessellator.draw();
		
		// outline
		RenderSystem.setShaderColor(0.0625F, 0.0625F, 0.0625F, 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa2, ya2, 0).next();
		bufferBuilder.vertex(matrix, xa1, ya3, 0).next();
		bufferBuilder.vertex(matrix, xa3, ya2, 0).next();
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		tessellator.draw();
		
		matrixStack.pop();
		matrix = matrixStack.peek().getPositionMatrix();
		Vec3d lerpedPlayerPos = EntityUtils.getLerpedPos(player, partialTicks);
		
		// points
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION_COLOR);
		for(Entity e : hack.getEntities())
		{
			Vec3d lerpedEntityPos = EntityUtils.getLerpedPos(e, partialTicks);
			double diffX = lerpedEntityPos.x - lerpedPlayerPos.x;
			double diffZ = lerpedEntityPos.z - lerpedPlayerPos.z;
			double distance = Math.sqrt(diffX * diffX + diffZ * diffZ)
				* (getWidth() * 0.5 / hack.getRadius());
			double neededRotation = Math.toDegrees(Math.atan2(diffZ, diffX));
			double angle;
			if(hack.isRotateEnabled())
				angle = Math.toRadians(player.getYaw() - neededRotation - 90);
			else
				angle = Math.toRadians(180 - neededRotation - 90);
			double renderX = Math.sin(angle) * distance;
			double renderY = Math.cos(angle) * distance;
			
			if(Math.abs(renderX) > getWidth() / 2.0
				|| Math.abs(renderY) > getHeight() / 2.0)
				continue;
			
			int color;
			if(WurstClient.INSTANCE.getFriends().isFriend(e))
				color = 0x0000FF;
			else if(e instanceof PlayerEntity)
				color = 0xFF0000;
			else if(e instanceof Monster)
				color = 0xFF8000;
			else if(e instanceof AnimalEntity || e instanceof AmbientEntity
				|| e instanceof WaterCreatureEntity)
				color = 0x00FF00;
			else
				color = 0x808080;
			
			float red = (color >> 16 & 255) / 255F;
			float green = (color >> 8 & 255) / 255F;
			float blue = (color & 255) / 255F;
			float alpha = 1;
			bufferBuilder
				.vertex(matrix, middleX + (float)renderX - 0.5F,
					middleY + (float)renderY - 0.5F, 0)
				.color(red, green, blue, alpha).next();
			bufferBuilder
				.vertex(matrix, middleX + (float)renderX + 0.5F,
					middleY + (float)renderY - 0.5F, 0)
				.color(red, green, blue, alpha).next();
			bufferBuilder
				.vertex(matrix, middleX + (float)renderX + 0.5F,
					middleY + (float)renderY + 0.5F, 0)
				.color(red, green, blue, alpha).next();
			bufferBuilder
				.vertex(matrix, middleX + (float)renderX - 0.5F,
					middleY + (float)renderY + 0.5F, 0)
				.color(red, green, blue, alpha).next();
		}
		tessellator.draw();
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
