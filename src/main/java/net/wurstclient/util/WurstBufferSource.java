/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.wurstclient.WurstClient;

/**
 * Simple wrapper around {@link StagedVertexBuffer} to replace Minecraft's
 * {@code MultiBufferSource} which was removed in 26.2-snapshot-5.
 */
public final class WurstBufferSource
{
	private final StagedVertexBuffer stagedBuffer = new StagedVertexBuffer(
		() -> "WurstBufferSource", RenderType.BIG_BUFFER_SIZE);
	private final List<StagedVertexBuffer.Draw> draws = new ArrayList<>();
	private final List<RenderType> drawTypes = new ArrayList<>();
	
	public VertexConsumer getBuffer(RenderType renderType)
	{
		if(!drawTypes.isEmpty() && drawTypes.getLast() == renderType
			&& renderType.canConsolidateConsecutiveGeometry())
			return stagedBuffer.getVertexBuilder(draws.getLast());
		
		StagedVertexBuffer.Draw draw =
			stagedBuffer.appendDraw(renderType.format(),
				renderType.primitiveTopology(), renderType.sortOnUpload()
					? RenderSystem.getProjectionType().vertexSorting() : null);
		
		draws.add(draw);
		drawTypes.add(renderType);
		return stagedBuffer.getVertexBuilder(draw);
	}
	
	public void uploadAndDraw()
	{
		try
		{
			if(draws.isEmpty())
				return;
			
			stagedBuffer.upload();
			RenderTarget renderTarget =
				WurstClient.MC.gameRenderer.mainRenderTarget();
			
			try(RenderPass renderPass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(() -> "WurstBufferSource",
					renderTarget.getColorTextureView(), Optional.empty(),
					renderTarget.getDepthTextureView(), OptionalDouble.empty()))
			{
				RenderSystem.bindDefaultUniforms(renderPass);
				
				for(int i = 0; i < draws.size(); i++)
					draw(drawTypes.get(i), draws.get(i), renderPass);
			}
			
			stagedBuffer.endDraw();
			
		}finally
		{
			draws.clear();
			drawTypes.clear();
			stagedBuffer.close();
		}
	}
	
	private void draw(RenderType type, StagedVertexBuffer.Draw draw,
		RenderPass renderPass)
	{
		StagedVertexBuffer.ExecuteInfo info = stagedBuffer.getExecuteInfo(draw);
		
		if(info != null)
			type.prepare().drawFromBuffer(info, renderPass);
	}
}
