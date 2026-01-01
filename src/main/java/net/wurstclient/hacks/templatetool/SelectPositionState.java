/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.wurstclient.hacks.TemplateToolHack;
import net.wurstclient.util.RenderUtils;

public abstract class SelectPositionState extends TemplateToolState
{
	private BlockPos crosshairBlock;
	
	@Override
	public final void onUpdate(TemplateToolHack hack)
	{
		crosshairBlock = getCrosshairBlock();
		
		if(MC.options.keyUse.isDown() && crosshairBlock != null)
		{
			setSelectedPos(hack, crosshairBlock);
			return;
		}
		
		if(isPressingEnter() && getSelectedPos(hack) != null)
			hack.setState(getNextState());
	}
	
	private BlockPos getCrosshairBlock()
	{
		if(!(MC.hitResult instanceof BlockHitResult bHitResult))
			return null;
		
		BlockPos pos = bHitResult.getBlockPos();
		if(MC.options.keyShift.isDown())
			pos = pos.relative(bHitResult.getDirection());
		
		return pos;
	}
	
	private boolean isPressingEnter()
	{
		return InputConstants.isKeyDown(MC.getWindow(), GLFW.GLFW_KEY_ENTER);
	}
	
	@Override
	public void onRender(TemplateToolHack hack, PoseStack matrixStack,
		float partialTicks)
	{
		if(crosshairBlock == null)
			return;
		
		AABB box = new AABB(crosshairBlock).deflate(1 / 16.0);
		int black = 0x80000000;
		int gray = 0x26404040;
		RenderUtils.drawOutlinedBox(matrixStack, box, black, false);
		RenderUtils.drawSolidBox(matrixStack, box, gray, false);
	}
	
	@Override
	protected final String getMessage(TemplateToolHack hack)
	{
		if(getSelectedPos(hack) != null)
			return "Press enter to confirm, or select a different position.";
		
		return getDefaultMessage();
	}
	
	protected abstract String getDefaultMessage();
	
	protected abstract BlockPos getSelectedPos(TemplateToolHack hack);
	
	protected abstract void setSelectedPos(TemplateToolHack hack, BlockPos pos);
	
	protected abstract TemplateToolState getNextState();
}
