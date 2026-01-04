/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMinecraftClient;

public final class OverlayRenderer
{
	protected static final Minecraft MC = WurstClient.MC;
	protected static final IMinecraftClient IMC = WurstClient.IMC;
	
	private float progress;
	private float prevProgress;
	private BlockPos prevPos;
	
	public void resetProgress()
	{
		progress = 0;
		prevProgress = 0;
		prevPos = null;
	}
	
	public void updateProgress()
	{
		prevProgress = progress;
		progress = MC.gameMode.destroyProgress;
		
		if(progress < prevProgress)
			prevProgress = progress;
	}
	
	public void render(PoseStack matrixStack, float partialTicks, BlockPos pos)
	{
		if(pos == null)
			return;
		
		// Reset progress if breaking a different block
		if(prevPos != null && !pos.equals(prevPos))
			resetProgress();
		
		prevPos = pos;
		
		// Get interpolated progress
		boolean breaksInstantly = MC.player.getAbilities().instabuild
			|| BlockUtils.getHardness(pos) >= 1;
		float p = breaksInstantly ? 1
			: Mth.lerp(partialTicks, prevProgress, progress);
		
		// Get colors
		float red = p * 2F;
		float green = 2 - red;
		float[] rgb = {red, green, 0};
		int quadColor = RenderUtils.toIntColor(rgb, 0.25F);
		int lineColor = RenderUtils.toIntColor(rgb, 0.5F);
		
		// Set size
		AABB box = new AABB(pos);
		if(p < 1)
			box = box.deflate((1 - p) * 0.5);
		
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}
}
