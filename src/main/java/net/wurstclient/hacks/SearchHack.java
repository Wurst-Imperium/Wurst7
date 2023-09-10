/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilder.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.wurstclient.Category;
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
import net.wurstclient.util.ChunkSearcher;
import net.wurstclient.util.ChunkUtils;
import net.wurstclient.util.MinPriorityThreadFactory;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class SearchHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final BlockSetting block = new BlockSetting("Block",
		"The type of block to search for.", "minecraft:diamond_ore", false);
	
	private final ChunkAreaSetting area = new ChunkAreaSetting("Area",
		"The area around the player to search in.\n"
			+ "Higher values require a faster computer.");
	
	private final SliderSetting limit = new SliderSetting("Limit",
		"The maximum number of blocks to display.\n"
			+ "Higher values require a faster computer.",
		4, 3, 6, 1, ValueDisplay.LOGARITHMIC);
	private int prevLimit;
	private boolean notify;
	
	private final HashMap<ChunkPos, ChunkSearcher> searchers = new HashMap<>();
	private final Set<ChunkPos> chunksToUpdate =
		Collections.synchronizedSet(new HashSet<>());
	private ExecutorService threadPool;
	
	private ForkJoinPool forkJoinPool;
	private ForkJoinTask<HashSet<BlockPos>> getMatchingBlocksTask;
	private ForkJoinTask<ArrayList<int[]>> compileVerticesTask;
	
	private VertexBuffer vertexBuffer;
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
	public void onEnable()
	{
		prevLimit = limit.getValueI();
		notify = true;
		
		threadPool = MinPriorityThreadFactory.newFixedThreadPool();
		forkJoinPool = new ForkJoinPool();
		
		bufferUpToDate = false;
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		stopBuildingBuffer();
		threadPool.shutdownNow();
		forkJoinPool.shutdownNow();
		
		if(vertexBuffer != null)
		{
			vertexBuffer.close();
			vertexBuffer = null;
		}
		
		chunksToUpdate.clear();
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		ChunkPos chunkPos = ChunkUtils.getAffectedChunk(event.getPacket());
		
		if(chunkPos != null)
			chunksToUpdate.add(chunkPos);
	}
	
	@Override
	public void onUpdate()
	{
		Block currentBlock = block.getBlock();
		DimensionType dimension = MC.world.getDimension();
		
		addSearchersInRange(currentBlock, dimension);
		removeSearchersOutOfRange();
		replaceSearchersWithDifferences(currentBlock, dimension);
		replaceSearchersWithChunkUpdate(currentBlock, dimension);
		
		if(!areAllChunkSearchersDone())
			return;
		
		checkIfLimitChanged();
		
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
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		float[] rainbow = RenderUtils.getRainbowColor();
		RenderSystem.setShaderColor(rainbow[0], rainbow[1], rainbow[2], 0.5F);
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		if(vertexBuffer != null)
		{
			Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
			Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
			ShaderProgram shader = RenderSystem.getShader();
			vertexBuffer.bind();
			vertexBuffer.draw(viewMatrix, projMatrix, shader);
			VertexBuffer.unbind();
		}
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private void addSearchersInRange(Block block, DimensionType dimension)
	{
		for(Chunk chunk : area.getChunksInRange())
		{
			if(searchers.containsKey(chunk.getPos()))
				continue;
			
			addSearcher(chunk, block, dimension);
		}
	}
	
	private void removeSearchersOutOfRange()
	{
		for(ChunkSearcher searcher : new ArrayList<>(searchers.values()))
		{
			if(area.isInRange(searcher.getPos()))
				continue;
			
			removeSearcher(searcher);
		}
	}
	
	private void replaceSearchersWithDifferences(Block currentBlock,
		DimensionType dimension)
	{
		for(ChunkSearcher oldSearcher : new ArrayList<>(searchers.values()))
		{
			if(currentBlock.equals(oldSearcher.getBlock())
				&& dimension == oldSearcher.getDimension())
				continue;
			
			removeSearcher(oldSearcher);
			addSearcher(oldSearcher.getChunk(), currentBlock, dimension);
		}
	}
	
	private void replaceSearchersWithChunkUpdate(Block currentBlock,
		DimensionType dimension)
	{
		// get the chunks to update and remove them from the set
		ChunkPos[] chunks;
		synchronized(chunksToUpdate)
		{
			chunks = chunksToUpdate.toArray(ChunkPos[]::new);
			chunksToUpdate.clear();
		}
		
		// update the chunks separately so the synchronization
		// doesn't have to wait for that
		for(ChunkPos chunkPos : chunks)
		{
			ChunkSearcher oldSearcher = searchers.get(chunkPos);
			if(oldSearcher == null)
				continue;
			
			removeSearcher(oldSearcher);
			Chunk chunk = MC.world.getChunk(chunkPos.x, chunkPos.z);
			addSearcher(chunk, currentBlock, dimension);
		}
	}
	
	private void addSearcher(Chunk chunk, Block block, DimensionType dimension)
	{
		stopBuildingBuffer();
		
		ChunkSearcher searcher = new ChunkSearcher(chunk, block, dimension);
		searchers.put(chunk.getPos(), searcher);
		searcher.startSearching(threadPool);
	}
	
	private void removeSearcher(ChunkSearcher searcher)
	{
		stopBuildingBuffer();
		
		searchers.remove(searcher.getPos());
		searcher.cancelSearching();
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
	
	private boolean areAllChunkSearchersDone()
	{
		for(ChunkSearcher searcher : searchers.values())
			if(searcher.getStatus() != ChunkSearcher.Status.DONE)
				return false;
			
		return true;
	}
	
	private void checkIfLimitChanged()
	{
		if(limit.getValueI() != prevLimit)
		{
			stopBuildingBuffer();
			notify = true;
			prevLimit = limit.getValueI();
		}
	}
	
	private void startGetMatchingBlocksTask()
	{
		BlockPos eyesPos = BlockPos.ofFloored(RotationUtils.getEyesPos());
		Comparator<BlockPos> comparator =
			Comparator.comparingInt(pos -> eyesPos.getManhattanDistance(pos));
		
		getMatchingBlocksTask = forkJoinPool.submit(() -> searchers.values()
			.parallelStream().flatMap(ChunkSearcher::getMatchingBlocks)
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
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		compileVerticesTask = forkJoinPool.submit(() -> BlockVertexCompiler
			.compile(matchingBlocks, regionX, regionZ));
	}
	
	private void setBufferFromTask()
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		
		for(int[] vertex : compileVerticesTask.join())
			bufferBuilder.vertex(vertex[0], vertex[1], vertex[2]).next();
		
		BuiltBuffer buffer = bufferBuilder.end();
		
		if(vertexBuffer != null)
			vertexBuffer.close();
		
		vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
		
		bufferUpToDate = true;
	}
}
