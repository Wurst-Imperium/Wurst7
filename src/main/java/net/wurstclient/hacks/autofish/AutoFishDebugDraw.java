/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import java.awt.Color;
import java.util.stream.Stream;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

public final class AutoFishDebugDraw
{
	private static final MinecraftClient MC = WurstClient.MC;
	
	private final CheckboxSetting debugDraw = new CheckboxSetting("Debug draw",
		"Shows where bites are occurring and where they will be detected."
			+ " Useful for optimizing your 'Valid range' setting.",
		false);
	
	private final ColorSetting ddColor = new ColorSetting("DD color",
		"Color of the debug draw, if enabled.", Color.RED);
	
	private final SliderSetting validRange;
	private final FishingSpotManager fishingSpots;
	private Vec3d lastSoundPos;
	
	public AutoFishDebugDraw(SliderSetting validRange,
		FishingSpotManager fishingSpots)
	{
		this.validRange = validRange;
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
	
	public void render(MatrixStack matrices, float partialTicks)
	{
		if(!debugDraw.isChecked() && !fishingSpots.isMcmmoMode())
			return;
		
		RenderSystem.depthFunc(GlConst.GL_ALWAYS);
		
		VertexConsumerProvider.Immediate vcp =
			MC.getBufferBuilders().getEntityVertexConsumers();
		
		matrices.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrices, region);
		
		if(debugDraw.isChecked())
		{
			FishingBobberEntity bobber = MC.player.fishHook;
			if(bobber != null)
				drawValidRange(matrices, vcp, partialTicks, bobber, region);
			
			if(lastSoundPos != null)
				drawLastBite(matrices, vcp, region);
			
			drawFishingSpots(matrices, vcp, region);
		}
		
		if(fishingSpots.isMcmmoMode())
			drawMcmmoRange(matrices, vcp, region);
		
		matrices.pop();
		
		vcp.draw(WurstRenderLayers.ESP_QUADS);
		vcp.draw(WurstRenderLayers.ESP_LINES);
	}
	
	private void drawValidRange(MatrixStack matrices,
		VertexConsumerProvider vcp, float partialTicks,
		FishingBobberEntity bobber, RegionPos region)
	{
		double vr = validRange.getValue();
		Vec3d pos = EntityUtils.getLerpedPos(bobber, partialTicks)
			.subtract(region.toVec3d());
		Box vrBox = new Box(-vr, -1 / 16.0, -vr, vr, 1 / 16.0, vr).offset(pos);
		
		RenderUtils.drawOutlinedBox(matrices,
			vcp.getBuffer(WurstRenderLayers.ESP_LINES), vrBox,
			ddColor.getColorI(0x80));
	}
	
	private void drawLastBite(MatrixStack matrixStack,
		VertexConsumerProvider vcp, RegionPos region)
	{
		Vec3d pos = lastSoundPos.subtract(region.toVec3d());
		int color = ddColor.getColorI(0x80);
		VertexConsumer buffer = vcp.getBuffer(WurstRenderLayers.ESP_LINES);
		
		RenderUtils.drawLine(matrixStack, buffer, pos.add(-0.125, 0, -0.125),
			pos.add(0.125, 0, 0.125), color);
		RenderUtils.drawLine(matrixStack, buffer, pos.add(0.125, 0, -0.125),
			pos.add(-0.125, 0, 0.125), color);
	}
	
	private void drawFishingSpots(MatrixStack matrices,
		VertexConsumerProvider vcp, RegionPos region)
	{
		Box headBox = new Box(-0.25, 0, -0.25, 0.25, 0.5, 0.25);
		Box noseBox =
			headBox.offset(0.125, 0.125, 0.5).shrink(0.25, 0.35, 0.45);
		
		int color = ddColor.getColorI(0xC0);
		VertexConsumer linesBuffer = vcp.getBuffer(WurstRenderLayers.ESP_LINES);
		
		for(FishingSpot spot : fishingSpots.getFishingSpots())
		{
			Vec3d playerPos = spot.input().pos().subtract(region.toVec3d());
			Vec3d bobberPos = spot.bobberPos().subtract(region.toVec3d());
			
			matrices.push();
			matrices.translate(playerPos.x, playerPos.y, playerPos.z);
			matrices.multiply(spot.input().rotation().toQuaternion());
			
			RenderUtils.drawOutlinedBox(matrices, linesBuffer, headBox, color);
			RenderUtils.drawOutlinedBox(matrices, linesBuffer, noseBox, color);
			if(!spot.openWater())
				RenderUtils.drawCrossBox(matrices, linesBuffer, headBox, color);
			
			matrices.pop();
			
			RenderUtils.drawArrow(matrices, linesBuffer, playerPos, bobberPos,
				color, 0.1F);
		}
	}
	
	private void drawMcmmoRange(MatrixStack matrices,
		VertexConsumerProvider vcp, RegionPos region)
	{
		FishingSpot lastSpot = fishingSpots.getLastSpot();
		if(lastSpot == null)
			return;
		
		// only draw range during setup, or if debug draw is enabled
		if(fishingSpots.isSetupDone() && !debugDraw.isChecked())
			return;
		
		int mcmmoRange = fishingSpots.getRange();
		Vec3d bobberPos = lastSpot.bobberPos().subtract(region.toVec3d());
		Box rangeBox = new Box(0, 0, 0, 0, 0, 0)
			.expand(mcmmoRange, 1, mcmmoRange).offset(bobberPos);
		
		int quadsColor = 0x40FF0000;
		VertexConsumer quadsBuffer = vcp.getBuffer(WurstRenderLayers.ESP_QUADS);
		RenderUtils.drawSolidBox(matrices, quadsBuffer, rangeBox, quadsColor);
		
		int linesColor = 0x80FF0000;
		VertexConsumer linesBuffer = vcp.getBuffer(WurstRenderLayers.ESP_LINES);
		RenderUtils.drawOutlinedBox(matrices, linesBuffer, rangeBox,
			linesColor);
		RenderUtils.drawOutlinedBox(matrices, linesBuffer,
			rangeBox.contract(0, 1, 0), linesColor);
	}
}
