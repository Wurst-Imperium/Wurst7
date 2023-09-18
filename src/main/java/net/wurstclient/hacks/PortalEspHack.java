/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.BiPredicate;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
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
import net.wurstclient.util.ChunkSearcher;
import net.wurstclient.util.ChunkSearcher.Result;
import net.wurstclient.util.ChunkUtils;
import net.wurstclient.util.MinPriorityThreadFactory;
import net.wurstclient.util.RenderUtils;

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
	
	private final HashMap<ChunkPos, ChunkSearcher> searchers = new HashMap<>();
	private final BiPredicate<BlockPos, BlockState> query =
		(pos, state) -> state.getBlock() == Blocks.NETHER_PORTAL
			|| state.getBlock() == Blocks.END_PORTAL
			|| state.getBlock() == Blocks.END_PORTAL_FRAME
			|| state.getBlock() == Blocks.END_GATEWAY;
	
	private final Set<ChunkPos> chunksToUpdate =
		Collections.synchronizedSet(new HashSet<>());
	private ExecutorService threadPool;
	
	private boolean groupsUpToDate;
	
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
		threadPool = MinPriorityThreadFactory.newFixedThreadPool();
		groupsUpToDate = false;
		
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
		
		threadPool.shutdownNow();
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
		DimensionType dimension = MC.world.getDimension();
		HashSet<ChunkPos> chunkUpdates = clearChunksToUpdate();
		
		// remove outdated ChunkSearchers
		for(ChunkSearcher searcher : new ArrayList<>(searchers.values()))
		{
			boolean remove = false;
			ChunkPos searcherPos = searcher.getPos();
			
			// wrong dimension
			if(dimension != searcher.getDimension())
				remove = true;
			
			// out of range
			else if(!area.isInRange(searcherPos))
				remove = true;
			
			// chunk update
			else if(chunkUpdates.contains(searcherPos))
				remove = true;
			
			if(remove)
			{
				searchers.remove(searcherPos);
				searcher.cancel();
				groupsUpToDate = false;
			}
		}
		
		// add new ChunkSearchers
		for(Chunk chunk : area.getChunksInRange())
		{
			ChunkPos chunkPos = chunk.getPos();
			if(searchers.containsKey(chunkPos))
				continue;
			
			ChunkSearcher searcher = new ChunkSearcher(query, chunk, dimension);
			searchers.put(chunkPos, searcher);
			searcher.start(threadPool);
			groupsUpToDate = false;
		}
		
		if(!searchers.values().stream().allMatch(ChunkSearcher::isDone))
			return;
		
		if(!groupsUpToDate)
			updateGroupBoxes();
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
	
	private HashSet<ChunkPos> clearChunksToUpdate()
	{
		synchronized(chunksToUpdate)
		{
			HashSet<ChunkPos> chunks = new HashSet<>(chunksToUpdate);
			chunksToUpdate.clear();
			return chunks;
		}
	}
	
	private void updateGroupBoxes()
	{
		groups.forEach(PortalEspBlockGroup::clear);
		searchers.values().stream().flatMap(ChunkSearcher::getMatches)
			.forEachOrdered(this::addToGroupBoxes);
		
		groupsUpToDate = true;
	}
	
	private void addToGroupBoxes(Result result)
	{
		for(PortalEspBlockGroup group : groups)
			if(result.state().getBlock() == group.getBlock())
			{
				group.add(result.pos());
				break;
			}
	}
}
