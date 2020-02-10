/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.RayTraceContext;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.hack.HackList;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@DontSaveState
public final class TunnellerHack extends Hack
	implements UpdateListener, RenderListener
{
	private final EnumSetting<TunnelSize> size = new EnumSetting<>(
		"Tunnel size", TunnelSize.values(), TunnelSize.SIZE_3X3);
	
	private final SliderSetting limit = new SliderSetting("Limit",
		"Automatically stops once the tunnel\n"
			+ "has reached the given length.\n\n" + "0 = no limit",
		0, 0, 1000, 1,
		v -> v == 0 ? "disabled" : v == 1 ? "1 block" : (int)v + " blocks");
	
	private final CheckboxSetting torches =
		new CheckboxSetting(
			"Place torches", "Places just enough torches\n"
				+ "to prevent mobs from\n" + "spawning inside the tunnel.",
			false);
	
	private BlockPos start;
	private Direction direction;
	private int length;
	
	private Task[] tasks;
	private int[] displayLists = new int[5];
	
	private BlockPos currentBlock;
	private float progress;
	private float prevProgress;
	
	public TunnellerHack()
	{
		super("Tunneller", "Automatically digs a tunnel.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r Although this bot will try to avoid\n"
			+ "lava and other dangers, there is no guarantee\n"
			+ "that it won't die. Only send it out with gear\n"
			+ "that you don't mind losing.");
		
		setCategory(Category.BLOCKS);
		addSetting(size);
		addSetting(limit);
		addSetting(torches);
	}
	
	@Override
	public String getRenderName()
	{
		if(limit.getValueI() == 0)
			return getName();
		else
			return getName() + " [" + length + "/" + limit.getValueI() + "]";
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
		WURST.getHax().speedNukerHack.setEnabled(false);
		
		// add listeners
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		for(int i = 0; i < displayLists.length; i++)
			displayLists[i] = GL11.glGenLists(1);
		
		ClientPlayerEntity player = MC.player;
		start = new BlockPos(player);
		direction = player.getHorizontalFacing();
		length = 0;
		
		tasks = new Task[]{new DodgeLiquidTask(), new FillInFloorTask(),
			new PlaceTorchTask(), new DigTunnelTask(), new WalkForwardTask()};
		
		updateCyanList();
	}
	
	@Override
	public void onDisable()
	{
		// remove listeners
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		if(currentBlock != null)
		{
			IMC.getInteractionManager().setBreakingBlock(true);
			MC.interactionManager.cancelBlockBreaking();
			currentBlock = null;
		}
		
		for(int displayList : displayLists)
			GL11.glDeleteLists(displayList, 1);
	}
	
	@Override
	public void onUpdate()
	{
		HackList hax = WURST.getHax();
		Hack[] incompatibleHax = {hax.autoToolHack, hax.autoWalkHack,
			hax.blinkHack, hax.flightHack, hax.nukerHack,
			// TODO:
			// hax.nukerLegitHack,
			// hax.speedNukerHack,
			hax.sneakHack};
		for(Hack hack : incompatibleHax)
			hack.setEnabled(false);
		
		if(hax.freecamHack.isEnabled())
			return;
		
		GameOptions gs = MC.options;
		KeyBinding[] bindings = {gs.keyForward, gs.keyBack, gs.keyLeft,
			gs.keyRight, gs.keyJump, gs.keySneak};
		for(KeyBinding binding : bindings)
			binding.setPressed(false);
		
		for(Task task : tasks)
		{
			if(!task.canRun())
				continue;
			
			task.run();
			break;
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
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		for(int displayList : displayLists)
			GL11.glCallList(displayList);
		
		if(currentBlock != null)
		{
			float p = prevProgress + (progress - prevProgress) * partialTicks;
			float red = p * 2F;
			float green = 2 - red;
			
			GL11.glTranslated(currentBlock.getX(), currentBlock.getY(),
				currentBlock.getZ());
			if(p < 1)
			{
				GL11.glTranslated(0.5, 0.5, 0.5);
				GL11.glScaled(p, p, p);
				GL11.glTranslated(-0.5, -0.5, -0.5);
			}
			
			Box box2 = new Box(BlockPos.ORIGIN);
			GL11.glColor4f(red, green, 0, 0.25F);
			RenderUtils.drawSolidBox(box2);
			GL11.glColor4f(red, green, 0, 0.5F);
			RenderUtils.drawOutlinedBox(box2);
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private void updateCyanList()
	{
		GL11.glNewList(displayLists[0], GL11.GL_COMPILE);
		
		GL11.glPushMatrix();
		GL11.glTranslated(start.getX(), start.getY(), start.getZ());
		GL11.glTranslated(0.5, 0.5, 0.5);
		
		GL11.glColor4f(0, 1, 1, 0.5F);
		GL11.glBegin(GL11.GL_LINES);
		RenderUtils.drawNode(new Box(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25));
		GL11.glEnd();
		
		RenderUtils.drawArrow(new Vec3d(direction.getVector()).multiply(0.25),
			new Vec3d(direction.getVector()).multiply(Math.max(0.5, length)));
		
		GL11.glPopMatrix();
		GL11.glEndList();
	}
	
	private BlockPos offset(BlockPos pos, Vec3i vec)
	{
		return pos.offset(direction.rotateYCounterclockwise(), vec.getX())
			.up(vec.getY());
	}
	
	private int getDistance(BlockPos pos1, BlockPos pos2)
	{
		return Math.abs(pos1.getX() - pos2.getX())
			+ Math.abs(pos1.getY() - pos2.getY())
			+ Math.abs(pos1.getZ() - pos2.getZ());
	}
	
	private static abstract class Task
	{
		public abstract boolean canRun();
		
		public abstract void run();
	}
	
	private class DigTunnelTask extends Task
	{
		private int requiredDistance;
		
		@Override
		public boolean canRun()
		{
			BlockPos player = new BlockPos(MC.player);
			BlockPos base = start.offset(direction, length);
			int distance = getDistance(player, base);
			
			if(distance <= 1)
				requiredDistance = size.getSelected().maxRange;
			else if(distance > size.getSelected().maxRange)
				requiredDistance = 1;
			
			return distance <= requiredDistance;
		}
		
		@Override
		public void run()
		{
			BlockPos base = start.offset(direction, length);
			BlockPos from = offset(base, size.getSelected().from);
			BlockPos to = offset(base, size.getSelected().to);
			
			ArrayList<BlockPos> blocks = new ArrayList<>();
			BlockUtils.getAllInBox(from, to).forEach(blocks::add);
			Collections.reverse(blocks);
			
			GL11.glNewList(displayLists[1], GL11.GL_COMPILE);
			Box box = new Box(0.1, 0.1, 0.1, 0.9, 0.9, 0.9);
			GL11.glColor4f(0, 1, 0, 0.5F);
			for(BlockPos pos : blocks)
			{
				GL11.glPushMatrix();
				GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
				RenderUtils.drawOutlinedBox(box);
				GL11.glPopMatrix();
			}
			GL11.glEndList();
			
			currentBlock = null;
			for(BlockPos pos : blocks)
			{
				if(!BlockUtils.canBeClicked(pos))
					continue;
				
				currentBlock = pos;
				break;
			}
			
			if(currentBlock == null)
			{
				MC.interactionManager.cancelBlockBreaking();
				progress = 1;
				prevProgress = 1;
				
				length++;
				if(limit.getValueI() == 0 || length < limit.getValueI())
					updateCyanList();
				else
				{
					ChatUtils.message("Tunnel completed.");
					setEnabled(false);
				}
				
				return;
			}
			
			WURST.getHax().autoToolHack.equipBestTool(currentBlock, false, true,
				false);
			breakBlockSimple(currentBlock);
			
			if(MC.player.abilities.creativeMode
				|| BlockUtils.getHardness(currentBlock) >= 1)
			{
				progress = 1;
				prevProgress = 1;
				return;
			}
			
			prevProgress = progress;
			progress = IMC.getInteractionManager().getCurrentBreakingProgress();
			
			if(progress < prevProgress)
				prevProgress = progress;
		}
	}
	
	private class WalkForwardTask extends Task
	{
		@Override
		public boolean canRun()
		{
			BlockPos player = new BlockPos(MC.player);
			BlockPos base = start.offset(direction, length);
			
			return getDistance(player, base) > 1;
		}
		
		@Override
		public void run()
		{
			BlockPos base = start.offset(direction, length);
			Vec3d vec = new Vec3d(base).add(0.5, 0.5, 0.5);
			WURST.getRotationFaker().faceVectorClientIgnorePitch(vec);
			
			MC.options.keyForward.setPressed(true);
		}
	}
	
	private class FillInFloorTask extends Task
	{
		private final ArrayList<BlockPos> blocks = new ArrayList<>();
		
		@Override
		public boolean canRun()
		{
			BlockPos player = new BlockPos(MC.player);
			BlockPos from = offsetFloor(player, size.getSelected().from);
			BlockPos to = offsetFloor(player, size.getSelected().to);
			
			blocks.clear();
			for(BlockPos pos : BlockUtils.getAllInBox(from, to))
				if(!BlockUtils.getState(pos).isFullCube(MC.world, pos))
					blocks.add(pos);
				
			GL11.glNewList(displayLists[2], GL11.GL_COMPILE);
			Box box = new Box(0.1, 0.1, 0.1, 0.9, 0.9, 0.9);
			GL11.glColor4f(1, 1, 0, 0.5F);
			for(BlockPos pos : blocks)
			{
				GL11.glPushMatrix();
				GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
				RenderUtils.drawOutlinedBox(box);
				GL11.glPopMatrix();
			}
			GL11.glEndList();
			
			return !blocks.isEmpty();
		}
		
		private BlockPos offsetFloor(BlockPos pos, Vec3i vec)
		{
			return pos.offset(direction.rotateYCounterclockwise(), vec.getX())
				.down();
		}
		
		@Override
		public void run()
		{
			MC.options.keySneak.setPressed(true);
			Vec3d velocity = MC.player.getVelocity();
			MC.player.setVelocity(0, velocity.y, 0);
			
			Vec3d eyes = RotationUtils.getEyesPos().add(-0.5, -0.5, -0.5);
			Comparator<BlockPos> comparator =
				Comparator.<BlockPos> comparingDouble(
					p -> eyes.squaredDistanceTo(new Vec3d(p)));
			
			BlockPos pos = blocks.stream().max(comparator).get();
			
			if(!equipSolidBlock(pos))
			{
				ChatUtils.error(
					"Found a hole in the tunnel's floor but don't have any blocks to fill it with.");
				setEnabled(false);
				return;
			}
			
			if(BlockUtils.getState(pos).getMaterial().isReplaceable())
				placeBlockSimple(pos);
			else
			{
				WURST.getHax().autoToolHack.equipBestTool(pos, false, true,
					false);
				breakBlockSimple(pos);
			}
		}
		
		private boolean equipSolidBlock(BlockPos pos)
		{
			for(int slot = 0; slot < 9; slot++)
			{
				// filter out non-block items
				ItemStack stack = MC.player.inventory.getInvStack(slot);
				if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
					continue;
				
				Block block = Block.getBlockFromItem(stack.getItem());
				
				// filter out non-solid blocks
				BlockState state = block.getDefaultState();
				if(!block.isFullOpaque(state, EmptyBlockView.INSTANCE,
					BlockPos.ORIGIN))
					continue;
				
				// filter out blocks that would fall
				if(block instanceof FallingBlock && FallingBlock
					.canFallThrough(BlockUtils.getState(pos.down())))
					continue;
				
				MC.player.inventory.selectedSlot = slot;
				return true;
			}
			
			return false;
		}
	}
	
	private class DodgeLiquidTask extends Task
	{
		private final HashSet<BlockPos> liquids = new HashSet<>();
		private int disableTimer = 60;
		
		@Override
		public boolean canRun()
		{
			if(!liquids.isEmpty())
				return true;
			
			BlockPos base = start.offset(direction, length);
			BlockPos from = offset(base, size.getSelected().from);
			BlockPos to = offset(base, size.getSelected().to);
			int maxY = Math.max(from.getY(), to.getY());
			
			for(BlockPos pos : BlockUtils.getAllInBox(from, to))
			{
				// check current & previous blocks
				int maxOffset = Math.min(size.getSelected().maxRange, length);
				for(int i = 0; i <= maxOffset; i++)
				{
					BlockPos pos2 = pos.offset(direction.getOpposite(), i);
					
					if(!BlockUtils.getState(pos2).getFluidState().isEmpty())
						liquids.add(pos2);
				}
				
				if(BlockUtils.getState(pos).isFullCube(MC.world, pos))
					continue;
				
				// check next blocks
				BlockPos pos3 = pos.offset(direction);
				if(!BlockUtils.getState(pos3).getFluidState().isEmpty())
					liquids.add(pos3);
				
				// check ceiling blocks
				if(pos.getY() == maxY)
				{
					BlockPos pos4 = pos.up();
					
					if(!BlockUtils.getState(pos4).getFluidState().isEmpty())
						liquids.add(pos4);
				}
			}
			
			if(liquids.isEmpty())
				return false;
			
			ChatUtils.error("The tunnel is flooded, cannot continue.");
			
			GL11.glNewList(displayLists[3], GL11.GL_COMPILE);
			Box box = new Box(0.1, 0.1, 0.1, 0.9, 0.9, 0.9);
			GL11.glColor4f(1, 0, 0, 0.5F);
			for(BlockPos pos : liquids)
			{
				GL11.glPushMatrix();
				GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
				RenderUtils.drawOutlinedBox(box);
				GL11.glPopMatrix();
			}
			GL11.glEndList();
			return true;
		}
		
		@Override
		public void run()
		{
			BlockPos player = new BlockPos(MC.player);
			KeyBinding forward = MC.options.keyForward;
			
			Vec3d diffVec = new Vec3d(player.subtract(start));
			Vec3d dirVec = new Vec3d(direction.getVector());
			double dotProduct = diffVec.dotProduct(dirVec);
			
			BlockPos pos1 = start.offset(direction, (int)dotProduct);
			if(!player.equals(pos1))
			{
				WURST.getRotationFaker()
					.faceVectorClientIgnorePitch(toVec3d(pos1));
				forward.setPressed(true);
				return;
			}
			
			BlockPos pos2 = start.offset(direction, Math.max(0, length - 10));
			if(!player.equals(pos2))
			{
				WURST.getRotationFaker()
					.faceVectorClientIgnorePitch(toVec3d(pos2));
				forward.setPressed(true);
				MC.player.setSprinting(true);
				return;
			}
			
			BlockPos pos3 = start.offset(direction, length + 1);
			WURST.getRotationFaker().faceVectorClientIgnorePitch(toVec3d(pos3));
			forward.setPressed(false);
			MC.player.setSprinting(false);
			
			if(disableTimer > 0)
			{
				disableTimer--;
				return;
			}
			
			setEnabled(false);
		}
		
		private Vec3d toVec3d(BlockPos pos)
		{
			return new Vec3d(pos).add(0.5, 0.5, 0.5);
		}
	}
	
	private class PlaceTorchTask extends Task
	{
		private BlockPos lastTorch;
		private BlockPos nextTorch = start;
		
		@Override
		public boolean canRun()
		{
			if(!torches.isChecked())
			{
				lastTorch = null;
				nextTorch = new BlockPos(MC.player);
				GL11.glNewList(displayLists[4], GL11.GL_COMPILE);
				GL11.glEndList();
				return false;
			}
			
			if(lastTorch != null)
				nextTorch = lastTorch.offset(direction,
					size.getSelected().torchDistance);
			
			GL11.glNewList(displayLists[4], GL11.GL_COMPILE);
			GL11.glColor4f(1, 1, 0, 0.5F);
			Vec3d torchVec = new Vec3d(nextTorch).add(0.5, 0, 0.5);
			RenderUtils.drawArrow(torchVec, torchVec.add(0, 0.5, 0));
			GL11.glEndList();
			
			BlockPos base = start.offset(direction, length);
			if(getDistance(start, base) <= getDistance(start, nextTorch))
				return false;
			
			return Blocks.TORCH.canPlaceAt(BlockUtils.getState(nextTorch),
				MC.world, nextTorch);
		}
		
		@Override
		public void run()
		{
			if(!equipTorch())
			{
				ChatUtils.error("Out of torches.");
				setEnabled(false);
				return;
			}
			
			MC.options.keySneak.setPressed(true);
			placeBlockSimple(nextTorch);
			
			if(BlockUtils.getBlock(nextTorch) instanceof TorchBlock)
				lastTorch = nextTorch;
		}
		
		private boolean equipTorch()
		{
			for(int slot = 0; slot < 9; slot++)
			{
				// filter out non-block items
				ItemStack stack = MC.player.inventory.getInvStack(slot);
				if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
					continue;
				
				// filter out non-torch blocks
				Block block = Block.getBlockFromItem(stack.getItem());
				if(!(block instanceof TorchBlock))
					continue;
				
				MC.player.inventory.selectedSlot = slot;
				return true;
			}
			
			return false;
		}
	}
	
	private void placeBlockSimple(BlockPos pos)
	{
		Direction side = null;
		Direction[] sides = Direction.values();
		
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = new Vec3d(pos).add(0.5, 0.5, 0.5);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		Vec3d[] hitVecs = new Vec3d[sides.length];
		for(int i = 0; i < sides.length; i++)
			hitVecs[i] =
				posVec.add(new Vec3d(sides[i].getVector()).multiply(0.5));
		
		for(int i = 0; i < sides.length; i++)
		{
			// check if neighbor can be right clicked
			BlockPos neighbor = pos.offset(sides[i]);
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			// check line of sight
			BlockState neighborState = BlockUtils.getState(neighbor);
			VoxelShape neighborShape =
				neighborState.getOutlineShape(MC.world, neighbor);
			if(MC.world.rayTraceBlock(eyesPos, hitVecs[i], neighbor,
				neighborShape, neighborState) != null)
				continue;
			
			side = sides[i];
			break;
		}
		
		if(side == null)
			for(int i = 0; i < sides.length; i++)
			{
				// check if neighbor can be right clicked
				if(!BlockUtils.canBeClicked(pos.offset(sides[i])))
					continue;
				
				// check if side is facing away from player
				if(distanceSqPosVec > eyesPos.squaredDistanceTo(hitVecs[i]))
					continue;
				
				side = sides[i];
				break;
			}
		
		if(side == null)
			return;
		
		Vec3d hitVec = hitVecs[side.ordinal()];
		
		// face block
		WURST.getRotationFaker().faceVectorPacket(hitVec);
		if(RotationUtils.getAngleToLastReportedLookVec(hitVec) > 1)
			return;
		
		// check timer
		if(IMC.getItemUseCooldown() > 0)
			return;
		
		// place block
		IMC.getInteractionManager().rightClickBlock(pos.offset(side),
			side.getOpposite(), hitVec);
		
		// swing arm
		MC.player.networkHandler
			.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
		
		// reset timer
		IMC.setItemUseCooldown(4);
	}
	
	private boolean breakBlockSimple(BlockPos pos)
	{
		Direction side = null;
		Direction[] sides = Direction.values();
		
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d relCenter = BlockUtils.getBoundingBox(pos)
			.offset(-pos.getX(), -pos.getY(), -pos.getZ()).getCenter();
		Vec3d center = new Vec3d(pos).add(relCenter);
		
		Vec3d[] hitVecs = new Vec3d[sides.length];
		for(int i = 0; i < sides.length; i++)
		{
			Vec3i dirVec = sides[i].getVector();
			Vec3d relHitVec = new Vec3d(relCenter.x * dirVec.getX(),
				relCenter.y * dirVec.getY(), relCenter.z * dirVec.getZ());
			hitVecs[i] = center.add(relHitVec);
		}
		
		for(int i = 0; i < sides.length; i++)
		{
			// check line of sight
			if(MC.world
				.rayTrace(new RayTraceContext(eyesPos, hitVecs[i],
					RayTraceContext.ShapeType.COLLIDER,
					RayTraceContext.FluidHandling.NONE, MC.player))
				.getType() != HitResult.Type.MISS)
				continue;
			
			side = sides[i];
			break;
		}
		
		if(side == null)
		{
			double distanceSqToCenter = eyesPos.squaredDistanceTo(center);
			for(int i = 0; i < sides.length; i++)
			{
				// check if side is facing towards player
				if(eyesPos.squaredDistanceTo(hitVecs[i]) >= distanceSqToCenter)
					continue;
				
				side = sides[i];
				break;
			}
		}
		
		if(side == null)
			throw new RuntimeException(
				"How could none of the sides be facing towards the player?!");
		
		// face block
		WURST.getRotationFaker().faceVectorPacket(hitVecs[side.ordinal()]);
		
		// damage block
		if(!MC.interactionManager.updateBlockBreakingProgress(pos, side))
			return false;
		
		// swing arm
		MC.player.networkHandler
			.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
		
		return true;
	}
	
	private enum TunnelSize
	{
		SIZE_1X2("1x2", new Vec3i(0, 1, 0), new Vec3i(0, 0, 0), 4, 13),
		
		SIZE_3X3("3x3", new Vec3i(1, 2, 0), new Vec3i(-1, 0, 0), 4, 11);
		
		private final String name;
		private final Vec3i from;
		private final Vec3i to;
		private final int maxRange;
		private final int torchDistance;
		
		private TunnelSize(String name, Vec3i from, Vec3i to, int maxRange,
			int torchDistance)
		{
			this.name = name;
			this.from = from;
			this.to = to;
			this.maxRange = maxRange;
			this.torchDistance = torchDistance;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
