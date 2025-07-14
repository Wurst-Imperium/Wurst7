/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.minecraft.block.Block;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockVertexCompiler;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.EasyVertexBuffer;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RenderUtils.ColoredPoint;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.chunk.ChunkSearcher;
import net.wurstclient.util.chunk.ChunkSearcherCoordinator;

@SearchTags({"BlockESP", "block esp"})
public final class SearchHack extends Hack implements UpdateListener,
	RenderListener, CameraTransformViewBobbingListener
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
	
	private final CheckboxSetting drawTracers = new CheckboxSetting("Tracers",
		"Draws lines from your crosshair to the found blocks.", false);
	
	private final SliderSetting tracerLimit = new SliderSetting("Tracer limit",
		"The maximum number of tracers to display.", 4, 1, 5, 1,
		ValueDisplay.LOGARITHMIC);
	
	private final ChunkSearcherCoordinator coordinator =
		new ChunkSearcherCoordinator(area);
	
	private ForkJoinPool forkJoinPool;
	private ForkJoinTask<ArrayList<BlockPos>> getMatchingBlocksTask;
	private ForkJoinTask<ArrayList<int[]>> compileVerticesTask;
	
	private EasyVertexBuffer vertexBuffer;
	private RegionPos bufferRegion;
	private boolean bufferUpToDate;
	private final ArrayList<BlockPos> foundBlocks = new ArrayList<>();
	
	public SearchHack()
	{
		super("Search");
		setCategory(Category.RENDER);
		addSetting(block);
		addSetting(area);
		addSetting(limit);
		addSetting(drawTracers);
		addSetting(tracerLimit);
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
		foundBlocks.clear();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, coordinator);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, coordinator);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		
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
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(drawTracers.isChecked())
			event.cancel();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if((vertexBuffer == null || bufferRegion == null)
			&& (!drawTracers.isChecked() || foundBlocks.isEmpty()))
			return;
		
		float[] rainbow = RenderUtils.getRainbowColor();
		
		if(vertexBuffer != null && bufferRegion != null)
		{
			matrixStack.push();
			RenderUtils.applyRegionalRenderOffset(matrixStack, bufferRegion);
			
			vertexBuffer.draw(matrixStack, WurstRenderLayers.ESP_QUADS, rainbow,
				0.5F);
			
			matrixStack.pop();
		}
		
		if(drawTracers.isChecked() && !foundBlocks.isEmpty())
		{
			List<BlockPos> blocksToTrace = foundBlocks.subList(0,
				Math.min(foundBlocks.size(), tracerLimit.getValueLog()));
			ArrayList<ColoredPoint> points = new ArrayList<>();
			for(BlockPos pos : blocksToTrace)
				points.add(new ColoredPoint(Vec3d.ofCenter(pos),
					RenderUtils.toIntColor(rainbow, 0.5F)));
			
			RenderUtils.drawTracers(matrixStack, partialTicks, points, false);
		}
	}
	
	private void stopBuildingBuffer()
	{
		if(getMatchingBlocksTask != null)
			getMatchingBlocksTask.cancel(true);
		getMatchingBlocksTask = null;
		
		if(compileVerticesTask != null)
			compileVerticesTask.cancel(true);
		compileVerticesTask = null;
		
		foundBlocks.clear();
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
			.collect(Collectors.toCollection(ArrayList::new)));
	}
	
	private void startCompileVerticesTask()
	{
		ArrayList<BlockPos> matchingBlocks = getMatchingBlocksTask.join();
		foundBlocks.clear();
		foundBlocks.addAll(matchingBlocks);
		
		if(matchingBlocks.size() < limit.getValueLog())
			notify = true;
		else if(notify)
		{
			ChatUtils.warning("Search found \u00a7lA LOT\u00a7r of blocks!"
				+ " To prevent lag, it will only show the closest \u00a76"
				+ limit.getValueString() + "\u00a7r results.");
			notify = false;
		}
		
		compileVerticesTask = forkJoinPool.submit(
			() -> BlockVertexCompiler.compile(new HashSet<>(matchingBlocks)));
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
