/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.minecraft.block.Blocks;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockVertexCompiler;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.EasyVertexBuffer;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.chunk.ChunkSearcher;
import net.wurstclient.util.chunk.ChunkSearcherCoordinator;

@SearchTags({"cave finder"})
public final class CaveFinderHack extends Hack
	implements UpdateListener, RenderListener
{
	private final ChunkAreaSetting area = new ChunkAreaSetting("Area",
		"The area around the player to search in.\n"
			+ "Higher values require a faster computer.");
	
	private final SliderSetting limit = new SliderSetting("Limit",
		"The maximum number of blocks to display.\n"
			+ "Higher values require a faster computer.",
		5, 3, 6, 1, ValueDisplay.LOGARITHMIC);
	
	private final ColorSetting color = new ColorSetting("Color",
		"Caves will be highlighted in this color.", Color.RED);
	
	private final SliderSetting opacity = new SliderSetting("Opacity",
		"How opaque the highlights should be.\n" + "0 = breathing animation", 0,
		0, 1, 0.01, ValueDisplay.PERCENTAGE.withLabel(0, "breathing"));
	
	private int prevLimit;
	private boolean notify;
	
	private final ChunkSearcherCoordinator coordinator =
		new ChunkSearcherCoordinator(
			(pos, state) -> state.getBlock() == Blocks.CAVE_AIR, area);
	
	private ForkJoinPool forkJoinPool;
	private ForkJoinTask<HashSet<BlockPos>> getMatchingBlocksTask;
	private ForkJoinTask<ArrayList<int[]>> compileVerticesTask;
	
	private EasyVertexBuffer vertexBuffer;
	private RegionPos bufferRegion;
	private boolean bufferUpToDate;
	
	public CaveFinderHack()
	{
		super("CaveFinder");
		setCategory(Category.RENDER);
		addSetting(area);
		addSetting(limit);
		addSetting(color);
		addSetting(opacity);
	}
	
	@Override
	protected void onEnable()
	{
		prevLimit = limit.getValueI();
		notify = true;
		
		forkJoinPool = new ForkJoinPool();
		
		bufferUpToDate = false;
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, coordinator);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, coordinator);
		EVENTS.remove(RenderListener.class, this);
		
		stopBuildingBuffer();
		coordinator.reset();
		forkJoinPool.shutdownNow();
		
		if(vertexBuffer != null)
			vertexBuffer.close();
		vertexBuffer = null;
		bufferRegion = null;
	}
	
	@Override
	public void onUpdate()
	{
		boolean searchersChanged = coordinator.update();
		
		if(searchersChanged)
			stopBuildingBuffer();
		
		if(!coordinator.isDone())
			return;
		
		// check if limit has changed
		if(limit.getValueI() != prevLimit)
		{
			stopBuildingBuffer();
			prevLimit = limit.getValueI();
			notify = true;
		}
		
		// build the buffer
		
		if(getMatchingBlocksTask == null)
			startGetMatchingBlocksTask();
		
		if(!getMatchingBlocksTask.isDone())
			return;
		
		if(compileVerticesTask == null)
			startCompileVerticesTask();
		
		if(!compileVerticesTask.isDone())
			return;
		
		if(!bufferUpToDate)
			setBufferFromTask();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(vertexBuffer == null || bufferRegion == null)
			return;
		
		float x = System.currentTimeMillis() % 2000 / 1000F;
		float alpha = 0.25F + 0.25F * MathHelper.sin(x * MathHelper.PI);
		if(opacity.getValue() > 0)
			alpha = opacity.getValueF();
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack, bufferRegion);
		
		vertexBuffer.draw(matrixStack, WurstRenderLayers.ESP_QUADS,
			color.getColorF(), alpha);
		
		matrixStack.pop();
	}
	
	private void stopBuildingBuffer()
	{
		if(getMatchingBlocksTask != null)
		{
			getMatchingBlocksTask.cancel(true);
			getMatchingBlocksTask = null;
		}
		
		if(compileVerticesTask != null)
		{
			compileVerticesTask.cancel(true);
			compileVerticesTask = null;
		}
		
		bufferUpToDate = false;
	}
	
	private void startGetMatchingBlocksTask()
	{
		BlockPos eyesPos = BlockPos.ofFloored(RotationUtils.getEyesPos());
		Comparator<BlockPos> comparator =
			Comparator.comparingInt(pos -> eyesPos.getManhattanDistance(pos));
		
		getMatchingBlocksTask = forkJoinPool.submit(() -> coordinator
			.getMatches().parallel().map(ChunkSearcher.Result::pos)
			.sorted(comparator).limit(limit.getValueLog())
			.collect(Collectors.toCollection(HashSet::new)));
	}
	
	private void startCompileVerticesTask()
	{
		HashSet<BlockPos> matchingBlocks = getMatchingBlocksTask.join();
		
		if(matchingBlocks.size() < limit.getValueLog())
			notify = true;
		else if(notify)
		{
			ChatUtils.warning("CaveFinder found \u00a7lA LOT\u00a7r of blocks!"
				+ " To prevent lag, it will only show the closest \u00a76"
				+ limit.getValueString() + "\u00a7r results.");
			notify = false;
		}
		
		compileVerticesTask = forkJoinPool
			.submit(() -> BlockVertexCompiler.compile(matchingBlocks));
	}
	
	private void setBufferFromTask()
	{
		ArrayList<int[]> vertices = compileVerticesTask.join();
		RegionPos region = RenderUtils.getCameraRegion();
		
		if(vertexBuffer != null)
			vertexBuffer.close();
		
		vertexBuffer = EasyVertexBuffer.createAndUpload(DrawMode.QUADS,
			VertexFormats.POSITION_COLOR, buffer -> {
				for(int[] vertex : vertices)
					buffer.vertex(vertex[0] - region.x(), vertex[1],
						vertex[2] - region.z()).color(0xFFFFFFFF);
			});
		
		bufferUpToDate = true;
		bufferRegion = region;
	}
}
