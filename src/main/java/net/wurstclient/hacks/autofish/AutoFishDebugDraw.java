/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import java.awt.Color;
import java.util.stream.Stream;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;

public final class AutoFishDebugDraw
{
	private static final Minecraft MC = WurstClient.MC;
	
	private final CheckboxSetting debugDraw = new CheckboxSetting("Debug draw",
		"Shows where bites are occurring and where they will be detected."
			+ " Useful for optimizing your 'Valid range' setting.",
		false);
	
	private final ColorSetting ddColor = new ColorSetting("DD color",
		"Color of the debug draw, if enabled.", Color.RED);
	
	private final SliderSetting validRange;
	private final FishingSpotManager fishingSpots;
	private Vec3 lastSoundPos;
	
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
	
	public void updateSoundPos(ClientboundSoundPacket sound)
	{
		lastSoundPos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
	}
	
	public void render(PoseStack matrices, float partialTicks)
	{
		if(!debugDraw.isChecked() && !fishingSpots.isMcmmoMode())
			return;
		
		if(debugDraw.isChecked())
		{
			FishingHook bobber = MC.player.fishing;
			if(bobber != null)
				drawValidRange(matrices, partialTicks, bobber);
			
			if(lastSoundPos != null)
				drawLastBite(matrices);
			
			drawFishingSpots(matrices);
		}
		
		if(fishingSpots.isMcmmoMode())
			drawMcmmoRange(matrices);
	}
	
	private void drawValidRange(PoseStack matrices, float partialTicks,
		FishingHook bobber)
	{
		double vr = validRange.getValue();
		Vec3 pos = EntityUtils.getLerpedPos(bobber, partialTicks);
		AABB vrBox = new AABB(-vr, -1 / 16.0, -vr, vr, 1 / 16.0, vr).move(pos);
		
		RenderUtils.drawOutlinedBox(matrices, vrBox, ddColor.getColorI(0x80),
			false);
	}
	
	private void drawLastBite(PoseStack matrixStack)
	{
		Vec3 pos = lastSoundPos;
		int color = ddColor.getColorI(0x80);
		
		RenderUtils.drawLine(matrixStack, pos.add(-0.125, 0, -0.125),
			pos.add(0.125, 0, 0.125), color, false);
		RenderUtils.drawLine(matrixStack, pos.add(0.125, 0, -0.125),
			pos.add(-0.125, 0, 0.125), color, false);
	}
	
	private void drawFishingSpots(PoseStack matrices)
	{
		AABB headBox = new AABB(-0.25, 0, -0.25, 0.25, 0.5, 0.25);
		AABB noseBox =
			headBox.move(0.125, 0.125, 0.5).contract(0.25, 0.35, 0.45);
		
		int color = ddColor.getColorI(0xC0);
		
		MultiBufferSource.BufferSource vcp = RenderUtils.getVCP();
		Vec3 camPos = RenderUtils.getCameraPos();
		
		for(FishingSpot spot : fishingSpots.getFishingSpots())
		{
			Vec3 playerPos = spot.input().pos();
			Vec3 bobberPos = spot.bobberPos();
			
			matrices.pushPose();
			matrices.translate(playerPos.x - camPos.x, playerPos.y - camPos.y,
				playerPos.z - camPos.z);
			matrices.mulPose(spot.input().rotation().toQuaternion());
			
			VertexConsumer lineBuffer =
				vcp.getBuffer(WurstRenderLayers.ESP_LINES);
			
			RenderUtils.drawOutlinedBox(matrices, lineBuffer, headBox, color);
			RenderUtils.drawOutlinedBox(matrices, lineBuffer, noseBox, color);
			if(!spot.openWater())
				RenderUtils.drawCrossBox(matrices, lineBuffer, headBox, color);
			
			matrices.popPose();
			
			RenderUtils.drawArrow(matrices, lineBuffer,
				playerPos.subtract(camPos), bobberPos.subtract(camPos), color,
				0.1F);
			
			vcp.endBatch(WurstRenderLayers.ESP_LINES);
		}
	}
	
	private void drawMcmmoRange(PoseStack matrices)
	{
		FishingSpot lastSpot = fishingSpots.getLastSpot();
		if(lastSpot == null)
			return;
		
		// only draw range during setup, or if debug draw is enabled
		if(fishingSpots.isSetupDone() && !debugDraw.isChecked())
			return;
		
		int mcmmoRange = fishingSpots.getRange();
		Vec3 bobberPos = lastSpot.bobberPos();
		AABB rangeBox = new AABB(0, 0, 0, 0, 0, 0)
			.inflate(mcmmoRange, 1, mcmmoRange).move(bobberPos);
		
		int quadsColor = 0x40FF0000;
		RenderUtils.drawSolidBox(matrices, rangeBox, quadsColor, false);
		
		int linesColor = 0x80FF0000;
		RenderUtils.drawOutlinedBox(matrices, rangeBox, linesColor, false);
		RenderUtils.drawOutlinedBox(matrices, rangeBox.deflate(0, 1, 0),
			linesColor, false);
	}
}
