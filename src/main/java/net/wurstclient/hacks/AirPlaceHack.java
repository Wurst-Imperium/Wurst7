/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.InteractionSimulator;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

@SearchTags({"air place"})
public final class AirPlaceHack extends Hack
	implements RightClickListener, UpdateListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting guide = new CheckboxSetting("Guide",
		"Shows a guide for where blocks will be placed.", true);
	
	private final ColorSetting guideColor = new ColorSetting("Guide color",
		"Color of the block placing guide, if enabled.", Color.RED);
	
	private BlockPos renderPos;
	
	public AirPlaceHack()
	{
		super("AirPlace");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(guide);
		addSetting(guideColor);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(RightClickListener.class, this);
		renderPos = null;
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		BlockHitResult hitResult = getHitResultIfMissed();
		if(hitResult == null)
			return;
		
		MC.itemUseCooldown = 4;
		if(MC.player.isRiding())
			return;
		
		InteractionSimulator.rightClickBlock(hitResult);
		event.cancel();
	}
	
	@Override
	public void onUpdate()
	{
		renderPos = null;
		
		if(!guide.isChecked())
			return;
		
		if(MC.player.getMainHandStack().isEmpty()
			&& MC.player.getOffHandStack().isEmpty())
			return;
		
		if(MC.player.isRiding())
			return;
		
		BlockHitResult hitResult = getHitResultIfMissed();
		if(hitResult != null)
			renderPos = hitResult.getBlockPos();
	}
	
	private BlockHitResult getHitResultIfMissed()
	{
		HitResult hitResult = MC.player.raycast(range.getValue(), 0, false);
		if(hitResult.getType() != HitResult.Type.MISS)
			return null;
		
		if(!(hitResult instanceof BlockHitResult blockHitResult))
			return null;
		
		return blockHitResult;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(renderPos == null)
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		Box box = new Box(renderPos.subtract(region.toBlockPos()));
		float[] colorF = guideColor.getColorF();
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.1F);
		RenderUtils.drawSolidBox(box, matrixStack);
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.75F);
		RenderUtils.drawOutlinedBox(box, matrixStack);
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
