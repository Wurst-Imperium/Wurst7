/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.MinPriorityThreadFactory;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"mob spawn esp", "LightLevelESP", "light level esp",
	"LightLevelOverlay", "light level overlay"})
public final class MobSpawnEspHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final EnumSetting<DrawDistance> drawDistance = new EnumSetting<>(
		"Draw distance", DrawDistance.values(), DrawDistance.D9);
	private final SliderSetting loadingSpeed =
		new SliderSetting("Loading speed", 1, 1, 5, 1, v -> (int)v + "x");
	
	private final HashMap<Chunk, ChunkScanner> scanners = new HashMap<>();
	private ExecutorService pool;
	
	public MobSpawnEspHack()
	{
		super("MobSpawnESP",
			"Highlights areas where mobs can spawn.\n" + "\u00a7eyellow\u00a7r"
				+ " - mobs can spawn at night\n" + "\u00a7cred\u00a7r"
				+ " - mobs can always spawn");
		setCategory(Category.RENDER);
		addSetting(drawDistance);
		addSetting(loadingSpeed);
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
			if(scanner.displayList != 0)
				GL11.glDeleteLists(scanner.displayList, 1);
			
			scanners.remove(scanner.chunk);
		}
		
		pool.shutdownNow();
	}
	
	@Override
	public void onUpdate()
	{
		ClientWorld world = MC.world;
		
		BlockPos eyesBlock = new BlockPos(RotationUtils.getEyesPos());
		int chunkX = eyesBlock.getX() >> 4;
		int chunkZ = eyesBlock.getZ() >> 4;
		int chunkRange = drawDistance.getSelected().chunkRange;
		
		ArrayList<Chunk> chunks = new ArrayList<>();
		for(int x = chunkX - chunkRange; x <= chunkX + chunkRange; x++)
			for(int z = chunkZ - chunkRange; z <= chunkZ + chunkRange; z++)
				chunks.add(world.getChunk(x, z));
			
		// create & start scanners for new chunks
		for(Chunk chunk : chunks)
		{
			if(scanners.containsKey(chunk))
				continue;
			
			ChunkScanner scanner = new ChunkScanner(chunk);
			scanners.put(chunk, scanner);
			scanner.future = pool.submit(() -> scanner.scan());
		}
		
		// remove old scanners that are out of range
		for(ChunkScanner scanner : new ArrayList<>(scanners.values()))
		{
			if(Math.abs(scanner.chunk.getPos().x - chunkX) <= chunkRange
				&& Math.abs(scanner.chunk.getPos().z - chunkZ) <= chunkRange)
				continue;
			
			if(!scanner.doneCompiling)
				continue;
			
			if(scanner.displayList != 0)
				GL11.glDeleteLists(scanner.displayList, 1);
			
			if(scanner.future != null)
				scanner.future.cancel(true);
			
			scanners.remove(scanner.chunk);
		}
		
		// generate display lists
		Comparator<ChunkScanner> c =
			Comparator.comparingInt(s -> Math.abs(s.chunk.getPos().x - chunkX)
				+ Math.abs(s.chunk.getPos().z - chunkZ));
		List<ChunkScanner> sortedScanners = scanners.values().stream()
			.filter(s -> s.doneScanning).filter(s -> !s.doneCompiling).sorted(c)
			.limit(loadingSpeed.getValueI()).collect(Collectors.toList());
		
		for(ChunkScanner scanner : sortedScanners)
		{
			if(scanner.displayList == 0)
				scanner.displayList = GL11.glGenLists(1);
			
			try
			{
				scanner.compileDisplayList();
				
			}catch(ConcurrentModificationException e)
			{
				System.out.println(
					"WARNING! ChunkScanner.compileDisplayList(); failed with the following exception:");
				e.printStackTrace();
				
				GL11.glDeleteLists(scanner.displayList, 1);
				scanner.displayList = 0;
			}
		}
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
		
		ArrayList<Chunk> chunks = new ArrayList<>();
		for(int x = chunk.getPos().x - 1; x <= chunk.getPos().x + 1; x++)
			for(int z = chunk.getPos().z - 1; z <= chunk.getPos().z + 1; z++)
				chunks.add(world.getChunk(x, z));
			
		for(Chunk chunk2 : chunks)
		{
			ChunkScanner scanner = scanners.get(chunk2);
			if(scanner == null)
				return;
			
			scanner.reset();
			scanner.future = pool.submit(() -> scanner.scan());
		}
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
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		for(ChunkScanner scanner : new ArrayList<>(scanners.values()))
		{
			if(scanner.displayList == 0)
				continue;
			
			GL11.glCallList(scanner.displayList);
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private class ChunkScanner
	{
		public Future<?> future;
		private final Chunk chunk;
		private final Set<BlockPos> red = new HashSet<>();
		private final Set<BlockPos> yellow = new HashSet<>();
		private int displayList;
		
		private boolean doneScanning;
		private boolean doneCompiling;
		
		public ChunkScanner(Chunk chunk)
		{
			this.chunk = chunk;
		}
		
		private void scan()
		{
			int minX = chunk.getPos().getStartX();
			int minY = 0;
			int minZ = chunk.getPos().getStartZ();
			int maxX = chunk.getPos().getEndX();
			int maxY = 255;
			int maxZ = chunk.getPos().getEndZ();
			
			ClientWorld world = MC.world;
			ArrayList<BlockPos> blocks = new ArrayList<>();
			
			for(int x = minX; x <= maxX; x++)
				for(int y = minY; y <= maxY; y++)
					for(int z = minZ; z <= maxZ; z++)
					{
						BlockPos pos = new BlockPos(x, y, z);
						BlockState state = world.getBlockState(pos);
						
						if(state.getMaterial().blocksMovement())
							continue;
						if(!state.getFluidState().isEmpty())
							continue;
						
						BlockState stateDown = world.getBlockState(pos.down());
						if(!stateDown.isFullOpaque(world, pos.down()))
							continue;
						
						blocks.add(pos);
					}
				
			if(Thread.interrupted())
				return;
			
			red.addAll(blocks.stream()
				.filter(pos -> world.getLightLevel(LightType.BLOCK, pos) < 8)
				.filter(pos -> world.getLightLevel(LightType.SKY, pos) < 8)
				.collect(Collectors.toList()));
			
			if(Thread.interrupted())
				return;
			
			yellow.addAll(blocks.stream().filter(pos -> !red.contains(pos))
				.filter(pos -> world.getLightLevel(LightType.BLOCK, pos) < 8)
				.collect(Collectors.toList()));
			doneScanning = true;
		}
		
		private void compileDisplayList()
		{
			GL11.glNewList(displayList, GL11.GL_COMPILE);
			
			try
			{
				GL11.glColor4f(1, 0, 0, 0.5F);
				GL11.glBegin(GL11.GL_LINES);
				new ArrayList<>(red).forEach(pos -> {
					GL11.glVertex3d(pos.getX(), pos.getY() + 0.01, pos.getZ());
					GL11.glVertex3d(pos.getX() + 1, pos.getY() + 0.01,
						pos.getZ() + 1);
					GL11.glVertex3d(pos.getX() + 1, pos.getY() + 0.01,
						pos.getZ());
					GL11.glVertex3d(pos.getX(), pos.getY() + 0.01,
						pos.getZ() + 1);
				});
				
				GL11.glColor4f(1, 1, 0, 0.5F);
				new ArrayList<>(yellow).forEach(pos -> {
					GL11.glVertex3d(pos.getX(), pos.getY() + 0.01, pos.getZ());
					GL11.glVertex3d(pos.getX() + 1, pos.getY() + 0.01,
						pos.getZ() + 1);
					GL11.glVertex3d(pos.getX() + 1, pos.getY() + 0.01,
						pos.getZ());
					GL11.glVertex3d(pos.getX(), pos.getY() + 0.01,
						pos.getZ() + 1);
				});
				GL11.glEnd();
				
			}finally
			{
				GL11.glEndList();
			}
			
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
		D25("25x25 chunks", 12);
		
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
