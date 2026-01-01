/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.ChunkAreaSetting;
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

@SearchTags({"BlockESP", "block esp"})
public final class SearchHack extends Hack
	implements UpdateListener, RenderListener
{
	private final BlockSetting block = new BlockSetting("Block",
		"The type of block to search for.", "minecraft:diamond_ore", false);
	private Block lastBlock;
	
	private final ChunkAreaSetting area = new ChunkAreaSetting("Area",
		"The area around the player to search in.\n"
			+ "Higher values require a faster computer.");
	
	private final SliderSetting limit = new SliderSetting("Limit",
		"The maximum number of blocks to display.\n"
			+ "Higher values require a faster computer.",
		4, 3, 6, 1, ValueDisplay.LOGARITHMIC);
	private int prevLimit;
	private boolean notify;
	
	private final ChunkSearcherCoordinator coordinator =
		new ChunkSearcherCoordinator(area);
	
	private ForkJoinPool forkJoinPool;
	private ForkJoinTask<HashSet<BlockPos>> getMatchingBlocksTask;
	private ForkJoinTask<ArrayList<int[]>> compileVerticesTask;
	
	private EasyVertexBuffer vertexBuffer;
	private RegionPos bufferRegion;
	private boolean bufferUpToDate;
	
	public SearchHack()
	{
		super("Search");
		setCategory(Category.RENDER);
		addSetting(block);
		addSetting(area);
		addSetting(limit);
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + block.getBlockName().replace("minecraft:", "")
			+ "]";
	}
	
	@Override
	protected void onEnable()
	{
		lastBlock = block.getBlock();
		coordinator.setTargetBlock(lastBlock);
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
		boolean searchersChanged = false;
		
		// clear ChunkSearchers if block has changed
		Block currentBlock = block.getBlock();
		if(currentBlock != lastBlock)
		{
			lastBlock = currentBlock;
			coordinator.setTargetBlock(lastBlock);
			searchersChanged = true;
		}
		
		if(coordinator.update())
			searchersChanged = true;
		
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
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(vertexBuffer == null || bufferRegion == null)
			return;
		
		matrixStack.pushPose();
		RenderUtils.applyRegionalRenderOffset(matrixStack, bufferRegion);
		
		float[] rainbow = RenderUtils.getRainbowColor();
		vertexBuffer.draw(matrixStack, WurstRenderLayers.ESP_QUADS, rainbow,
			0.5F);
		
		matrixStack.popPose();
	}
	
	private void stopBuildingBuffer()
	{
		if(getMatchingBlocksTask != null)
			getMatchingBlocksTask.cancel(true);
		getMatchingBlocksTask = null;
		
		if(compileVerticesTask != null)
			compileVerticesTask.cancel(true);
		compileVerticesTask = null;
		
		bufferUpToDate = false;
	}
	
	private void startGetMatchingBlocksTask()
	{
		BlockPos eyesPos = BlockPos.containing(RotationUtils.getEyesPos());
		Comparator<BlockPos> comparator =
			Comparator.comparingInt(pos -> eyesPos.distManhattan(pos));
		
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
			ChatUtils.warning("Search found \u00a7lA LOT\u00a7r of blocks!"
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
		
		vertexBuffer = EasyVertexBuffer.createAndUpload(Mode.QUADS,
			DefaultVertexFormat.POSITION_COLOR, buffer -> {
				for(int[] vertex : vertices)
					buffer.addVertex(vertex[0] - region.x(), vertex[1],
						vertex[2] - region.z()).setColor(0xFFFFFFFF);
			});
		
		bufferUpToDate = true;
		bufferRegion = region;
	}
}
