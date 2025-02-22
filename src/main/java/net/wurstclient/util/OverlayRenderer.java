/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMinecraftClient;

public final class OverlayRenderer
{
	protected static final MinecraftClient MC = WurstClient.MC;
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
		progress = MC.interactionManager.currentBreakingProgress;
		
		if(progress < prevProgress)
			prevProgress = progress;
	}
	
	public void render(MatrixStack matrixStack, float partialTicks,
		BlockPos pos)
	{
		if(pos == null)
			return;
		
		// Reset progress if breaking a different block
		if(prevPos != null && !pos.equals(prevPos))
			resetProgress();
		
		prevPos = pos;
		
		// Get interpolated progress
		boolean breaksInstantly = MC.player.getAbilities().creativeMode
			|| BlockUtils.getHardness(pos) >= 1;
		float p = breaksInstantly ? 1
			: MathHelper.lerp(partialTicks, prevProgress, progress);
		
		// Get colors
		float red = p * 2F;
		float green = 2 - red;
		float[] rgb = {red, green, 0};
		int quadColor = RenderUtils.toIntColor(rgb, 0.25F);
		int lineColor = RenderUtils.toIntColor(rgb, 0.5F);
		
		// Set size
		Box box = new Box(pos);
		if(p < 1)
			box = box.contract((1 - p) * 0.5);
		
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}
}
