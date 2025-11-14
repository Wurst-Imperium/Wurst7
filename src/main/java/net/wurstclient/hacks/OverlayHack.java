/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.OverlayRenderer;

public final class OverlayHack extends Hack
	implements UpdateListener, RenderListener
{
	private final OverlayRenderer renderer = new OverlayRenderer();
	
	public OverlayHack()
	{
		super("Overlay");
		setCategory(Category.RENDER);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		renderer.resetProgress();
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.gameMode.isDestroying())
			renderer.updateProgress();
		else
			renderer.resetProgress();
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(!MC.gameMode.isDestroying())
			return;
		
		if(!(MC.hitResult instanceof BlockHitResult blockHitResult)
			|| blockHitResult.getType() != HitResult.Type.BLOCK)
			return;
		
		renderer.render(matrixStack, partialTicks,
			blockHitResult.getBlockPos());
	}
}
