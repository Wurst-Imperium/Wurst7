/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
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
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.Category;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.search.SearchArea;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockVertexCompiler;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.ChunkSearcher;
import net.wurstclient.util.MinPriorityThreadFactory;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class SearchHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final BlockSetting block = new BlockSetting("Block",
		"The type of block to search for.", "minecraft:diamond_ore", false);
	
	private final EnumSetting<SearchArea> area = new EnumSetting<>("Area",
		"The area around the player to search in.\n"
			+ "Higher values require a faster computer.",
		SearchArea.values(), SearchArea.D11);
	
	private final SliderSetting limit = new SliderSetting("Limit",
		"The maximum number of blocks to display.\n"
			+ "Higher values require a faster computer.",
		4, 3, 6, 1, ValueDisplay.LOGARITHMIC);
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
		
		if(displayList != 0)
		{
			GL11.glDeleteLists(displayList, 1);
			displayList = 0;
		}
		
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
			
			ArrayList<BlockPos> changedBlocks = new ArrayList<>();
			change.visitUpdates((pos, state) -> changedBlocks.add(pos));
			if(changedBlocks.isEmpty())
				return;
			
			chunk = world.getChunk(changedBlocks.get(0));
			
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
		int dimensionId = MC.world.getRegistryKey().toString().hashCode();
		
		addSearchersInRange(center, currentBlock, dimensionId);
		removeSearchersOutOfRange(center);
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
		RenderUtils.applyRegionalRenderOffset();
		
		float[] rainbow = RenderUtils.getRainbowColor();
		GL11.glColor4f(rainbow[0], rainbow[1], rainbow[2], 0.5F);
		
		if(displayList != 0)
		{
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glCallList(displayList);
			GL11.glEnd();
		}
		
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
	
	private void addSearchersInRange(ChunkPos center, Block block,
		int dimensionId)
	{
		ArrayList<Chunk> chunksInRange =
			area.getSelected().getChunksInRange(center);
		
		for(Chunk chunk : chunksInRange)
		{
			if(searchers.containsKey(chunk))
				continue;
			
			addSearcher(chunk, block, dimensionId);
		}
	}
	
	private void removeSearchersOutOfRange(ChunkPos center)
	{
		for(ChunkSearcher searcher : new ArrayList<>(searchers.values()))
		{
			ChunkPos searcherPos = searcher.getChunk().getPos();
			if(area.getSelected().isInRange(searcherPos, center))
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
		
		displayListUpToDate = false;
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
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		Callable<ArrayList<int[]>> task =
			BlockVertexCompiler.createTask(matchingBlocks, regionX, regionZ);
		
		compileVerticesTask = pool2.submit(task);
	}
	
	private void setDisplayListFromTask()
	{
		ArrayList<int[]> vertices = getVerticesFromTask();
		
		GL11.glNewList(displayList, GL11.GL_COMPILE);
		
		for(int[] vertex : vertices)
			GL11.glVertex3d(vertex[0], vertex[1], vertex[2]);
		
		GL11.glEndList();
		
		displayListUpToDate = true;
	}
	
	private ArrayList<int[]> getVerticesFromTask()
	{
		try
		{
			return compileVerticesTask.get();
			
		}catch(InterruptedException | ExecutionException e)
		{
			throw new RuntimeException(e);
		}
	}
}
