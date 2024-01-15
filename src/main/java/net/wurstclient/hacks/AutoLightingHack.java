/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SearchTags({"auto lighting", "AutoLighting", "auto torch", "AutoTorch"})
public final class AutoLightingHack extends Hack
	implements UpdateListener, RenderListener
{
	/**
	 * Maps each light level to the maximum number of blocks it can illuminate.
	 */
	private static final int[] lvl2blocks = new int[16];
	
	/** Maps items to their corresponding light levels. */
	private static final ConcurrentHashMap<Item, Integer> item2lvl =
		new ConcurrentHashMap<>();
	
	static
	{
		Arrays.fill(lvl2blocks, -1);
		item2lvl.put(Items.TORCH, 14);
	}
	
	/** Defines the maximum distance for placing blocks. */
	private final SliderSetting range1 = new SliderSetting("RangePlace", 4.25,
		1, 6, 0.05, SliderSetting.ValueDisplay.DECIMAL);
	
	/**
	 * Determines the distance within which the searches for potential block
	 * placement locations.
	 */
	private final SliderSetting range2 = new SliderSetting("RangeSearch", 8, 1,
		10, 0.05, SliderSetting.ValueDisplay.DECIMAL);
	
	/**
	 * Sets the range to include for calculating the effect of newly placed
	 * light sources.
	 */
	private final SliderSetting range3 = new SliderSetting("RangeInclude", 30,
		10, 15 * 4, 0.05, SliderSetting.ValueDisplay.DECIMAL);
	
	/** Toggles the output of debug messages. */
	private final CheckboxSetting debug =
		new CheckboxSetting("debug", "Output Debug Message", false);
	
	/** Option to compare positions within the placement range. */
	private final CheckboxSetting crp =
		new CheckboxSetting("Compare RP", "Compare Pos with RangePlace", false);
	
	/**
	 * Specifies the radius in front of the player for block placement
	 * calculations.
	 */
	private final SliderSetting frontpath =
		new SliderSetting("Front Path Distance", 3, 0, 6, 0.05,
			SliderSetting.ValueDisplay.DECIMAL);
	
	/** Sets the cooldown time after each torch placement. */
	private final SliderSetting cooldown =
		new SliderSetting("Place Cooldown (s)", 0.25, 1.0 / 2, 2, 1.0 / 20,
			SliderSetting.ValueDisplay.DECIMAL);
	
	/** A executor service for handling asynchronous tasks. */
	private final ExecutorService executorService =
		Executors.newSingleThreadExecutor();
	
	/** Future task for the current search operation for block placement. */
	private Future<BlockPos> currentSearchTask = null;
	
	/** Stores the position where a block needs to be placed next. */
	private @Nullable BlockPos putPos = null;
	
	/** Tracks the current status of the mod (e.g., idle, waiting, placing). */
	private Status status = Status.Idle;
	
	/** Counter for cooldown time when the mod is in idle state. */
	private int idleCooldown = 0;
	
	public AutoLightingHack()
	{
		super("AutoLighting");
		setCategory(Category.BLOCKS);
		
		addSetting(this.range1);
		addSetting(this.range2);
		addSetting(this.range3);
		addSetting(this.cooldown);
		addSetting(this.frontpath);
		addSetting(this.crp);
		addSetting(this.debug);
	}
	
	/**
	 * Calculates the maximum number of blocks that can be illuminated by a
	 * light source with a brightness level of v.
	 *
	 * @return the maximum number of illuminated blocks
	 */
	public static int getMaxLight(int v)
	{
		if(v < lvl2blocks.length)
		{
			if(lvl2blocks[v] > -1)
				return lvl2blocks[v];
			synchronized(lvl2blocks)
			{
				if(lvl2blocks[v] > -1)
					return lvl2blocks[v];
				lvl2blocks[v] = getLight(0, 0, 0, v, null).size();
				return lvl2blocks[v];
			}
		}
		return Integer.MAX_VALUE;
	}
	
	/**
	 * Retrieves the maximum number of blocks that can be illuminated by a light
	 * source with a light level corresponding to item 'v'.
	 *
	 * @return The maximum number of blocks that can be illuminated
	 */
	public static int getMaxLight(Item v)
	{
		return getMaxLight(item2lvl.getOrDefault(v, 14));
	}
	
	/**
	 * Calculates the illuminated blocks in a certain area based on a given
	 * light level and position.
	 * This method can operate in two modes: real and theoretical. In real mode,
	 * it checks actual light levels in the world;
	 * in theoretical mode, it assumes no obstructions to light.
	 *
	 * @param x
	 *            the x-coordinate of the light source
	 * @param y
	 *            the y-coordinate of the light source
	 * @param z
	 *            the z-coordinate of the light source
	 * @param v
	 *            the light level of the source
	 * @param needs
	 *            a set of block positions to specifically check for
	 *            illumination (optional, null for theoretical mode)
	 * @return a list of BlockPos objects representing all blocks illuminated by
	 *         the light source
	 * @implNote This method is marked as pure, meaning it doesn't modify any
	 *           state and returns a new object.
	 */
	@Contract(value = "_,_,_,_,_->new", pure = true)
	private static @NotNull ArrayList<BlockPos> getLight(int x, int y, int z,
		int v, @Nullable Set<BlockPos> needs)
	{
		if(v <= 0)
			return new ArrayList<>();
		final var w = MC.world;
		final boolean is_real = needs != null;
		if(is_real && w == null)
			return new ArrayList<>();
		final var count = new ArrayList<BlockPos>();
		int searched = 0;
		Queue<ValuePos> queue = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		
		final var fst = new ValuePos(x, y, z, v);
		queue.add(fst);
		visited.add(fst.toXYZ());
		
		while(!queue.isEmpty())
		{
			final var point = queue.poll();
			if(is_real && searched++ > getMaxLight(lvl2blocks.length - 1))
				break;
			final var pt = point.toXYZ();
			
			if(is_real)
			{
				if(w.getBlockState(pt).isOpaque())
					continue;
				final var lvl = w.getLightLevel(LightType.BLOCK, pt);
				if(lvl >= point.v)
					continue;
				if(needs.contains(pt))
					count.add(pt);
			}else
			{
				count.add(pt);
			}
			
			if(point.v > 0)
			{
				v = point.v - 1;
				for(int i = 0; i < 6; i++)
				{
					final var to = point.move(i);
					if(visited.contains(to))
						continue;
					visited.add(to);
					queue.add(new ValuePos(to.getX(), to.getY(), to.getZ(), v));
				}
			}
			
		}
		
		return count;
	}
	
	private void message(Object msg, boolean is_debug)
	{
		if(is_debug && !this.debug.isChecked())
			return;
		ChatUtils.message("[%s] %s".formatted(getName(), msg));
	}
	
	private void message(Object msg)
	{
		message(msg, false);
	}
	
	@Override
	public String getRenderName()
	{
		return super.getRenderName() + " [" + this.status.name() + "]";
	}
	
	/**
	 * Searches the player's inventory for a torch and returns its slot index.
	 * Iterates through the first 9 inventory slots (the hotbar) to find a torch
	 * item.
	 *
	 * @return the index of the torch in the player's hotbar, or -1 if no torch
	 *         is found.
	 */
	private int getTorch()
	{
		var inv = Objects.requireNonNull(MC.player.getInventory());
		for(int i = 0; i < 9; i++)
		{
			ItemStack stack = inv.getStack(i);
			if(stack.isEmpty() || !(stack.getItem().equals(Items.TORCH)))
				continue;
			return i;
		}
		return -1;
	}
	
	/**
	 * Attempts to place a block at a predetermined position.
	 * This method first checks if there is a suitable position to place a
	 * block.
	 * If a torch is available in the inventory, it uses it for block placement.
	 * Adjusts player's aim and sends necessary packets to execute the block
	 * placement.
	 *
	 * @return true if a block placement attempt is made, false otherwise.
	 * @implNote Adjusts the player's inventory selection and triggers network
	 *           packets for block placement.
	 */
	private boolean placeBlock()
	{
		if(this.putPos == null)
			return false;
		var newSlot = getTorch();
		if(newSlot < 0)
		{
			this.status = Status.NoTorch;
			return false;
		}
		this.status = Status.Waiting;
		Vec3d eyesPos = RotationUtils.getEyesPos();
		BlockPos down = this.putPos.offset(Direction.DOWN);
		if(!BlockUtils.canBeClicked(down))
			return true;
		Direction up = Direction.DOWN.getOpposite();
		Vec3d hitVec =
			Vec3d.ofCenter(down).add(Vec3d.of(up.getVector()).multiply(0.5));
		var distanceSq = eyesPos.squaredDistanceTo(hitVec);
		if(distanceSq > this.range2.getValueSq())
			return true;
		if(distanceSq > this.range1.getValueSq())
			return false;
		// place block
		message("placeBlock = " + this.putPos, true);
		
		this.status = Status.Putting;
		int oldSlot = MC.player.getInventory().selectedSlot;
		MC.player.getInventory().selectedSlot = newSlot;
		
		var rotation = RotationUtils.getNeededRotations(hitVec);
		MC.player.networkHandler
			.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotation.yaw(),
				rotation.pitch(), MC.player.isOnGround()));
		IMC.getInteractionManager().rightClickBlock(down, up, hitVec);
		MC.player.swingHand(Hand.MAIN_HAND);
		MC.itemUseCooldown = 4;
		
		MC.player.getInventory().selectedSlot = oldSlot;
		
		this.idleCooldown = (int)(this.cooldown.getValue() * 20);
		
		return true;
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		this.status = Status.Idle;
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		if(this.currentSearchTask != null)
			this.currentSearchTask.cancel(true);
		this.currentSearchTask = null;
	}
	
	@Override
	public void onUpdate()
	{
		if(this.currentSearchTask != null && !this.currentSearchTask.isDone())
		{
			this.status = Status.Searching;
			return;
		}
		if(this.idleCooldown > 0)
		{
			this.idleCooldown--;
			this.status = Status.Idle;
			return;
		}
		
		if(this.currentSearchTask != null)
		{
			try
			{
				this.putPos = this.currentSearchTask.get();
			}catch(Exception e)
			{
				e.printStackTrace();
				ChatUtils
					.error("[" + super.getName() + "] Can not search: " + e);
				super.setEnabled(false);
			}
			this.currentSearchTask = null;
		}
		
		if(this.putPos == null)
			this.currentSearchTask = this.executorService.submit(this::search);
		
		if(placeBlock())
			this.putPos = null;
	}
	
	private Vec3d ofBottom(BlockPos place)
	{
		return Vec3d.add(place, 0.5, 0, 0.5);
	}
	
	/**
	 * Searches for the optimal block position to place a light source based on
	 * various criteria.
	 * The method evaluates blocks within a defined range, considering light
	 * levels and entity spawning conditions.
	 * Prioritizes blocks based on their distance, visibility, and potential to
	 * illuminate the area effectively.
	 *
	 * @return the optimal BlockPos for placing a light source, or null if no
	 *         suitable position is found.
	 * @implNote Considers multiple ranges and conditions (e.g., line of sight,
	 *           block state, entity spawning)
	 *           to determine the best placement location.
	 */
	private @Nullable BlockPos search()
	{
		final int level = 14 - 1;
		final double range1 = this.range1.getValue();
		final double range1Sq = MathHelper.square(range1);
		final double range2Sq = this.range2.getValueSq();
		final double range3Sq = this.range3.getValueSq();
		final double frontpath = this.frontpath.getValue();
		final double frontpathSq = MathHelper.square(frontpath);
		Vec3d eyesVec = RotationUtils.getEyesPos();
		ClientWorld world = Objects.requireNonNull(MC.world);
		Stream<BlockPos> stream = //
			BlockUtils
				.getAllInBoxStream(BlockPos.ofFloored(eyesVec),
					range3.getValueCeil())//
				.filter(pos -> eyesVec
					.squaredDistanceTo(Vec3d.ofCenter(pos)) <= range3Sq)//
				.sorted(Comparator.comparingDouble(
					pos -> eyesVec.squaredDistanceTo(Vec3d.ofCenter(pos))))//
				.filter(pos -> {
					BlockState state = world.getBlockState(pos);
					
					if(state.blocksMovement())
						return false;
					if(!state.getFluidState().isEmpty())
						return false;
					
					BlockState stateDown = world.getBlockState(pos.down());
					if(!stateDown.allowsSpawning(world, pos.down(),
						EntityType.ZOMBIE))
						return false;
					
					return world.getLightLevel(LightType.BLOCK, pos) < 1;
				})//
		;
		final var blocksAll = stream.toList();
		final var blocksAllSet = new HashSet<>(blocksAll);
		final var blocksPlaceSet = blocksAll.stream()
			.filter(pos -> eyesVec.squaredDistanceTo(ofBottom(pos)) <= range1Sq)
			.collect(Collectors.toSet());
		if(blocksPlaceSet.isEmpty())
			return null;
		var blocksSearchStream = blocksAll.stream();
		if(frontpath > 0.03)
		{
			// final var currentYaw =
			// MathHelper.wrapDegrees(WurstClient.MC.player.getYaw());
			// var rd = MC.player.getRotationVector();
			// rd = new Vec3d(rd.x, 0, rd.z);
			
			float g = -MC.player.getYaw() * ((float)Math.PI / 180);
			final var rayDir =
				new Vec3d(MathHelper.sin(g), 0, MathHelper.cos(g));
			final var rayStart = RotationUtils.getEyesPos();
			
			blocksSearchStream = blocksSearchStream.filter(pos -> {
				final var btn = ofBottom(pos);
				if(eyesVec.squaredDistanceTo(btn) < range1Sq)
				{
					// final var needed =
					// RotationUtils.getNeededRotations(btn).getYaw();
					// final var diffYaw = MathHelper.wrapDegrees(currentYaw -
					// needed);
					// return Math.abs(diffYaw) < 60;
					return true;
				}
				if(eyesVec.squaredDistanceTo(btn) < range2Sq)
				{
					final var v = btn.subtract(rayStart);
					// Compute point-to-ray projection vector
					double dotProduct = v.dotProduct(rayDir);
					if(dotProduct < 0)
						return false;
					Vec3d projection =
						rayDir.multiply(dotProduct / rayDir.dotProduct(rayDir));
					// Calculate the length of the vertical line (distance from
					// point to ray)
					double distance = v.subtract(projection).lengthSquared();
					return distance < frontpathSq;
				}
				return false;
			});
		}else
		{
			blocksSearchStream = blocksSearchStream.filter(
				pos -> eyesVec.squaredDistanceTo(ofBottom(pos)) < range2Sq);
		}
		final var blocksSearch = blocksSearchStream.toList();
		
		BlockPos bestPos = null;
		
		var bestLights = 0;
		var bestLights_r1 = 0;
		for(final var p : blocksSearch)
		{
			if(!BlockUtils.canBeClicked(p.offset(Direction.DOWN)))
				continue;
			if(!world.getBlockState(p).isAir())
				continue;
			final var light_count =
				getLight(p.getX(), p.getY(), p.getZ(), level, blocksAllSet);
			final var l = light_count.size();
			if(this.crp.isChecked())
			{
				light_count.retainAll(blocksPlaceSet);
				final var l_r1 = light_count.size();
				if(!(l_r1 > bestLights_r1
					|| (l > bestLights && (l_r1 == bestLights_r1))))
					continue;
				bestLights_r1 = l_r1;
			}else if(!(l > bestLights))
				continue;
			bestLights = l;
			bestPos = p;
		}
		
		if(bestPos != null)
			message("Search done " + bestPos, true);
		return bestPos;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		final var pos = this.putPos;
		if(pos == null)
			return;
		
		float scale = 7F / 8F;
		double offset = (1D - scale) / 2D;
		Vec3d eyesPos = RotationUtils.getEyesPos();
		double rangeSq = Math.pow(this.range1.getValue(), 2);
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		RenderSystem.setShaderColor(0F, 0F, 0F, 0.5F);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		matrixStack.push();
		matrixStack.translate(pos.getX() - region.x(), pos.getY(),
			pos.getZ() - region.z());
		matrixStack.translate(offset, offset, offset);
		matrixStack.scale(scale, scale, scale);
		
		Vec3d posVec = Vec3d.ofCenter(pos);
		
		drawBox(matrixStack, eyesPos.squaredDistanceTo(posVec) <= rangeSq);
		
		matrixStack.pop();
		
		matrixStack.pop();
		
		// GL resets
		GL11.glDisable(GL11.GL_BLEND);
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
	
	private void drawBox(MatrixStack matrixStack, boolean isGreen)
	{
		GL11.glDepthMask(false);
		RenderSystem.setShaderColor(isGreen ? 0F : 1F, isGreen ? 1F : 0F, 0F,
			0.15F);
		RenderUtils.drawSolidBox(matrixStack);
		GL11.glDepthMask(true);
		
		RenderSystem.setShaderColor(0F, 0F, 0F, 0.5F);
		RenderUtils.drawOutlinedBox(matrixStack);
	}
	
	private enum Status
	{
		Idle,
		Searching,
		Waiting,
		Putting,
		NoTorch
	}
	
	private record ValuePos(int x, int y, int z, int v)
	{
		
		private static final int[] dx = {-1, 1, 0, 0, 0, 0};
		private static final int[] dy = {0, 0, -1, 1, 0, 0};
		private static final int[] dz = {0, 0, 0, 0, -1, 1};
		
		@Contract(value = " -> new", pure = true)
		public @NotNull BlockPos toXYZ()
		{
			return new BlockPos(this.x, this.y, this.z);
		}
		
		@Contract(value = "_, _, _ -> new", pure = true)
		public @NotNull BlockPos move(int x, int y, int z)
		{
			return new BlockPos(this.x + x, this.y + y, this.z + z);
		}
		
		@Contract(value = "_ -> new", pure = true)
		public @NotNull BlockPos move(int i)
		{
			return move(dx[i], dy[i], dz[i]);
		}
	}
	
}
