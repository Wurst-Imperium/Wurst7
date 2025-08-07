/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.templatetool.TemplateToolState;
import net.wurstclient.hacks.templatetool.states.SelectBoxStartState;
import net.wurstclient.util.RenderUtils;

public final class TemplateToolHack extends Hack
	implements UpdateListener, RenderListener, GUIRenderListener
{
	private TemplateToolState state;
	private BlockPos startPos;
	private BlockPos endPos;
	private BlockPos originPos;
	private final LinkedHashMap<BlockPos, BlockState> nonEmptyBlocks =
		new LinkedHashMap<>();
	private final LinkedHashSet<BlockPos> sortedBlocks = new LinkedHashSet<>();
	private boolean blockTypesEnabled;
	private File file;
	
	public TemplateToolHack()
	{
		super("TemplateTool");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().autoBuildHack.setEnabled(false);
		WURST.getHax().instaBuildHack.setEnabled(false);
		WURST.getHax().bowAimbotHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		
		setState(new SelectBoxStartState());
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(GUIRenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(GUIRenderListener.class, this);
		
		setState(null);
		startPos = null;
		endPos = null;
		originPos = null;
		nonEmptyBlocks.clear();
		sortedBlocks.clear();
		blockTypesEnabled = false;
		file = null;
	}
	
	@Override
	public void onUpdate()
	{
		if(state != null)
			state.onUpdate(this);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		int black = 0x80000000;
		int green15 = 0x2600FF00;
		
		// Draw template bounds
		if(startPos != null && endPos != null)
		{
			Box bounds = Box.enclosing(startPos, endPos).contract(1 / 16.0);
			RenderUtils.drawOutlinedBox(matrixStack, bounds, black, true);
		}
		
		// Draw origin
		if(originPos != null)
		{
			Box box = new Box(originPos).contract(1 / 16.0);
			RenderUtils.drawOutlinedBox(matrixStack, box, black, false);
			RenderUtils.drawSolidBox(matrixStack, box, green15, false);
		}
		
		// Draw state-specific things
		if(state != null)
			state.onRender(this, matrixStack, partialTicks);
	}
	
	@Override
	public void onRenderGUI(DrawContext context, float partialTicks)
	{
		if(state != null)
			state.onRenderGUI(this, context, partialTicks);
	}
	
	public void setState(TemplateToolState state)
	{
		if(this.state != null)
			this.state.onExit(this);
		
		this.state = state;
		
		if(state != null)
			state.onEnter(this);
	}
	
	public BlockPos getStartPos()
	{
		return startPos;
	}
	
	public void setStartPos(BlockPos pos)
	{
		startPos = pos;
	}
	
	public BlockPos getEndPos()
	{
		return endPos;
	}
	
	public void setEndPos(BlockPos pos)
	{
		endPos = pos;
	}
	
	public BlockPos getOriginPos()
	{
		return originPos;
	}
	
	public void setOriginPos(BlockPos pos)
	{
		originPos = pos;
	}
	
	public LinkedHashMap<BlockPos, BlockState> getNonEmptyBlocks()
	{
		return nonEmptyBlocks;
	}
	
	public LinkedHashSet<BlockPos> getSortedBlocks()
	{
		return sortedBlocks;
	}
	
	public boolean areBlockTypesEnabled()
	{
		return blockTypesEnabled;
	}
	
	public void setBlockTypesEnabled(boolean blockTypesEnabled)
	{
		this.blockTypesEnabled = blockTypesEnabled;
	}
	
	public File getFile()
	{
		return file;
	}
	
	public void setFile(File file)
	{
		this.file = file;
	}
}
