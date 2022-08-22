/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilder.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.BlockVertexCompiler;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.ChunkSearcher;
import net.wurstclient.util.MinPriorityThreadFactory;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"cave finder"})
public final class CaveFinderHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final EnumSetting<Area> area = new EnumSetting<>("Area",
		"The area around the player to search in.\n"
			+ "Higher values require a faster computer.",
		Area.values(), Area.D11);
	
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
	
	private final HashMap<Chunk, ChunkSearcher> searchers = new HashMap<>();
	private final Set<Chunk> chunksToUpdate =
		Collections.synchronizedSet(new HashSet<>());
	private ExecutorService pool1;
	
	private ForkJoinPool pool2;
	private ForkJoinTask<HashSet<BlockPos>> getMatchingBlocksTask;
	private ForkJoinTask<ArrayList<int[]>> compileVerticesTask;
	
	private VertexBuffer vertexBuffer;
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
	public void onEnable()
	{
		prevLimit = limit.getValueI();
		notify = true;
		
		pool1 = MinPriorityThreadFactory.newFixedThreadPool();
		pool2 = new ForkJoinPool();
		
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
		
		stopPool2Tasks();
		pool1.shutdownNow();
		pool2.shutdownNow();
		
		if(vertexBuffer != null)
			vertexBuffer.close();
		
		chunksToUpdate.clear();
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		ClientPlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		if(player == null || world == null)
			return;
		
		Packet<?> packet = event.getPacket();
		Chunk chunk;
		
		if(packet instanceof BlockUpdateS2CPacket change)
		{
			BlockPos pos = change.getPos();
			chunk = world.getChunk(pos);
			
		}else if(packet instanceof ChunkDeltaUpdateS2CPacket change)
		{
			ArrayList<BlockPos> changedBlocks = new ArrayList<>();
			change.visitUpdates((pos, state) -> changedBlocks.add(pos));
			if(changedBlocks.isEmpty())
				return;
			
			chunk = world.getChunk(changedBlocks.get(0));
			
		}else if(packet instanceof ChunkDataS2CPacket chunkData)
			chunk = world.getChunk(chunkData.getX(), chunkData.getZ());
		else
			return;
		
		chunksToUpdate.add(chunk);
	}
	
	@Override
	public void onUpdate()
	{
		Block currentBlock = BlockUtils.getBlockFromName("minecraft:cave_air");
		BlockPos eyesPos = new BlockPos(RotationUtils.getEyesPos());
		
		ChunkPos center = getPlayerChunkPos(eyesPos);
		int range = area.getSelected().chunkRange;
		int dimensionId = MC.world.getRegistryKey().toString().hashCode();
		
		addSearchersInRange(center, range, currentBlock, dimensionId);
		removeSearchersOutOfRange(center, range);
		replaceSearchersWithDifferences(currentBlock, dimensionId);
		replaceSearchersWithChunkUpdate(currentBlock, dimensionId);
		
		if(!areAllChunkSearchersDone())
			return;
		
		checkIfLimitChanged();
		
		if(getMatchingBlocksTask == null)
			startGetMatchingBlocksTask(eyesPos);
		
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
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		// generate rainbow color
		float x = System.currentTimeMillis() % 2000 / 1000F;
		float alpha = 0.25F + 0.25F * MathHelper.sin(x * (float)Math.PI);
		
		if(opacity.getValue() > 0)
			alpha = opacity.getValueF();
		
		float[] colorF = color.getColorF();
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], alpha);
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		if(vertexBuffer != null)
		{
			Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
			Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
			Shader shader = RenderSystem.getShader();
			vertexBuffer.bind();
			vertexBuffer.draw(viewMatrix, projMatrix, shader);
			VertexBuffer.unbind();
		}
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private ChunkPos getPlayerChunkPos(BlockPos eyesPos)
	{
		int chunkX = eyesPos.getX() >> 4;
		int chunkZ = eyesPos.getZ() >> 4;
		return MC.world.getChunk(chunkX, chunkZ).getPos();
	}
	
	private void addSearchersInRange(ChunkPos center, int chunkRange,
		Block block, int dimensionId)
	{
		ArrayList<Chunk> chunksInRange = getChunksInRange(center, chunkRange);
		
		for(Chunk chunk : chunksInRange)
		{
			if(searchers.containsKey(chunk))
				continue;
			
			addSearcher(chunk, block, dimensionId);
		}
	}
	
	private ArrayList<Chunk> getChunksInRange(ChunkPos center, int chunkRange)
	{
		ArrayList<Chunk> chunksInRange = new ArrayList<>();
		
		for(int x = center.x - chunkRange; x <= center.x + chunkRange; x++)
			for(int z = center.z - chunkRange; z <= center.z + chunkRange; z++)
			{
				Chunk chunk = MC.world.getChunk(x, z);
				if(chunk instanceof EmptyChunk)
					continue;
				
				chunksInRange.add(chunk);
			}
		
		return chunksInRange;
	}
	
	private void removeSearchersOutOfRange(ChunkPos center, int chunkRange)
	{
		for(ChunkSearcher searcher : new ArrayList<>(searchers.values()))
		{
			ChunkPos searcherPos = searcher.getChunk().getPos();
			
			if(Math.abs(searcherPos.x - center.x) <= chunkRange
				&& Math.abs(searcherPos.z - center.z) <= chunkRange)
				continue;
			
			removeSearcher(searcher);
		}
	}
	
	private void replaceSearchersWithDifferences(Block currentBlock,
		int dimensionId)
	{
		for(ChunkSearcher oldSearcher : new ArrayList<>(searchers.values()))
		{
			if(currentBlock.equals(oldSearcher.getBlock())
				&& dimensionId == oldSearcher.getDimensionId())
				continue;
			
			removeSearcher(oldSearcher);
			addSearcher(oldSearcher.getChunk(), currentBlock, dimensionId);
		}
	}
	
	private void replaceSearchersWithChunkUpdate(Block currentBlock,
		int dimensionId)
	{
		synchronized(chunksToUpdate)
		{
			if(chunksToUpdate.isEmpty())
				return;
			
			for(Iterator<Chunk> itr = chunksToUpdate.iterator(); itr.hasNext();)
			{
				Chunk chunk = itr.next();
				
				ChunkSearcher oldSearcher = searchers.get(chunk);
				if(oldSearcher == null)
					continue;
				
				removeSearcher(oldSearcher);
				addSearcher(chunk, currentBlock, dimensionId);
				itr.remove();
			}
		}
	}
	
	private void addSearcher(Chunk chunk, Block block, int dimensionId)
	{
		stopPool2Tasks();
		
		ChunkSearcher searcher = new ChunkSearcher(chunk, block, dimensionId);
		searchers.put(chunk, searcher);
		searcher.startSearching(pool1);
	}
	
	private void removeSearcher(ChunkSearcher searcher)
	{
		stopPool2Tasks();
		
		searchers.remove(searcher.getChunk());
		searcher.cancelSearching();
	}
	
	private void stopPool2Tasks()
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
			stopPool2Tasks();
			notify = true;
			prevLimit = limit.getValueI();
		}
	}
	
	private void startGetMatchingBlocksTask(BlockPos eyesPos)
	{
		int maxBlocks = (int)Math.pow(10, limit.getValueI());
		
		Callable<HashSet<BlockPos>> task = () -> searchers.values()
			.parallelStream()
			.flatMap(searcher -> searcher.getMatchingBlocks().stream())
			.sorted(Comparator
				.comparingInt(pos -> eyesPos.getManhattanDistance(pos)))
			.limit(maxBlocks).collect(Collectors.toCollection(HashSet::new));
		
		getMatchingBlocksTask = pool2.submit(task);
	}
	
	private HashSet<BlockPos> getMatchingBlocksFromTask()
	{
		HashSet<BlockPos> matchingBlocks = new HashSet<>();
		
		try
		{
			matchingBlocks = getMatchingBlocksTask.get();
			
		}catch(InterruptedException | ExecutionException e)
		{
			throw new RuntimeException(e);
		}
		
		int maxBlocks = (int)Math.pow(10, limit.getValueI());
		
		if(matchingBlocks.size() < maxBlocks)
			notify = true;
		else if(notify)
		{
			ChatUtils.warning("CaveFinder found \u00a7lA LOT\u00a7r of blocks!"
				+ " To prevent lag, it will only show the closest \u00a76"
				+ limit.getValueString() + "\u00a7r results.");
			notify = false;
		}
		
		return matchingBlocks;
	}
	
	private void startCompileVerticesTask()
	{
		HashSet<BlockPos> matchingBlocks = getMatchingBlocksFromTask();
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		Callable<ArrayList<int[]>> task =
			BlockVertexCompiler.createTask(matchingBlocks, regionX, regionZ);
		
		compileVerticesTask = pool2.submit(task);
	}
	
	private void setBufferFromTask()
	{
		ArrayList<int[]> vertices = getVerticesFromTask();
		
		if(vertexBuffer != null)
			vertexBuffer.close();
		
		vertexBuffer = new VertexBuffer();
		
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		
		for(int[] vertex : vertices)
			bufferBuilder.vertex(vertex[0], vertex[1], vertex[2]).next();
		
		BuiltBuffer buffer = bufferBuilder.end();
		
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
		
		bufferUpToDate = true;
	}
	
	public ArrayList<int[]> getVerticesFromTask()
	{
		try
		{
			return compileVerticesTask.get();
			
		}catch(InterruptedException | ExecutionException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private enum Area
	{
		D3("3x3 chunks", 1),
		D5("5x5 chunks", 2),
		D7("7x7 chunks", 3),
		D9("9x9 chunks", 4),
		D11("11x11 chunks", 5),
		D13("13x13 chunks", 6),
		D15("15x15 chunks", 7),
		D17("17x17 chunks", 8),
		D19("19x19 chunks", 9),
		D21("21x21 chunks", 10),
		D23("23x23 chunks", 11),
		D25("25x25 chunks", 12),
		D27("27x27 chunks", 13),
		D29("29x29 chunks", 14),
		D31("31x31 chunks", 15),
		D33("33x33 chunks", 16);
		
		private final String name;
		private final int chunkRange;
		
		private Area(String name, int chunkRange)
		{
			this.name = name;
			this.chunkRange = chunkRange;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
