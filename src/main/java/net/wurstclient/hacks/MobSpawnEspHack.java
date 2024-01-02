/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilder.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.settings.ChunkAreaSetting.ChunkArea;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChunkUtils;
import net.wurstclient.util.MinPriorityThreadFactory;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

@SearchTags({"mob spawn esp", "LightLevelESP", "light level esp",
	"LightLevelOverlay", "light level overlay"})
public final class MobSpawnEspHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final ChunkAreaSetting drawDistance =
		new ChunkAreaSetting("Draw distance", "", ChunkArea.A9);
	
	private final SliderSetting loadingSpeed = new SliderSetting(
		"Loading speed", 1, 1, 5, 1, ValueDisplay.INTEGER.withSuffix("x"));
	
	private final CheckboxSetting depthTest =
		new CheckboxSetting("Depth test", true);
	
	private final HashMap<ChunkPos, ChunkScanner> scanners = new HashMap<>();
	private ExecutorService pool;
	
	public MobSpawnEspHack()
	{
		super("MobSpawnESP");
		setCategory(Category.RENDER);
		addSetting(drawDistance);
		addSetting(loadingSpeed);
		addSetting(depthTest);
	}
	
	@Override
	public void onEnable()
	{
		pool = MinPriorityThreadFactory.newFixedThreadPool();
		
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
		
		for(ChunkScanner scanner : new ArrayList<>(scanners.values()))
		{
			scanner.reset();
			scanners.remove(scanner.chunk.getPos());
		}
		
		pool.shutdownNow();
	}
	
	@Override
	public void onUpdate()
	{
		DimensionType dimension = MC.world.getDimension();
		
		// remove old scanners that are out of range
		for(ChunkScanner scanner : new ArrayList<>(scanners.values()))
		{
			if(drawDistance.isInRange(scanner.chunk.getPos())
				&& dimension == scanner.dimension)
				continue;
			
			scanner.reset();
			scanners.remove(scanner.chunk.getPos());
		}
		
		// create & start scanners for new chunks
		for(Chunk chunk : drawDistance.getChunksInRange())
		{
			ChunkPos chunkPos = chunk.getPos();
			if(scanners.containsKey(chunkPos))
				continue;
			
			ChunkScanner scanner = new ChunkScanner(chunk, dimension);
			scanners.put(chunkPos, scanner);
			scanner.future = pool.submit(() -> scanner.scan());
		}
		
		// generate vertex buffers
		ChunkPos center = MC.player.getChunkPos();
		Comparator<ChunkScanner> c = Comparator.comparingInt(
			s -> ChunkUtils.getManhattanDistance(center, s.chunk.getPos()));
		List<ChunkScanner> sortedScanners = scanners.values().stream()
			.filter(s -> s.doneScanning).filter(s -> !s.doneCompiling).sorted(c)
			.limit(loadingSpeed.getValueI()).collect(Collectors.toList());
		
		for(ChunkScanner scanner : sortedScanners)
			try
			{
				scanner.compileBuffer();
				
			}catch(ConcurrentModificationException e)
			{
				System.out.println(
					"WARNING! ChunkScanner.compileDisplayList(); failed with the following exception:");
				e.printStackTrace();
				
				if(scanner.vertexBuffer != null)
					scanner.vertexBuffer.close();
			}
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		ClientWorld world = MC.world;
		if(MC.player == null || world == null)
			return;
		
		ChunkPos center = ChunkUtils.getAffectedChunk(event.getPacket());
		if(center == null)
			return;
		
		ArrayList<ChunkPos> chunks = new ArrayList<>();
		for(int x = center.x - 1; x <= center.x + 1; x++)
			for(int z = center.z - 1; z <= center.z + 1; z++)
				chunks.add(new ChunkPos(x, z));
			
		for(ChunkPos chunkPos : chunks)
		{
			ChunkScanner scanner = scanners.get(chunkPos);
			if(scanner == null)
				return;
			
			scanner.reset();
			scanner.future = pool.submit(() -> scanner.scan());
		}
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		
		boolean depthTest = this.depthTest.isChecked();
		if(!depthTest)
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		
		for(ChunkScanner scanner : new ArrayList<>(scanners.values()))
		{
			if(scanner.vertexBuffer == null)
				continue;
			
			matrixStack.push();
			RenderUtils.applyRegionalRenderOffset(matrixStack, scanner.chunk);
			
			Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
			Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
			ShaderProgram shader = RenderSystem.getShader();
			scanner.vertexBuffer.bind();
			scanner.vertexBuffer.draw(viewMatrix, projMatrix, shader);
			VertexBuffer.unbind();
			
			matrixStack.pop();
		}
		
		if(!depthTest)
			GL11.glEnable(GL11.GL_DEPTH_TEST);
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private static class ChunkScanner
	{
		public Future<?> future;
		private final Chunk chunk;
		private final DimensionType dimension;
		private final Set<BlockPos> red = new HashSet<>();
		private final Set<BlockPos> yellow = new HashSet<>();
		private VertexBuffer vertexBuffer;
		
		private boolean doneScanning;
		private boolean doneCompiling;
		
		public ChunkScanner(Chunk chunk, DimensionType dimension)
		{
			this.chunk = chunk;
			this.dimension = dimension;
		}
		
		@SuppressWarnings("deprecation")
		private void scan()
		{
			ClientWorld world = MC.world;
			ArrayList<BlockPos> blocks = new ArrayList<>();
			
			int minX = chunk.getPos().getStartX();
			int minY = world.getBottomY();
			int minZ = chunk.getPos().getStartZ();
			int maxX = chunk.getPos().getEndX();
			int maxY = world.getTopY();
			int maxZ = chunk.getPos().getEndZ();
			
			for(int x = minX; x <= maxX; x++)
				for(int y = minY; y <= maxY; y++)
					for(int z = minZ; z <= maxZ; z++)
					{
						BlockPos pos = new BlockPos(x, y, z);
						BlockState state = world.getBlockState(pos);
						
						if(state.blocksMovement())
							continue;
						if(!state.getFluidState().isEmpty())
							continue;
						
						BlockState stateDown = world.getBlockState(pos.down());
						if(!stateDown.allowsSpawning(world, pos.down(),
							EntityType.ZOMBIE))
							continue;
						
						blocks.add(pos);
					}
				
			if(Thread.interrupted())
				return;
			
			red.addAll(blocks.stream()
				.filter(pos -> world.getLightLevel(LightType.BLOCK, pos) < 1)
				.filter(pos -> world.getLightLevel(LightType.SKY, pos) < 8)
				.collect(Collectors.toList()));
			
			if(Thread.interrupted())
				return;
			
			yellow.addAll(blocks.stream().filter(pos -> !red.contains(pos))
				.filter(pos -> world.getLightLevel(LightType.BLOCK, pos) < 1)
				.collect(Collectors.toList()));
			doneScanning = true;
		}
		
		private void compileBuffer()
		{
			RegionPos region = RegionPos.of(chunk.getPos());
			
			if(vertexBuffer != null)
				vertexBuffer.close();
			
			vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tessellator tessellator = RenderSystem.renderThreadTesselator();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			
			bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
				VertexFormats.POSITION_COLOR);
			
			new ArrayList<>(red).stream().filter(Objects::nonNull)
				.map(pos -> new BlockPos(pos.getX() - region.x(), pos.getY(),
					pos.getZ() - region.z()))
				.forEach(pos -> {
					bufferBuilder
						.vertex(pos.getX(), pos.getY() + 0.01, pos.getZ())
						.color(1, 0, 0, 0.5F).next();
					bufferBuilder.vertex(pos.getX() + 1, pos.getY() + 0.01,
						pos.getZ() + 1).color(1, 0, 0, 0.5F).next();
					bufferBuilder
						.vertex(pos.getX() + 1, pos.getY() + 0.01, pos.getZ())
						.color(1, 0, 0, 0.5F).next();
					bufferBuilder
						.vertex(pos.getX(), pos.getY() + 0.01, pos.getZ() + 1)
						.color(1, 0, 0, 0.5F).next();
				});
			
			new ArrayList<>(yellow).stream().filter(Objects::nonNull)
				.map(pos -> new BlockPos(pos.getX() - region.x(), pos.getY(),
					pos.getZ() - region.z()))
				.forEach(pos -> {
					bufferBuilder
						.vertex(pos.getX(), pos.getY() + 0.01, pos.getZ())
						.color(1, 1, 0, 0.5F).next();
					bufferBuilder.vertex(pos.getX() + 1, pos.getY() + 0.01,
						pos.getZ() + 1).color(1, 1, 0, 0.5F).next();
					bufferBuilder
						.vertex(pos.getX() + 1, pos.getY() + 0.01, pos.getZ())
						.color(1, 1, 0, 0.5F).next();
					bufferBuilder
						.vertex(pos.getX(), pos.getY() + 0.01, pos.getZ() + 1)
						.color(1, 1, 0, 0.5F).next();
				});
			
			BuiltBuffer buffer = bufferBuilder.end();
			vertexBuffer.bind();
			vertexBuffer.upload(buffer);
			VertexBuffer.unbind();
			
			doneCompiling = true;
		}
		
		private void reset()
		{
			if(future != null)
				future.cancel(true);
			
			red.clear();
			yellow.clear();
			
			doneScanning = false;
			doneCompiling = false;
		}
	}
}
