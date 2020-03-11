/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.text.DecimalFormat;
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
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.Category;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.MinPriorityThreadFactory;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class SearchHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final BlockSetting block = new BlockSetting("Block",
		"The type of block to search for.", "minecraft:diamond_ore");
	
	private final EnumSetting<Area> area = new EnumSetting<>("Area",
		"The area around the player to search in.\n"
			+ "Higher values require a faster computer.",
		Area.values(), Area.D11);
	
	private final SliderSetting limit = new SliderSetting("Limit",
		"The maximum number of blocks to display.\n"
			+ "Higher values require a faster computer.",
		4, 3, 6, 1,
		v -> new DecimalFormat("##,###,###").format(Math.pow(10, v)));
	private int prevLimit;
	private boolean notify;
	
	private final HashMap<Chunk, ChunkSearcher> searchers = new HashMap<>();
	private final Set<Chunk> chunksToUpdate =
		Collections.synchronizedSet(new HashSet<>());
	private ExecutorService pool1;
	
	private ForkJoinPool pool2;
	private ForkJoinTask<HashSet<BlockPos>> getMatchingBlocksTask;
	private ForkJoinTask<ArrayList<int[]>> compileVerticesTask;
	
	private int displayList;
	private boolean displayListUpToDate;
	
	public SearchHack()
	{
		super("Search", "Helps you to find specific blocks by\n"
			+ "highlighting them in rainbow color.");
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
		
		pool1 = MinPriorityThreadFactory.newFixedThreadPool();
		pool2 = new ForkJoinPool();
		
		displayList = GL11.glGenLists(1);
		displayListUpToDate = false;
		
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
		GL11.glDeleteLists(displayList, 1);
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
		
		if(packet instanceof BlockUpdateS2CPacket)
		{
			BlockUpdateS2CPacket change = (BlockUpdateS2CPacket)packet;
			BlockPos pos = change.getPos();
			chunk = world.getChunk(pos);
			
		}else if(packet instanceof ChunkDeltaUpdateS2CPacket)
		{
			ChunkDeltaUpdateS2CPacket change =
				(ChunkDeltaUpdateS2CPacket)packet;
			ChunkDeltaUpdateS2CPacket.ChunkDeltaRecord[] changedBlocks =
				change.getRecords();
			if(changedBlocks.length == 0)
				return;
			
			BlockPos pos = changedBlocks[0].getBlockPos();
			chunk = world.getChunk(pos);
			
		}else if(packet instanceof ChunkDataS2CPacket)
		{
			ChunkDataS2CPacket chunkData = (ChunkDataS2CPacket)packet;
			chunk = world.getChunk(chunkData.getX(), chunkData.getZ());
			
		}else
			return;
		
		chunksToUpdate.add(chunk);
	}
	
	@Override
	public void onUpdate()
	{
		Block currentBlock = block.getBlock();
		BlockPos eyesPos = new BlockPos(RotationUtils.getEyesPos());
		
		ChunkPos center = getPlayerChunkPos(eyesPos);
		int range = area.getSelected().chunkRange;
		int dimensionId = MC.player.dimension.getRawId();
		
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
		
		if(!displayListUpToDate)
			setDisplayListFromTask();
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		// generate rainbow color
		float x = System.currentTimeMillis() % 2000 / 1000F;
		float red = 0.5F + 0.5F * MathHelper.sin(x * (float)Math.PI);
		float green =
			0.5F + 0.5F * MathHelper.sin((x + 4F / 3F) * (float)Math.PI);
		float blue =
			0.5F + 0.5F * MathHelper.sin((x + 8F / 3F) * (float)Math.PI);
		
		GL11.glColor4f(red, green, blue, 0.5F);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glCallList(displayList);
		GL11.glEnd();
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
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
				chunksInRange.add(MC.world.getChunk(x, z));
			
		return chunksInRange;
	}
	
	private void removeSearchersOutOfRange(ChunkPos center, int chunkRange)
	{
		for(ChunkSearcher searcher : new ArrayList<>(searchers.values()))
		{
			if(Math.abs(searcher.chunk.getPos().x - center.x) <= chunkRange
				&& Math.abs(searcher.chunk.getPos().z - center.z) <= chunkRange)
				continue;
			
			removeSearcher(searcher);
		}
	}
	
	private void replaceSearchersWithDifferences(Block currentBlock,
		int dimensionId)
	{
		for(ChunkSearcher oldSearcher : new ArrayList<>(searchers.values()))
		{
			if(currentBlock.equals(oldSearcher.block)
				&& dimensionId == oldSearcher.dimensionId)
				continue;
			
			removeSearcher(oldSearcher);
			addSearcher(oldSearcher.chunk, currentBlock, dimensionId);
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
		
		searchers.remove(searcher.chunk);
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
		
		displayListUpToDate = false;
	}
	
	private boolean areAllChunkSearchersDone()
	{
		for(ChunkSearcher searcher : searchers.values())
			if(searcher.status != ChunkSearcher.Status.DONE)
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
		
		Callable<HashSet<BlockPos>> task =
			() -> searchers.values().parallelStream()
				.flatMap(searcher -> searcher.matchingBlocks.stream())
				.sorted(Comparator
					.comparingInt(pos -> eyesPos.getManhattanDistance(pos)))
				.limit(maxBlocks)
				.collect(Collectors.toCollection(() -> new HashSet<>()));
		
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
			ChatUtils.warning("Search found \u00a7lA LOT\u00a7r of blocks!"
				+ " To prevent lag, it will only show the closest \u00a76"
				+ limit.getValueString() + "\u00a7r results.");
			notify = false;
		}
		
		return matchingBlocks;
	}
	
	private void startCompileVerticesTask()
	{
		HashSet<BlockPos> matchingBlocks = getMatchingBlocksFromTask();
		
		Callable<ArrayList<int[]>> task = () -> matchingBlocks.parallelStream()
			.flatMap(pos -> getVertices(pos, matchingBlocks).stream())
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
		
		compileVerticesTask = pool2.submit(task);
	}
	
	private ArrayList<int[]> getVertices(BlockPos pos,
		HashSet<BlockPos> matchingBlocks)
	{
		ArrayList<int[]> vertices = new ArrayList<>();
		
		if(!matchingBlocks.contains(pos.down()))
		{
			vertices.add(getVertex(pos, 0, 0, 0));
			vertices.add(getVertex(pos, 1, 0, 0));
			vertices.add(getVertex(pos, 1, 0, 1));
			vertices.add(getVertex(pos, 0, 0, 1));
		}
		
		if(!matchingBlocks.contains(pos.up()))
		{
			vertices.add(getVertex(pos, 0, 1, 0));
			vertices.add(getVertex(pos, 0, 1, 1));
			vertices.add(getVertex(pos, 1, 1, 1));
			vertices.add(getVertex(pos, 1, 1, 0));
		}
		
		if(!matchingBlocks.contains(pos.north()))
		{
			vertices.add(getVertex(pos, 0, 0, 0));
			vertices.add(getVertex(pos, 0, 1, 0));
			vertices.add(getVertex(pos, 1, 1, 0));
			vertices.add(getVertex(pos, 1, 0, 0));
		}
		
		if(!matchingBlocks.contains(pos.east()))
		{
			vertices.add(getVertex(pos, 1, 0, 0));
			vertices.add(getVertex(pos, 1, 1, 0));
			vertices.add(getVertex(pos, 1, 1, 1));
			vertices.add(getVertex(pos, 1, 0, 1));
		}
		
		if(!matchingBlocks.contains(pos.south()))
		{
			vertices.add(getVertex(pos, 0, 0, 1));
			vertices.add(getVertex(pos, 1, 0, 1));
			vertices.add(getVertex(pos, 1, 1, 1));
			vertices.add(getVertex(pos, 0, 1, 1));
		}
		
		if(!matchingBlocks.contains(pos.west()))
		{
			vertices.add(getVertex(pos, 0, 0, 0));
			vertices.add(getVertex(pos, 0, 0, 1));
			vertices.add(getVertex(pos, 0, 1, 1));
			vertices.add(getVertex(pos, 0, 1, 0));
		}
		
		return vertices;
	}
	
	private int[] getVertex(BlockPos pos, int x, int y, int z)
	{
		return new int[]{pos.getX() + x, pos.getY() + y, pos.getZ() + z};
	}
	
	private void setDisplayListFromTask()
	{
		ArrayList<int[]> vertices;
		
		try
		{
			vertices = compileVerticesTask.get();
			
		}catch(InterruptedException | ExecutionException e)
		{
			throw new RuntimeException(e);
		}
		
		GL11.glNewList(displayList, GL11.GL_COMPILE);
		for(int[] vertex : vertices)
			GL11.glVertex3d(vertex[0], vertex[1], vertex[2]);
		GL11.glEndList();
		
		displayListUpToDate = true;
	}
	
	private static class ChunkSearcher
	{
		private final Chunk chunk;
		private final Block block;
		private final int dimensionId;
		private final ArrayList<BlockPos> matchingBlocks = new ArrayList<>();
		private Status status = Status.IDLE;
		private Future<?> future;
		
		public ChunkSearcher(Chunk chunk, Block block, int dimensionId)
		{
			this.chunk = chunk;
			this.block = block;
			this.dimensionId = dimensionId;
		}
		
		public void startSearching(ExecutorService pool)
		{
			if(status != Status.IDLE)
				throw new IllegalStateException();
			
			status = Status.SEARCHING;
			future = pool.submit(() -> searchNow());
		}
		
		private void searchNow()
		{
			if(status == Status.IDLE || status == Status.DONE
				|| !matchingBlocks.isEmpty())
				throw new IllegalStateException();
			
			ChunkPos chunkPos = chunk.getPos();
			int minX = chunkPos.getStartX();
			int minY = 0;
			int minZ = chunkPos.getStartZ();
			int maxX = chunkPos.getEndX();
			int maxY = 255;
			int maxZ = chunkPos.getEndZ();
			
			for(int x = minX; x <= maxX; x++)
				for(int y = minY; y <= maxY; y++)
					for(int z = minZ; z <= maxZ; z++)
					{
						if(status == Status.INTERRUPTED || Thread.interrupted())
							return;
						
						BlockPos pos = new BlockPos(x, y, z);
						Block block = BlockUtils.getBlock(pos);
						if(!this.block.equals(block))
							continue;
						
						matchingBlocks.add(pos);
					}
				
			status = Status.DONE;
		}
		
		public void cancelSearching()
		{
			new Thread(() -> cancelNow(), "ChunkSearcher-canceller").start();
		}
		
		private void cancelNow()
		{
			if(future != null)
				try
				{
					status = Status.INTERRUPTED;
					future.get();
					
				}catch(InterruptedException | ExecutionException e)
				{
					e.printStackTrace();
				}
			
			matchingBlocks.clear();
			status = Status.IDLE;
		}
		
		private enum Status
		{
			IDLE,
			SEARCHING,
			INTERRUPTED,
			DONE;
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
