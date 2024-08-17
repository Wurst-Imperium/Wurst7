/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import java.awt.Color;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

public final class AutoFishDebugDraw
{
	private final CheckboxSetting debugDraw = new CheckboxSetting("Debug draw",
		"Shows where bites are occurring.", false);
	
	private final ColorSetting ddColor = new ColorSetting("DD color",
		"Color of the debug draw, if enabled.", Color.RED);
	
	private final FishingSpotManager fishingSpots;
	private Vec3d lastSoundPos;
	
	public AutoFishDebugDraw(FishingSpotManager fishingSpots)
	{
		this.fishingSpots = fishingSpots;
	}
	
	public Stream<Setting> getSettings()
	{
		return Stream.of(debugDraw, ddColor);
	}
	
	public void reset()
	{
		lastSoundPos = null;
	}
	
	public void updateSoundPos(PlaySoundS2CPacket sound)
	{
		lastSoundPos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
	}
	
	public void render(MatrixStack matrixStack, float partialTicks)
	{
		if(!debugDraw.isChecked() && !fishingSpots.isMcmmoMode())
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		if(debugDraw.isChecked())
		{
			if(lastSoundPos != null)
				drawLastBite(matrixStack, region);
			
			drawFishingSpots(matrixStack, region);
		}
		
		if(fishingSpots.isMcmmoMode())
			drawMcmmoRange(matrixStack, region);
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private void drawLastBite(MatrixStack matrixStack, RegionPos region)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		matrixStack.push();
		matrixStack.translate(lastSoundPos.x - region.x(), lastSoundPos.y,
			lastSoundPos.z - region.z());
		
		ddColor.setAsShaderColor(0.5F);
		
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, -0.125F, 0, -0.125F);
		bufferBuilder.vertex(matrix, 0.125F, 0, 0.125F);
		bufferBuilder.vertex(matrix, 0.125F, 0, -0.125F);
		bufferBuilder.vertex(matrix, -0.125F, 0, 0.125F);
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
		
		matrixStack.pop();
	}
	
	private void drawFishingSpots(MatrixStack matrixStack, RegionPos region)
	{
		Box headBox = new Box(-0.25, 0, -0.25, 0.25, 0.5, 0.25);
		Box noseBox =
			headBox.offset(0.125, 0.125, 0.5).shrink(0.25, 0.35, 0.45);
		
		ddColor.setAsShaderColor(0.75F);
		
		for(FishingSpot spot : fishingSpots.getFishingSpots())
		{
			Vec3d playerPos = spot.input().pos().subtract(region.toVec3d());
			Vec3d bobberPos = spot.bobberPos().subtract(region.toVec3d());
			
			matrixStack.push();
			matrixStack.translate(playerPos.x, playerPos.y, playerPos.z);
			
			matrixStack.push();
			matrixStack.multiply(spot.input().rotation().toQuaternion());
			
			RenderUtils.drawOutlinedBox(headBox, matrixStack);
			RenderUtils.drawOutlinedBox(noseBox, matrixStack);
			if(!spot.openWater())
				RenderUtils.drawCrossBox(headBox, matrixStack);
			
			matrixStack.pop();
			
			RenderUtils.drawArrow(Vec3d.ZERO, bobberPos.subtract(playerPos),
				matrixStack);
			
			matrixStack.pop();
		}
	}
	
	private void drawMcmmoRange(MatrixStack matrixStack, RegionPos region)
	{
		FishingSpot lastSpot = fishingSpots.getLastSpot();
		if(lastSpot == null)
			return;
		
		// only draw range during setup, or if debug draw is enabled
		if(fishingSpots.isSetupDone() && !debugDraw.isChecked())
			return;
		
		Vec3d bobberPos = lastSpot.bobberPos().subtract(region.toVec3d());
		
		matrixStack.push();
		matrixStack.translate(bobberPos.x, bobberPos.y, bobberPos.z);
		
		int mcmmoRange = fishingSpots.getRange();
		Box rangeBox =
			new Box(0, 0, 0, 0, 0, 0).expand(mcmmoRange, 1, mcmmoRange);
		RenderSystem.setShaderColor(1, 0, 0, 0.25F);
		RenderUtils.drawSolidBox(rangeBox, matrixStack);
		
		RenderSystem.setShaderColor(1, 0, 0, 0.5F);
		RenderUtils.drawOutlinedBox(rangeBox, matrixStack);
		RenderUtils.drawOutlinedBox(rangeBox.contract(0, 1, 0), matrixStack);
		
		matrixStack.pop();
	}
}
