/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.Category;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.portalesp.PortalEspBlockGroup;
import net.wurstclient.hacks.portalesp.PortalEspRenderer;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.EspStyleSetting;
import net.wurstclient.util.ChunkSearcherMulti;
import net.wurstclient.util.ChunkSearcherMulti.Result;
import net.wurstclient.util.ChunkUtils;
import net.wurstclient.util.MinPriorityThreadFactory;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class PortalEspHack extends Hack implements UpdateListener,
	PacketInputListener, CameraTransformViewBobbingListener, RenderListener
{
	private final EspStyleSetting style = new EspStyleSetting();
	
	private final PortalEspBlockGroup netherPortal =
		new PortalEspBlockGroup(Blocks.NETHER_PORTAL,
			new ColorSetting("Nether portal color",
				"Nether portals will be highlighted in this color.", Color.RED),
			new CheckboxSetting("Include nether portals", true));
	
	private final PortalEspBlockGroup endPortal =
		new PortalEspBlockGroup(Blocks.END_PORTAL,
			new ColorSetting("End portal color",
				"End portals will be highlighted in this color.", Color.GREEN),
			new CheckboxSetting("Include end portals", true));
	
	private final PortalEspBlockGroup endPortalFrame = new PortalEspBlockGroup(
		Blocks.END_PORTAL_FRAME,
		new ColorSetting("End portal frame color",
			"End portal frames will be highlighted in this color.", Color.BLUE),
		new CheckboxSetting("Include end portal frames", true));
	
	private final PortalEspBlockGroup endGateway = new PortalEspBlockGroup(
		Blocks.END_GATEWAY,
		new ColorSetting("End gateway color",
			"End gateways will be highlighted in this color.", Color.YELLOW),
		new CheckboxSetting("Include end gateways", true));
	
	private final List<PortalEspBlockGroup> groups =
		Arrays.asList(netherPortal, endPortal, endPortalFrame, endGateway);
	
	private final ChunkAreaSetting area = new ChunkAreaSetting("Area",
		"The area around the player to search in.\n"
			+ "Higher values require a faster computer.");
	
	private final HashMap<ChunkPos, ChunkSearcherMulti> searchers =
		new HashMap<>();
	private final Set<ChunkPos> chunksToUpdate =
		Collections.synchronizedSet(new HashSet<>());
	private ExecutorService pool1;
	
	private ForkJoinPool pool2;
	private ForkJoinTask<ArrayList<Result>> getMatchingBlocksTask;
	
	public PortalEspHack()
	{
		super("PortalESP");
		setCategory(Category.RENDER);
		
		addSetting(style);
		groups.stream().flatMap(PortalEspBlockGroup::getSettings)
			.forEach(this::addSetting);
		addSetting(area);
	}
	
	@Override
	public void onEnable()
	{
		pool1 = MinPriorityThreadFactory.newFixedThreadPool();
		pool2 = new ForkJoinPool();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		PortalEspRenderer.prepareBuffers();
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		stopPool2Tasks();
		pool1.shutdownNow();
		pool2.shutdownNow();
		
		chunksToUpdate.clear();
		
		groups.forEach(PortalEspBlockGroup::clear);
		PortalEspRenderer.closeBuffers();
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		ChunkPos chunkPos = ChunkUtils.getAffectedChunk(event.getPacket());
		
		if(chunkPos != null)
			chunksToUpdate.add(chunkPos);
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.getSelected().hasLines())
			event.cancel();
	}
	
	@Override
	public void onUpdate()
	{
		ArrayList<Block> currentBlockList =
			groups.parallelStream().map(PortalEspBlockGroup::getBlock)
				.collect(Collectors.toCollection(ArrayList::new));
		
		BlockPos eyesPos = BlockPos.ofFloored(RotationUtils.getEyesPos());
		int dimensionId = MC.world.getRegistryKey().toString().hashCode();
		
		addSearchersInRange(currentBlockList, dimensionId);
		removeSearchersOutOfRange(dimensionId);
		replaceSearchersWithDifferences(currentBlockList, dimensionId);
		replaceSearchersWithChunkUpdate(currentBlockList, dimensionId);
		
		if(!areAllChunkSearchersDone())
			return;
		
		if(getMatchingBlocksTask == null)
			startGetMatchingBlocksTask(eyesPos);
		
		if(!getMatchingBlocksTask.isDone())
			return;
		
		getMatchingBlocksFromTask();
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
		
		PortalEspRenderer espRenderer = new PortalEspRenderer(matrixStack);
		
		if(style.getSelected().hasBoxes())
		{
			RenderSystem.setShader(GameRenderer::getPositionProgram);
			groups.stream().filter(PortalEspBlockGroup::isEnabled)
				.forEach(espRenderer::renderBoxes);
		}
		
		if(style.getSelected().hasLines())
		{
			RenderSystem.setShader(GameRenderer::getPositionProgram);
			groups.stream().filter(PortalEspBlockGroup::isEnabled)
				.forEach(espRenderer::renderLines);
		}
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private void addSearchersInRange(ArrayList<Block> blockList,
		int dimensionId)
	{
		for(Chunk chunk : area.getChunksInRange())
		{
			if(searchers.containsKey(chunk.getPos()))
				continue;
			
			addSearcher(chunk, blockList, dimensionId);
		}
	}
	
	private void removeSearchersOutOfRange(int dimensionId)
	{
		for(ChunkSearcherMulti searcher : new ArrayList<>(searchers.values()))
		{
			int searcherDimensionId = searcher.getDimensionId();
			if(area.isInRange(searcher.getPos())
				&& searcherDimensionId == dimensionId)
				continue;
			
			removeSearcher(searcher);
		}
	}
	
	private void replaceSearchersWithDifferences(
		ArrayList<Block> currentBlockList, int dimensionId)
	{
		for(ChunkSearcherMulti oldSearcher : new ArrayList<>(
			searchers.values()))
		{
			if(currentBlockList.equals(oldSearcher.getBlockList()))
				continue;
			
			removeSearcher(oldSearcher);
			addSearcher(oldSearcher.getChunk(), currentBlockList, dimensionId);
		}
	}
	
	private void replaceSearchersWithChunkUpdate(
		ArrayList<Block> currentBlockList, int dimensionId)
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
			ChunkSearcherMulti oldSearcher = searchers.get(chunkPos);
			if(oldSearcher == null)
				continue;
			
			removeSearcher(oldSearcher);
			Chunk chunk = MC.world.getChunk(chunkPos.x, chunkPos.z);
			addSearcher(chunk, currentBlockList, dimensionId);
		}
	}
	
	private void addSearcher(Chunk chunk, ArrayList<Block> blockList,
		int dimensionId)
	{
		ChunkSearcherMulti searcher =
			new ChunkSearcherMulti(chunk, blockList, dimensionId);
		searchers.put(chunk.getPos(), searcher);
		searcher.startSearching(pool1);
	}
	
	private void removeSearcher(ChunkSearcherMulti searcher)
	{
		stopPool2Tasks();
		
		searchers.remove(searcher.getPos());
		searcher.cancelSearching();
	}
	
	private void stopPool2Tasks()
	{
		if(getMatchingBlocksTask != null)
		{
			getMatchingBlocksTask.cancel(true);
			getMatchingBlocksTask = null;
		}
	}
	
	private boolean areAllChunkSearchersDone()
	{
		for(ChunkSearcherMulti searcher : searchers.values())
			if(searcher.getStatus() != ChunkSearcherMulti.Status.DONE)
				return false;
			
		return true;
	}
	
	private void startGetMatchingBlocksTask(BlockPos eyesPos)
	{
		Callable<ArrayList<Result>> task =
			() -> searchers.values().parallelStream()
				.flatMap(searcher -> searcher.getMatchingBlocks().stream())
				.sorted(Comparator.comparingInt(
					result -> eyesPos.getManhattanDistance(result.getPos())))
				.collect(Collectors.toCollection(ArrayList::new));
		
		getMatchingBlocksTask = pool2.submit(task);
	}
	
	private void getMatchingBlocksFromTask()
	{
		groups.forEach(PortalEspBlockGroup::clear);
		
		for(Result result : getMatchingBlocksTask.join())
			groups.parallelStream()
				.filter(group -> group.getBlock().getClass() == result
					.getBlock().getClass())
				.forEach(group -> group.add(result.getPos()));
	}
}
