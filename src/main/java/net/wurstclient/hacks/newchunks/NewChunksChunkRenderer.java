/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.newchunks;

import java.util.Set;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.ChunkPos;

public interface NewChunksChunkRenderer
{
	public void buildBuffer(VertexConsumer buffer, Set<ChunkPos> chunks,
		int drawDistance);
	
	public RenderType getLayer();
}
