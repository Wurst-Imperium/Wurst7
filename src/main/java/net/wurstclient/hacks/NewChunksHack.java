/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.newchunks.NewChunksChunkRenderer;
import net.wurstclient.hacks.newchunks.NewChunksReasonsRenderer;
import net.wurstclient.hacks.newchunks.NewChunksRenderer;
import net.wurstclient.hacks.newchunks.NewChunksShowSetting;
import net.wurstclient.hacks.newchunks.NewChunksShowSetting.Show;
import net.wurstclient.hacks.newchunks.NewChunksStyleSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChunkUtils;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

public final class NewChunksHack extends Hack
	implements UpdateListener, RenderListener
{
	private final NewChunksStyleSetting style = new NewChunksStyleSetting();
	
	private final NewChunksShowSetting show = new NewChunksShowSetting();
	
	private final CheckboxSetting showReasons = new CheckboxSetting(
		"Show reasons",
		"Highlights the block that caused each chunk to be marked as new/old.",
		false);
	
	private final CheckboxSetting showCounter =
		new CheckboxSetting("Show counter",
			"Shows the number of new/old chunks found so far.", false);
	
	private final SliderSetting altitude =
		new SliderSetting("Altitude", 0, -64, 320, 1, ValueDisplay.INTEGER);
	
	private final SliderSetting drawDistance =
		new SliderSetting("Draw distance", 32, 8, 64, 1, ValueDisplay.INTEGER);
	
	private final SliderSetting opacity = new SliderSetting("Opacity", 0.75,
		0.1, 1, 0.01, ValueDisplay.PERCENTAGE);
	
	private final ColorSetting newChunksColor =
		new ColorSetting("New chunks color", Color.RED);
	
	private final ColorSetting oldChunksColor =
		new ColorSetting("Old chunks color", Color.BLUE);
	
	private final CheckboxSetting logChunks = new CheckboxSetting("Log chunks",
		"Writes to the log file when a new/old chunk is found.", false);
	
	private final Set<ChunkPos> newChunks =
		Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> oldChunks =
		Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> dontCheckAgain =
		Collections.synchronizedSet(new HashSet<>());
	
	private final Set<BlockPos> newChunkReasons =
		Collections.synchronizedSet(new HashSet<>());
	private final Set<BlockPos> oldChunkReasons =
		Collections.synchronizedSet(new HashSet<>());
	
	private final NewChunksRenderer renderer = new NewChunksRenderer(altitude,
		opacity, newChunksColor, oldChunksColor);
	private final NewChunksReasonsRenderer reasonsRenderer =
		new NewChunksReasonsRenderer(drawDistance);
	
	private RegionPos lastRegion;
	private DimensionType lastDimension;
	
	public NewChunksHack()
	{
		super("NewChunks");
		setCategory(Category.RENDER);
		addSetting(style);
		addSetting(show);
		addSetting(showReasons);
		addSetting(showCounter);
		addSetting(altitude);
		addSetting(drawDistance);
		addSetting(opacity);
		addSetting(newChunksColor);
		addSetting(oldChunksColor);
		addSetting(logChunks);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		reset();
	}
	
	private void reset()
	{
		oldChunks.clear();
		newChunks.clear();
		dontCheckAgain.clear();
		oldChunkReasons.clear();
		newChunkReasons.clear();
		lastRegion = null;
		lastDimension = MC.world.getDimension();
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		renderer.closeBuffers();
	}
	
	@Override
	public String getRenderName()
	{
		if(!showCounter.isChecked())
			return getName();
		
		return String.format("%s [%d/%d]", getName(), newChunks.size(),
			oldChunks.size());
	}
	
	@Override
	public void onUpdate()
	{
		renderer.closeBuffers();
		
		Show showSetting = show.getSelected();
		int dd = drawDistance.getValueI();
		NewChunksChunkRenderer chunkRenderer =
			style.getSelected().getChunkRenderer();
		
		if(showSetting.includesNew())
		{
			renderer.updateBuffer(0, chunkRenderer.buildBuffer(newChunks, dd));
			
			if(showReasons.isChecked())
				renderer.updateBuffer(1,
					reasonsRenderer.buildBuffer(newChunkReasons));
		}
		
		if(showSetting.includesOld())
		{
			renderer.updateBuffer(2, chunkRenderer.buildBuffer(oldChunks, dd));
			
			if(showReasons.isChecked())
				renderer.updateBuffer(3,
					reasonsRenderer.buildBuffer(oldChunkReasons));
		}
	}
	
	public void afterLoadChunk(int x, int z)
	{
		if(!isEnabled())
			return;
		
		WorldChunk chunk = MC.world.getChunk(x, z);
		new Thread(() -> checkLoadedChunk(chunk), "NewChunks " + chunk.getPos())
			.start();
	}
	
	private void checkLoadedChunk(WorldChunk chunk)
	{
		ChunkPos chunkPos = chunk.getPos();
		if(newChunks.contains(chunkPos) || oldChunks.contains(chunkPos)
			|| dontCheckAgain.contains(chunkPos))
			return;
		
		int minX = chunkPos.getStartX();
		int minY = chunk.getBottomY();
		int minZ = chunkPos.getStartZ();
		int maxX = chunkPos.getEndX();
		int maxY = ChunkUtils.getHighestNonEmptySectionYOffset(chunk) + 16;
		int maxZ = chunkPos.getEndZ();
		
		for(int x = minX; x <= maxX; x++)
			for(int y = minY; y <= maxY; y++)
				for(int z = minZ; z <= maxZ; z++)
				{
					BlockPos pos = new BlockPos(x, y, z);
					FluidState fluidState = chunk.getFluidState(pos);
					
					if(fluidState.isEmpty() || fluidState.isStill())
						continue;
						
					// Liquid always generates still, the flowing happens later
					// through block updates. Therefore any chunk that contains
					// flowing liquids from the start should be an old chunk.
					oldChunks.add(chunkPos);
					oldChunkReasons.add(pos);
					if(logChunks.isChecked())
						System.out.println("old chunk at " + chunkPos);
					return;
				}
				
		// If the whole loop ran through without finding anything, make sure it
		// never runs again on that chunk, as that would be a huge waste of CPU
		// time.
		dontCheckAgain.add(chunkPos);
	}
	
	public void afterUpdateBlock(BlockPos pos)
	{
		if(!isEnabled())
			return;
		
		// Liquid starts flowing -> probably a new chunk
		FluidState fluidState = BlockUtils.getState(pos).getFluidState();
		if(fluidState.isEmpty() || fluidState.isStill())
			return;
		
		ChunkPos chunkPos = new ChunkPos(pos);
		if(newChunks.contains(chunkPos) || oldChunks.contains(chunkPos))
			return;
		
		newChunks.add(chunkPos);
		newChunkReasons.add(pos);
		if(logChunks.isChecked())
			System.out.println("new chunk at " + chunkPos);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(MC.world.getDimension() != lastDimension)
			reset();
		
		RegionPos region = RenderUtils.getCameraRegion();
		if(!region.equals(lastRegion))
		{
			onUpdate();
			lastRegion = region;
		}
		
		renderer.render(matrixStack, partialTicks);
	}
}
