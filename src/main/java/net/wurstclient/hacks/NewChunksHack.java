/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.SingleValuePalette;
import net.minecraft.world.level.material.FluidState;
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
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

public final class NewChunksHack extends Hack
	implements UpdateListener, RenderListener
{
	private final NewChunksStyleSetting style = new NewChunksStyleSetting();
	
	private final NewChunksShowSetting show = new NewChunksShowSetting();
	
	private final CheckboxSetting showReasons = new CheckboxSetting(
		"Show reasons",
		"Highlights the fluid blocks that caused chunks to be marked as new/old.\n\n"
			+ "Chunks that were marked based on biome or block palette have no block to highlight.",
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
	
	private final Set<ChunkPos> newChunks = new HashSet<>();
	private final Set<ChunkPos> oldChunks = new HashSet<>();
	private final Set<ChunkPos> notOldChunks = new HashSet<>();
	
	private final Set<BlockPos> newChunkReasons = new HashSet<>();
	private final Set<BlockPos> oldChunkReasons = new HashSet<>();
	
	private final NewChunksRenderer renderer = new NewChunksRenderer(altitude,
		opacity, newChunksColor, oldChunksColor);
	private final NewChunksReasonsRenderer reasonsRenderer =
		new NewChunksReasonsRenderer(drawDistance);
	
	private RegionPos lastRegion;
	
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
		
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE
			.register((client, level) -> reset());
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
		newChunks.clear();
		oldChunks.clear();
		notOldChunks.clear();
		newChunkReasons.clear();
		oldChunkReasons.clear();
		lastRegion = null;
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
			renderer.updateBuffer(0, chunkRenderer.getLayer(),
				buffer -> chunkRenderer.buildBuffer(buffer, newChunks, dd));
			
			if(showReasons.isChecked())
				renderer.updateBuffer(1, reasonsRenderer.getLayer(),
					buffer -> reasonsRenderer.buildBuffer(buffer,
						List.copyOf(newChunkReasons)));
		}
		
		if(showSetting.includesOld())
		{
			renderer.updateBuffer(2, chunkRenderer.getLayer(),
				buffer -> chunkRenderer.buildBuffer(buffer, oldChunks, dd));
			
			if(showReasons.isChecked())
				renderer.updateBuffer(3, reasonsRenderer.getLayer(),
					buffer -> reasonsRenderer.buildBuffer(buffer,
						List.copyOf(oldChunkReasons)));
		}
	}
	
	public void afterLoadChunk(LevelChunk chunk)
	{
		if(!isEnabled())
			return;
		
		ChunkPos chunkPos = chunk.getPos();
		if(newChunks.contains(chunkPos) || oldChunks.contains(chunkPos)
			|| notOldChunks.contains(chunkPos))
			return;
			
		// Chunk generation can leave unused local palette entries until a disk
		// round-trip.
		for(LevelChunkSection section : chunk.getSections())
			if(hasUnusedEntry(section.getBiomes(),
				biome -> biome.is(Biomes.PLAINS))
				|| hasUnusedEntry(section.getStates(),
					state -> state.is(Blocks.AIR)))
			{
				newChunks.add(chunkPos);
				if(logChunks.isChecked())
					System.out.println("new chunk (palette) at " + chunkPos);
				return;
			}
			
		// Fluids should always generate still, with flow starting later through
		// block updates. Therefore any chunk that contains flowing fluids in
		// the initial chunk data should be an old chunk.
		BlockPos reasonForOld = findFlowingFluid(chunk);
		
		if(reasonForOld == null)
		{
			notOldChunks.add(chunkPos);
			return;
		}
		
		oldChunks.add(chunkPos);
		oldChunkReasons.add(reasonForOld);
		if(logChunks.isChecked())
			System.out.println("old chunk (fluid) at " + chunkPos);
	}
	
	private static <T> boolean hasUnusedEntry(PalettedContainerRO<T> container,
		Predicate<T> predicate)
	{
		Palette<T> palette = ((PalettedContainer<T>)container).data.palette();
		
		// Global palettes describe the registry, not this section's history
		// Single-value palettes cannot have unused entries
		if(palette instanceof GlobalPalette<?>
			|| palette instanceof SingleValuePalette<?>
			|| !palette.maybeHas(predicate))
			return false;
		
		Set<T> usedEntries = new HashSet<>();
		container.count((entry, count) -> usedEntries.add(entry));
		return usedEntries.stream().noneMatch(predicate);
	}
	
	private BlockPos findFlowingFluid(LevelChunk chunk)
	{
		LevelChunkSection[] sections = chunk.getSections();
		for(int i = 0; i < sections.length; i++)
		{
			LevelChunkSection section = sections[i];
			
			// Skip checking every block if flowing fluid doesn't exist in the
			// block state palette.
			if(section.hasOnlyAir()
				|| !section.maybeHas(state -> isFlowing(state.getFluidState())))
				continue;
			
			for(int y = 0; y < 16; y++)
				for(int z = 0; z < 16; z++)
					for(int x = 0; x < 16; x++)
						if(isFlowing(section.getFluidState(x, y, z)))
							return new BlockPos(
								chunk.getPos().getMinBlockX() + x,
								chunk.getMinY() + i * 16 + y,
								chunk.getPos().getMinBlockZ() + z);
		}
		
		return null;
	}
	
	private boolean isFlowing(FluidState fluidState)
	{
		return !fluidState.isEmpty() && !fluidState.isSource();
	}
	
	public void afterUpdateBlock(BlockPos pos)
	{
		if(!isEnabled())
			return;
		
		// Liquid starts flowing -> probably a new chunk
		FluidState fluidState = BlockUtils.getState(pos).getFluidState();
		if(!isFlowing(fluidState))
			return;
		
		ChunkPos chunkPos = new ChunkPos(pos);
		if(newChunks.contains(chunkPos) || oldChunks.contains(chunkPos))
			return;
		
		newChunks.add(chunkPos);
		newChunkReasons.add(pos.immutable());
		if(logChunks.isChecked())
			System.out.println("new chunk (fluid) at " + chunkPos);
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		RegionPos region = RenderUtils.getCameraRegion();
		if(!region.equals(lastRegion))
		{
			onUpdate();
			lastRegion = region;
		}
		
		renderer.render(matrixStack, partialTicks);
	}
}
