/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;

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
import net.wurstclient.util.RenderUtils;

@SearchTags({"air place"})
public final class AirPlaceHack extends Hack
	implements RightClickListener, UpdateListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting guide = new CheckboxSetting("Guide",
		"description.wurst.setting.airplace.guide", true);
	
	private final ColorSetting guideColor = new ColorSetting("Guide color",
		"description.wurst.setting.airplace.guide_color", Color.RED);
	
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
		
		Box box = new Box(renderPos);
		
		int quadColor = guideColor.getColorI(0x1A);
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		
		int lineColor = guideColor.getColorI(0xC0);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}
}
