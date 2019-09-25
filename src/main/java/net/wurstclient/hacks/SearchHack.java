/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.Category;
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
	implements UpdateListener, RenderListener
{
	private final BlockSetting block = new BlockSetting("Block",
		"The type of block to search for.", "minecraft:diamond_ore");
	
	private final EnumSetting<DrawDistance> area =
		new EnumSetting<>("Area", "The area around the player to search in.",
			DrawDistance.values(), DrawDistance.D11);
	
	private final SliderSetting limit = new SliderSetting("Limit",
		"The maximum number of blocks to display.", 4, 3, 6, 1,
		v -> new DecimalFormat("##,###,###").format(Math.pow(10, v)));
	
	private final HashMap<Chunk, ChunkScanner> scanners = new HashMap<>();
	private ExecutorService pool1;
	private ForkJoinPool pool2;
	private ForkJoinTask<HashSet<BlockPos>> getMatchingBlocksTask;
	private ForkJoinTask<ArrayList<int[]>> compileVerticesTask;
	
	private int displayList;
	private boolean displayListUpToDate;
	
	public boolean notify;
	
	public SearchHack()
	{
		super("Search", "Helps you to find specific blocks.");
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
		notify = true;
		pool1 = Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors(),
			new MinPriorityThreadFactory());
		pool2 = new ForkJoinPool();
		displayList = GL11.glGenLists(1);
		displayListUpToDate = false;
		
		WURST.getEventManager().add(UpdateListener.class, this);
		WURST.getEventManager().add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		WURST.getEventManager().remove(UpdateListener.class, this);
		WURST.getEventManager().remove(RenderListener.class, this);
		
		stopRunningTasks();
		pool1.shutdownNow();
		pool2.shutdownNow();
		GL11.glDeleteLists(displayList, 1);
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
	
	@Override
	public void onUpdate()
	{
		Block currentBlock = block.getBlock();
		BlockPos eyesPos = new BlockPos(RotationUtils.getEyesPos());
		
		ChunkPos center = getPlayerChunkPos(eyesPos);
		int range = area.getSelected().chunkRange;
		
		addScannersInRange(center, range, currentBlock);
		removeScannersOutOfRange(center, range);
		replaceScannersWithDifferentBlock(currentBlock);
		
		if(!areAllChunkScannersDone())
			return;
		
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
	
	private ChunkPos getPlayerChunkPos(BlockPos eyesPos)
	{
		int chunkX = eyesPos.getX() >> 4;
		int chunkZ = eyesPos.getZ() >> 4;
		return MC.world.getChunk(chunkX, chunkZ).getPos();
	}
	
	private void addScannersInRange(ChunkPos center, int chunkRange,
		Block block)
	{
		ArrayList<Chunk> chunksInRange = getChunksInRange(center, chunkRange);
		
		for(Chunk chunk : chunksInRange)
		{
			if(scanners.containsKey(chunk))
				continue;
			
			addScanner(chunk, block);
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
	
	private void removeScannersOutOfRange(ChunkPos center, int chunkRange)
	{
		for(ChunkScanner scanner : new ArrayList<>(scanners.values()))
		{
			if(Math.abs(scanner.chunk.getPos().x - center.x) <= chunkRange
				&& Math.abs(scanner.chunk.getPos().z - center.z) <= chunkRange)
				continue;
			
			removeScanner(scanner);
		}
	}
	
	private void replaceScannersWithDifferentBlock(Block currentBlock)
	{
		for(ChunkScanner oldScanner : new ArrayList<>(scanners.values()))
		{
			if(currentBlock.equals(oldScanner.block))
				continue;
			
			removeScanner(oldScanner);
			addScanner(oldScanner.chunk, currentBlock);
		}
	}
	
	private void addScanner(Chunk chunk, Block block)
	{
		stopRunningTasks();
		
		ChunkScanner scanner = new ChunkScanner(chunk, block);
		scanners.put(chunk, scanner);
		scanner.startScanning();
	}
	
	private void removeScanner(ChunkScanner scanner)
	{
		stopRunningTasks();
		
		scanners.remove(scanner.chunk);
		scanner.cancelScanning();
	}
	
	private void stopRunningTasks()
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
	
	private boolean areAllChunkScannersDone()
	{
		for(ChunkScanner scanner : scanners.values())
			if(scanner.status != ChunkScannerStatus.DONE)
				return false;
			
		return true;
	}
	
	private void startGetMatchingBlocksTask(BlockPos eyesPos)
	{
		int maxBlocks = (int)Math.pow(10, limit.getValueI());
		
		Callable<HashSet<BlockPos>> task =
			() -> scanners.values().parallelStream()
				.flatMap(scanner -> scanner.matchingBlocks.stream())
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
	
	private class ChunkScanner
	{
		private final Chunk chunk;
		private final Block block;
		private final ArrayList<BlockPos> matchingBlocks = new ArrayList<>();
		private ChunkScannerStatus status = ChunkScannerStatus.IDLE;
		private Future future;
		
		public ChunkScanner(Chunk chunk, Block block)
		{
			this.chunk = chunk;
			this.block = block;
		}
		
		public void startScanning()
		{
			if(status != ChunkScannerStatus.IDLE)
				throw new IllegalStateException();
			
			status = ChunkScannerStatus.SCANNING;
			future = pool1.submit(() -> scanNow());
		}
		
		private void scanNow()
		{
			if(status == ChunkScannerStatus.IDLE
				|| status == ChunkScannerStatus.DONE
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
						if(status == ChunkScannerStatus.INTERRUPTED
							|| Thread.interrupted())
							return;
						
						BlockPos pos = new BlockPos(x, y, z);
						Block block = BlockUtils.getBlock(pos);
						if(!this.block.equals(block))
							continue;
						
						matchingBlocks.add(pos);
					}
				
			status = ChunkScannerStatus.DONE;
		}
		
		public void cancelScanning()
		{
			new Thread(() -> cancelNow(), "ChunkScanner-canceller").start();
		}
		
		private void cancelNow()
		{
			if(future != null)
				try
				{
					status = ChunkScannerStatus.INTERRUPTED;
					future.get();
					
				}catch(InterruptedException | ExecutionException e)
				{
					e.printStackTrace();
				}
			
			matchingBlocks.clear();
			status = ChunkScannerStatus.IDLE;
		}
	}
	
	private enum ChunkScannerStatus
	{
		IDLE,
		SCANNING,
		INTERRUPTED,
		DONE;
	}
	
	private enum DrawDistance
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
		
		private DrawDistance(String name, int chunkRange)
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
