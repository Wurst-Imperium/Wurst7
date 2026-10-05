/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.noclip;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.hacks.NoClipHack;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.WurstBufferSource;

public enum NoClipRenderer
{
	;
	
	private static final Minecraft MC = WurstClient.MC;
	
	public static Set<JumpToRender> findJumpsNear(BlockPos center)
	{
		Set<JumpToRender> jumps = new LinkedHashSet<>();
		for(BlockPos pos : BlockUtils.getAllInBox(center, 3))
		{
			if(pos.distManhattan(center) > 3)
				continue;
			
			if(!MC.level.isLoaded(pos)
				|| !MC.level.getBlockState(pos).isSolidRender())
				continue;
			
			for(Direction direction : Direction.values())
			{
				JumpToRender jump = findJump(pos, direction);
				if(jump != null)
					jumps.add(jump);
			}
		}
		
		return jumps;
	}
	
	private static JumpToRender findJump(BlockPos entryPos, Direction direction)
	{
		BlockPos preEntryPos = entryPos.relative(direction.getOpposite());
		if(!MC.level.isLoaded(preEntryPos)
			|| MC.level.getBlockState(preEntryPos).isSolidRender())
			return null;
		
		for(int blocks = 1; blocks + 1 <= NoClipHack.MAX_DISTANCE; blocks++)
		{
			BlockPos exitPos = entryPos.relative(direction, blocks - 1);
			BlockPos postExitPos = exitPos.relative(direction);
			if(!MC.level.isLoaded(postExitPos))
				return null;
			if(MC.level.getBlockState(postExitPos).isSolidRender())
				continue;
			
			BlockPos minPos =
				direction.getAxisDirection() == AxisDirection.POSITIVE
					? entryPos : exitPos;
			return new JumpToRender(minPos, direction.getAxis(), blocks,
				blocks + 1 > NoClipHack.MAX_DISTANCE_WITHOUT_ELYTRA);
		}
		return null;
	}
	
	public static void renderJumps(PoseStack matrixStack,
		Collection<JumpToRender> jumps)
	{
		if(jumps.isEmpty())
			return;
		
		WurstBufferSource bs = new WurstBufferSource();
		VertexConsumer buffer = bs.getBuffer(WurstRenderLayers.ESP_LINES);
		Vec3 cameraPos = RenderUtils.getCameraPos();
		for(JumpToRender jump : jumps)
			drawJump(matrixStack, buffer, jump, cameraPos);
		bs.uploadAndDraw();
	}
	
	private static void drawJump(PoseStack matrixStack, VertexConsumer buffer,
		JumpToRender jump, Vec3 cameraPos)
	{
		int color = jump.needsElytra() ? 0x8000FFFF : 0x8000FF00;
		Axis normalAxis = jump.axis();
		Vec3 minFaceCenter = Vec3.atCenterOf(jump.minPos())
			.with(normalAxis, jump.minPos().get(normalAxis))
			.subtract(cameraPos);
		Vec3 maxFaceCenter = minFaceCenter.with(normalAxis,
			minFaceCenter.get(normalAxis) + jump.blocks());
		Vec3 halfWidth =
			(normalAxis == Axis.X ? Vec3.Z_AXIS : Vec3.X_AXIS).scale(7 / 16.0);
		Vec3 halfHeight =
			(normalAxis == Axis.Y ? Vec3.Z_AXIS : Vec3.Y_AXIS).scale(7 / 16.0);
		for(Vec3 center : new Vec3[]{minFaceCenter, maxFaceCenter})
		{
			Vec3 a = center.subtract(halfWidth).subtract(halfHeight);
			Vec3 b = center.add(halfWidth).subtract(halfHeight);
			Vec3 c = center.add(halfWidth).add(halfHeight);
			Vec3 d = center.subtract(halfWidth).add(halfHeight);
			RenderUtils.drawLine(matrixStack, buffer, a, b, color);
			RenderUtils.drawLine(matrixStack, buffer, b, c, color);
			RenderUtils.drawLine(matrixStack, buffer, c, d, color);
			RenderUtils.drawLine(matrixStack, buffer, d, a, color);
		}
	}
	
	public record JumpToRender(BlockPos minPos, Axis axis, int blocks,
		boolean needsElytra)
	{}
}
