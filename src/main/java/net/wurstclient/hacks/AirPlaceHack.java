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
	
	private final ColorSetting color = new ColorSetting("Color",
		"Block placing guide will be highlighted in this color.", Color.RED);
	
	private final CheckboxSetting guide = new CheckboxSetting("Guide",
		"Shows a guide for where blocks will place", true);
	
	private BlockPos bp;
	
	public AirPlaceHack()
	{
		super("AirPlace");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(color);
		addSetting(guide);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(RightClickListener.class, this);
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
		HitResult hitResult = MC.player.raycast(range.getValue(), 0, false);
		if(hitResult.getType() != HitResult.Type.MISS)
			return;
		
		if(!(hitResult instanceof BlockHitResult blockHitResult))
			return;
		
		MC.itemUseCooldown = 4;
		if(MC.player.isRiding())
			return;
		
		InteractionSimulator.rightClickBlock(blockHitResult);
		event.cancel();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(guide.isChecked() && bp != null)
		{
			// GL settings
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			
			matrixStack.push();
			
			RegionPos region = RenderUtils.getCameraRegion();
			RenderUtils.applyRegionalRenderOffset(matrixStack, region);
			
			renderBoxes(matrixStack, partialTicks, region);
			
			matrixStack.pop();
			
			// GL resets
			RenderSystem.setShaderColor(1, 1, 1, 1);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDisable(GL11.GL_BLEND);
		}
	}
	
	private void renderBoxes(MatrixStack matrixStack, float partialTicks,
		RegionPos region)
	{
		matrixStack.push();
		
		float[] colorF = color.getColorF();
		matrixStack.translate(bp.getX(), bp.getY(), bp.getZ());
		
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.2F);
		
		Box bb = new Box(0, 0, 0, 1, 1, 1);
		
		RenderUtils.drawSolidBox(bb, matrixStack);
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 1F);
		RenderUtils.drawOutlinedBox(bb, matrixStack);
		
		matrixStack.pop();
	}
	
	@Override
	public void onUpdate()
	{
		HitResult hitResult = MC.player.raycast(range.getValue(), 0, false);
		if(hitResult.getType() != HitResult.Type.MISS)
		{
			bp = null; // dont draw if looking at non-airplace location like
						// ground
			return;
		}
		
		if(!(hitResult instanceof BlockHitResult blockHitResult))
			return;
		
		bp = blockHitResult.getBlockPos();
	}
}
