/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.stream.StreamSupport;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.wurstclient.Category;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.hack.HackList;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.EasyVertexBuffer;
import net.wurstclient.util.OverlayRenderer;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@DontSaveState
public final class TunnellerHack extends Hack
	implements UpdateListener, RenderListener
{
	private final EnumSetting<TunnelSize> size = new EnumSetting<>(
		"Tunnel size", TunnelSize.values(), TunnelSize.SIZE_3X3);
	
	private final SliderSetting limit = new SliderSetting("Limit",
		"Automatically stops once the tunnel has reached the given length.\n\n"
			+ "0 = no limit",
		0, 0, 1000, 1, ValueDisplay.INTEGER.withSuffix(" blocks")
			.withLabel(1, "1 block").withLabel(0, "disabled"));
	
	private final CheckboxSetting torches = new CheckboxSetting("Place torches",
		"Places just enough torches to prevent mobs from spawning inside the tunnel.",
		false);
	
	private final OverlayRenderer overlay = new OverlayRenderer();
	
	private BlockPos start;
	private Direction direction;
	private int length;
	
	private Task[] tasks;
	private EasyVertexBuffer[] vertexBuffers = new EasyVertexBuffer[5];
	
	private BlockPos currentBlock;
	private BlockPos lastTorch;
	private BlockPos nextTorch;
	
	public TunnellerHack()
	{
		super("Tunneller");
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
		return getName() + " [" + length + "/" + limit.getValueI() + "]";
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().followHack.setEnabled(false);
		WURST.getHax().instantBunkerHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().speedNukerHack.setEnabled(false);
		WURST.getHax().veinMinerHack.setEnabled(false);
		
		// add listeners
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		LocalPlayer player = MC.player;
		start = BlockPos.containing(player.position());
		direction = player.getDirection();
		length = 0;
		lastTorch = null;
		nextTorch = start;
		
		tasks = new Task[]{new DodgeLiquidTask(), new FillInFloorTask(),
			new PlaceTorchTask(), new WaitForFallingBlocksTask(),
			new DigTunnelTask(), new WalkForwardTask()};
		
		updateCyanBuffer();
	}
	
	@Override
	protected void onDisable()
	{
		// remove listeners
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		overlay.resetProgress();
		if(currentBlock != null)
		{
			MC.gameMode.isDestroying = true;
			MC.gameMode.stopDestroyBlock();
			currentBlock = null;
		}
		
		for(int i = 0; i < vertexBuffers.length; i++)
		{
			if(vertexBuffers[i] == null)
				continue;
			
			vertexBuffers[i].close();
			vertexBuffers[i] = null;
		}
	}
	
	@Override
	public void onUpdate()
	{
		HackList hax = WURST.getHax();
		Hack[] incompatibleHax = {hax.autoSwitchHack, hax.autoToolHack,
			hax.autoWalkHack, hax.blinkHack, hax.flightHack,
			hax.scaffoldWalkHack, hax.sneakHack};
		for(Hack hack : incompatibleHax)
			hack.setEnabled(false);
		
		if(hax.freecamHack.isEnabled() || hax.remoteViewHack.isEnabled())
			return;
		
		Options gs = MC.options;
		KeyMapping[] bindings = {gs.keyUp, gs.keyDown, gs.keyLeft, gs.keyRight,
			gs.keyJump, gs.keyShift};
		for(KeyMapping binding : bindings)
			binding.setDown(false);
		
		for(Task task : tasks)
		{
			if(!task.canRun())
				continue;
			
			task.run();
			break;
		}
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		matrixStack.pushPose();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		for(EasyVertexBuffer buffer : vertexBuffers)
		{
			if(buffer == null)
				continue;
			
			buffer.draw(matrixStack, WurstRenderLayers.ESP_LINES);
		}
		
		matrixStack.popPose();
		
		overlay.render(matrixStack, partialTicks, currentBlock);
	}
	
	private void updateCyanBuffer()
	{
		if(vertexBuffers[0] != null)
			vertexBuffers[0].close();
		
		RegionPos region = RenderUtils.getCameraRegion();
		Vec3 offset = Vec3.atCenterOf(start).subtract(region.toVec3d());
		int cyan = 0x8000FFFF;
		
		AABB nodeBox =
			new AABB(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25).move(offset);
		Vec3 dirVec = Vec3.atLowerCornerOf(direction.getUnitVec3i());
		Vec3 arrowStart = dirVec.scale(0.25).add(offset);
		Vec3 arrowEnd = dirVec.scale(Math.max(0.5, length)).add(offset);
		
		vertexBuffers[0] = EasyVertexBuffer.createAndUpload(Mode.LINES,
			DefaultVertexFormat.POSITION_COLOR_NORMAL, buffer -> {
				RenderUtils.drawNode(buffer, nodeBox, cyan);
				RenderUtils.drawArrow(buffer, arrowStart, arrowEnd, cyan, 0.1F);
			});
	}
	
	private BlockPos offset(BlockPos pos, Vec3i vec)
	{
		return pos.relative(direction.getCounterClockWise(), vec.getX())
			.above(vec.getY());
	}
	
	private int getDistance(BlockPos pos1, BlockPos pos2)
	{
		return Math.abs(pos1.getX() - pos2.getX())
			+ Math.abs(pos1.getY() - pos2.getY())
			+ Math.abs(pos1.getZ() - pos2.getZ());
	}
	
	/**
	 * Returns all block positions in the given box, in the order that Tunneller
	 * should mine them (left to right, top to bottom, front to back).
	 */
	public ArrayList<BlockPos> getAllInBox(BlockPos from, BlockPos to)
	{
		ArrayList<BlockPos> blocks = new ArrayList<>();
		
		Direction front = direction;
		Direction left = front.getCounterClockWise();
		
		int fromFront =
			from.getX() * front.getStepX() + from.getZ() * front.getStepZ();
		int toFront =
			to.getX() * front.getStepX() + to.getZ() * front.getStepZ();
		int fromLeft =
			from.getX() * left.getStepX() + from.getZ() * left.getStepZ();
		int toLeft = to.getX() * left.getStepX() + to.getZ() * left.getStepZ();
		
		int minFront = Math.min(fromFront, toFront);
		int maxFront = Math.max(fromFront, toFront);
		int minY = Math.min(from.getY(), to.getY());
		int maxY = Math.max(from.getY(), to.getY());
		int minLeft = Math.min(fromLeft, toLeft);
		int maxLeft = Math.max(fromLeft, toLeft);
		
		for(int f = minFront; f <= maxFront; f++)
			for(int y = maxY; y >= minY; y--)
				for(int l = maxLeft; l >= minLeft; l--)
				{
					int x = f * front.getStepX() + l * left.getStepX();
					int z = f * front.getStepZ() + l * left.getStepZ();
					blocks.add(new BlockPos(x, y, z));
				}
			
		return blocks;
	}
	
	private static abstract class Task
	{
		public abstract boolean canRun();
		
		public abstract void run();
	}
	
	private class DigTunnelTask extends Task
	{
		private int maxDistance;
		
		@Override
		public boolean canRun()
		{
			BlockPos player = BlockPos.containing(MC.player.position());
			BlockPos base = start.relative(direction, length);
			int distance = getDistance(player, base);
			
			if(distance <= 1)
				maxDistance = size.getSelected().maxRange;
			else if(distance > size.getSelected().maxRange)
				maxDistance = 1;
			
			return distance <= maxDistance;
		}
		
		@Override
		public void run()
		{
			BlockPos player = BlockPos.containing(MC.player.position());
			BlockPos base = start.relative(direction, length);
			BlockPos from = offset(player, size.getSelected().from);
			BlockPos to = offset(base, size.getSelected().to);
			
			ArrayList<BlockPos> blocks = new ArrayList<>();
			getAllInBox(from, to).forEach(blocks::add);
			
			RegionPos region = RenderUtils.getCameraRegion();
			AABB blockBox = new AABB(BlockPos.ZERO).deflate(0.1)
				.move(region.negate().toVec3d());
			
			currentBlock = null;
			ArrayList<AABB> boxes = new ArrayList<>();
			for(BlockPos pos : blocks)
			{
				if(!BlockUtils.canBeClicked(pos))
					continue;
				
				if((pos.equals(nextTorch) || pos.equals(lastTorch))
					&& BlockUtils.getBlock(pos) instanceof TorchBlock)
					continue;
				
				if(currentBlock == null)
					currentBlock = pos;
				
				boxes.add(blockBox.move(pos));
			}
			
			if(vertexBuffers[1] != null)
			{
				vertexBuffers[1].close();
				vertexBuffers[1] = null;
			}
			
			int green = 0x8000FF00;
			if(!boxes.isEmpty())
				vertexBuffers[1] = EasyVertexBuffer.createAndUpload(Mode.LINES,
					DefaultVertexFormat.POSITION_COLOR_NORMAL, buffer -> {
						for(AABB box : boxes)
							RenderUtils.drawOutlinedBox(buffer, box, green);
					});
			
			if(currentBlock == null)
			{
				MC.gameMode.stopDestroyBlock();
				overlay.resetProgress();
				
				length++;
				if(limit.getValueI() == 0 || length < limit.getValueI())
					updateCyanBuffer();
				else
				{
					ChatUtils.message("Tunnel completed.");
					setEnabled(false);
				}
				
				return;
			}
			
			WURST.getHax().autoToolHack.equipBestTool(currentBlock, false, true,
				0);
			breakBlock(currentBlock);
			
			if(MC.player.getAbilities().instabuild
				|| BlockUtils.getHardness(currentBlock) >= 1)
			{
				overlay.resetProgress();
				return;
			}
			
			overlay.updateProgress();
		}
	}
	
	private class WalkForwardTask extends Task
	{
		@Override
		public boolean canRun()
		{
			BlockPos player = BlockPos.containing(MC.player.position());
			BlockPos base = start.relative(direction, length);
			
			return getDistance(player, base) > 1;
		}
		
		@Override
		public void run()
		{
			BlockPos base = start.relative(direction, length);
			Vec3 vec = Vec3.atCenterOf(base);
			WURST.getRotationFaker().faceVectorClientIgnorePitch(vec);
			
			MC.options.keyUp.setDown(true);
		}
	}
	
	private class FillInFloorTask extends Task
	{
		private final ArrayList<BlockPos> blocks = new ArrayList<>();
		
		@Override
		public boolean canRun()
		{
			BlockPos player = BlockPos.containing(MC.player.position());
			BlockPos from = offsetFloor(player, size.getSelected().from);
			BlockPos to = offsetFloor(player, size.getSelected().to);
			
			blocks.clear();
			for(BlockPos pos : BlockUtils.getAllInBox(from, to))
				if(!BlockUtils.getState(pos).isCollisionShapeFullBlock(MC.level,
					pos))
					blocks.add(pos);
				
			if(vertexBuffers[2] != null)
			{
				vertexBuffers[2].close();
				vertexBuffers[2] = null;
			}
			
			if(!blocks.isEmpty())
			{
				RegionPos region = RenderUtils.getCameraRegion();
				AABB box = new AABB(BlockPos.ZERO).deflate(0.1)
					.move(region.negate().toVec3d());
				
				int yellow = 0x80FFFF00;
				vertexBuffers[2] = EasyVertexBuffer.createAndUpload(Mode.LINES,
					DefaultVertexFormat.POSITION_COLOR_NORMAL, buffer -> {
						for(BlockPos pos : blocks)
							RenderUtils.drawOutlinedBox(buffer, box.move(pos),
								yellow);
					});
				
				return true;
			}
			
			return false;
		}
		
		private BlockPos offsetFloor(BlockPos pos, Vec3i vec)
		{
			return pos.relative(direction.getCounterClockWise(), vec.getX())
				.below();
		}
		
		@Override
		public void run()
		{
			MC.options.keyShift.setDown(true);
			Vec3 velocity = MC.player.getDeltaMovement();
			MC.player.setDeltaMovement(0, velocity.y, 0);
			
			Vec3 eyes = RotationUtils.getEyesPos().add(-0.5, -0.5, -0.5);
			Comparator<BlockPos> comparator =
				Comparator.<BlockPos> comparingDouble(
					p -> eyes.distanceToSqr(Vec3.atLowerCornerOf(p)));
			
			BlockPos pos = blocks.stream().max(comparator).get();
			
			if(!equipSolidBlock(pos))
			{
				ChatUtils.error(
					"Found a hole in the tunnel's floor but don't have any blocks to fill it with.");
				setEnabled(false);
				return;
			}
			
			if(BlockUtils.getState(pos).canBeReplaced())
				placeBlockSimple(pos);
			else
			{
				WURST.getHax().autoToolHack.equipBestTool(pos, false, true, 0);
				breakBlock(pos);
			}
		}
		
		private boolean equipSolidBlock(BlockPos pos)
		{
			for(int slot = 0; slot < 9; slot++)
			{
				// filter out non-block items
				ItemStack stack = MC.player.getInventory().getItem(slot);
				if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
					continue;
				
				Block block = Block.byItem(stack.getItem());
				
				// filter out non-solid blocks
				BlockState state = block.defaultBlockState();
				if(!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE,
					BlockPos.ZERO))
					continue;
				
				// filter out blocks that would fall
				if(block instanceof FallingBlock
					&& FallingBlock.isFree(BlockUtils.getState(pos.below())))
					continue;
				
				MC.player.getInventory().setSelectedSlot(slot);
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
			
			BlockPos base = start.relative(direction, length);
			BlockPos from = offset(base, size.getSelected().from);
			BlockPos to = offset(base, size.getSelected().to);
			int maxY = Math.max(from.getY(), to.getY());
			
			for(BlockPos pos : BlockUtils.getAllInBox(from, to))
			{
				// check current & previous blocks
				int maxOffset = Math.min(size.getSelected().maxRange, length);
				for(int i = 0; i <= maxOffset; i++)
				{
					BlockPos pos2 = pos.relative(direction.getOpposite(), i);
					
					if(!BlockUtils.getState(pos2).getFluidState().isEmpty())
						liquids.add(pos2);
				}
				
				if(BlockUtils.getState(pos).isCollisionShapeFullBlock(MC.level,
					pos))
					continue;
				
				// check next blocks
				BlockPos pos3 = pos.relative(direction);
				if(!BlockUtils.getState(pos3).getFluidState().isEmpty())
					liquids.add(pos3);
				
				// check ceiling blocks
				if(pos.getY() == maxY)
				{
					BlockPos pos4 = pos.above();
					
					if(!BlockUtils.getState(pos4).getFluidState().isEmpty())
						liquids.add(pos4);
				}
			}
			
			if(liquids.isEmpty())
				return false;
			
			ChatUtils.error("The tunnel is flooded, cannot continue.");
			
			if(vertexBuffers[3] != null)
			{
				vertexBuffers[3].close();
				vertexBuffers[3] = null;
			}
			
			if(!liquids.isEmpty())
			{
				RegionPos region = RenderUtils.getCameraRegion();
				AABB box = new AABB(BlockPos.ZERO).deflate(0.1)
					.move(region.negate().toVec3d());
				
				int red = 0x80FF0000;
				vertexBuffers[3] = EasyVertexBuffer.createAndUpload(Mode.LINES,
					DefaultVertexFormat.POSITION_COLOR_NORMAL, buffer -> {
						for(BlockPos pos : liquids)
							RenderUtils.drawOutlinedBox(buffer, box.move(pos),
								red);
					});
			}
			
			return true;
		}
		
		@Override
		public void run()
		{
			BlockPos player = BlockPos.containing(MC.player.position());
			KeyMapping forward = MC.options.keyUp;
			
			Vec3 diffVec = Vec3.atLowerCornerOf(player.subtract(start));
			Vec3 dirVec = Vec3.atLowerCornerOf(direction.getUnitVec3i());
			double dotProduct = diffVec.dot(dirVec);
			
			BlockPos pos1 = start.relative(direction, (int)dotProduct);
			if(!player.equals(pos1))
			{
				WURST.getRotationFaker()
					.faceVectorClientIgnorePitch(toVec3d(pos1));
				forward.setDown(true);
				return;
			}
			
			BlockPos pos2 = start.relative(direction, Math.max(0, length - 10));
			if(!player.equals(pos2))
			{
				WURST.getRotationFaker()
					.faceVectorClientIgnorePitch(toVec3d(pos2));
				forward.setDown(true);
				MC.player.setSprinting(true);
				return;
			}
			
			BlockPos pos3 = start.relative(direction, length + 1);
			WURST.getRotationFaker().faceVectorClientIgnorePitch(toVec3d(pos3));
			forward.setDown(false);
			MC.player.setSprinting(false);
			
			if(disableTimer > 0)
			{
				disableTimer--;
				return;
			}
			
			setEnabled(false);
		}
		
		private Vec3 toVec3d(BlockPos pos)
		{
			return Vec3.atCenterOf(pos);
		}
	}
	
	private class PlaceTorchTask extends Task
	{
		@Override
		public boolean canRun()
		{
			if(vertexBuffers[4] != null)
			{
				vertexBuffers[4].close();
				vertexBuffers[4] = null;
			}
			
			if(!torches.isChecked())
			{
				lastTorch = null;
				nextTorch = BlockPos.containing(MC.player.position());
				return false;
			}
			
			if(BlockUtils.getBlock(nextTorch) instanceof TorchBlock)
				lastTorch = nextTorch;
			
			if(lastTorch != null)
				nextTorch = lastTorch.relative(direction,
					size.getSelected().torchDistance);
			
			RegionPos region = RenderUtils.getCameraRegion();
			Vec3 torchVec =
				Vec3.atBottomCenterOf(nextTorch).subtract(region.toVec3d());
			
			int yellow = 0x80FFFF00;
			vertexBuffers[4] = EasyVertexBuffer.createAndUpload(Mode.LINES,
				DefaultVertexFormat.POSITION_COLOR_NORMAL, buffer -> {
					RenderUtils.drawArrow(buffer, torchVec,
						torchVec.add(0, 0.5, 0), yellow, 0.1F);
				});
			
			BlockPos player = BlockPos.containing(MC.player.position());
			if(getDistance(player, nextTorch) > 4)
				return false;
			
			BlockState state = BlockUtils.getState(nextTorch);
			if(!state.canBeReplaced())
				return false;
			
			return Blocks.TORCH.defaultBlockState().canSurvive(MC.level,
				nextTorch);
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
			
			MC.options.keyShift.setDown(true);
			placeBlockSimple(nextTorch);
		}
		
		private boolean equipTorch()
		{
			for(int slot = 0; slot < 9; slot++)
			{
				// filter out non-block items
				ItemStack stack = MC.player.getInventory().getItem(slot);
				if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
					continue;
				
				// filter out non-torch blocks
				Block block = Block.byItem(stack.getItem());
				if(!(block instanceof TorchBlock))
					continue;
				
				MC.player.getInventory().setSelectedSlot(slot);
				return true;
			}
			
			return false;
		}
	}
	
	private static class WaitForFallingBlocksTask extends Task
	{
		@Override
		public boolean canRun()
		{
			// check for nearby falling blocks
			return StreamSupport
				.stream(MC.level.entitiesForRendering().spliterator(), false)
				.filter(FallingBlockEntity.class::isInstance)
				.anyMatch(e -> MC.player.distanceToSqr(e) < 36);
		}
		
		@Override
		public void run()
		{
			// just wait for them to land
		}
	}
	
	private void placeBlockSimple(BlockPos pos)
	{
		Direction side = null;
		Direction[] sides = Direction.values();
		
		Vec3 eyesPos = RotationUtils.getEyesPos();
		Vec3 posVec = Vec3.atCenterOf(pos);
		double distanceSqPosVec = eyesPos.distanceToSqr(posVec);
		
		Vec3[] hitVecs = new Vec3[sides.length];
		for(int i = 0; i < sides.length; i++)
			hitVecs[i] = posVec
				.add(Vec3.atLowerCornerOf(sides[i].getUnitVec3i()).scale(0.5));
		
		for(int i = 0; i < sides.length; i++)
		{
			// check if neighbor can be right clicked
			BlockPos neighbor = pos.relative(sides[i]);
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			// check line of sight
			BlockState neighborState = BlockUtils.getState(neighbor);
			VoxelShape neighborShape =
				neighborState.getShape(MC.level, neighbor);
			if(MC.level.clipWithInteractionOverride(eyesPos, hitVecs[i],
				neighbor, neighborShape, neighborState) != null)
				continue;
			
			side = sides[i];
			break;
		}
		
		if(side == null)
			for(int i = 0; i < sides.length; i++)
			{
				// check if neighbor can be right clicked
				if(!BlockUtils.canBeClicked(pos.relative(sides[i])))
					continue;
				
				// check if side is facing away from player
				if(distanceSqPosVec > eyesPos.distanceToSqr(hitVecs[i]))
					continue;
				
				side = sides[i];
				break;
			}
		
		if(side == null)
			return;
		
		Vec3 hitVec = hitVecs[side.ordinal()];
		
		// face block
		WURST.getRotationFaker().faceVectorPacket(hitVec);
		if(RotationUtils.getAngleToLastReportedLookVec(hitVec) > 1)
			return;
		
		// check timer
		if(MC.rightClickDelay > 0)
			return;
		
		// place block
		IMC.getInteractionManager().rightClickBlock(pos.relative(side),
			side.getOpposite(), hitVec);
		
		// swing arm
		SwingHand.SERVER.swing(InteractionHand.MAIN_HAND);
		
		// reset timer
		MC.rightClickDelay = 4;
	}
	
	private boolean breakBlock(BlockPos pos)
	{
		Direction[] sides = Direction.values();
		
		Vec3 eyesPos = RotationUtils.getEyesPos();
		Vec3 relCenter = BlockUtils.getBoundingBox(pos)
			.move(-pos.getX(), -pos.getY(), -pos.getZ()).getCenter();
		Vec3 center = Vec3.atLowerCornerOf(pos).add(relCenter);
		
		Vec3[] hitVecs = new Vec3[sides.length];
		for(int i = 0; i < sides.length; i++)
		{
			Vec3i dirVec = sides[i].getUnitVec3i();
			Vec3 relHitVec = new Vec3(relCenter.x * dirVec.getX(),
				relCenter.y * dirVec.getY(), relCenter.z * dirVec.getZ());
			hitVecs[i] = center.add(relHitVec);
		}
		
		double[] distancesSq = new double[sides.length];
		boolean[] linesOfSight = new boolean[sides.length];
		
		double distanceSqToCenter = eyesPos.distanceToSqr(center);
		for(int i = 0; i < sides.length; i++)
		{
			distancesSq[i] = eyesPos.distanceToSqr(hitVecs[i]);
			
			// no need to raytrace the rear sides,
			// they can't possibly have line of sight
			if(distancesSq[i] >= distanceSqToCenter)
				continue;
			
			linesOfSight[i] = BlockUtils.hasLineOfSight(eyesPos, hitVecs[i]);
		}
		
		Direction side = sides[0];
		for(int i = 1; i < sides.length; i++)
		{
			int bestSide = side.ordinal();
			
			// prefer sides with LOS
			if(!linesOfSight[bestSide] && linesOfSight[i])
			{
				side = sides[i];
				continue;
			}
			
			// then pick the closest side
			if(distancesSq[i] < distancesSq[bestSide])
				side = sides[i];
		}
		
		// face block
		WURST.getRotationFaker().faceVectorPacket(hitVecs[side.ordinal()]);
		
		// damage block
		if(!MC.gameMode.continueDestroyBlock(pos, side))
			return false;
		
		// swing arm
		SwingHand.SERVER.swing(InteractionHand.MAIN_HAND);
		
		return true;
	}
	
	private enum TunnelSize
	{
		SIZE_1X2("1x2", new Vec3i(0, 1, 0), new Vec3i(0, 0, 0), 4, 13),
		SIZE_1X3("1x3", new Vec3i(0, 2, 0), new Vec3i(0, 0, 0), 4, 13),
		SIZE_1X4("1x4", new Vec3i(0, 3, 0), new Vec3i(0, 0, 0), 4, 13),
		SIZE_1X5("1x5", new Vec3i(0, 4, 0), new Vec3i(0, 0, 0), 3, 13),
		
		SIZE_2X2("2x2", new Vec3i(1, 1, 0), new Vec3i(0, 0, 0), 4, 11),
		SIZE_2X3("2x3", new Vec3i(1, 2, 0), new Vec3i(0, 0, 0), 4, 11),
		SIZE_2X4("2x4", new Vec3i(1, 3, 0), new Vec3i(0, 0, 0), 4, 11),
		SIZE_2X5("2x5", new Vec3i(1, 4, 0), new Vec3i(0, 0, 0), 3, 11),
		
		SIZE_3X2("3x2", new Vec3i(1, 1, 0), new Vec3i(-1, 0, 0), 4, 11),
		SIZE_3X3("3x3", new Vec3i(1, 2, 0), new Vec3i(-1, 0, 0), 4, 11),
		SIZE_3X4("3x4", new Vec3i(1, 3, 0), new Vec3i(-1, 0, 0), 4, 11),
		SIZE_3X5("3x5", new Vec3i(1, 4, 0), new Vec3i(-1, 0, 0), 3, 11),
		
		SIZE_4X2("4x2", new Vec3i(2, 1, 0), new Vec3i(-1, 0, 0), 4, 9),
		SIZE_4X3("4x3", new Vec3i(2, 2, 0), new Vec3i(-1, 0, 0), 4, 9),
		SIZE_4X4("4x4", new Vec3i(2, 3, 0), new Vec3i(-1, 0, 0), 4, 9),
		SIZE_4X5("4x5", new Vec3i(2, 4, 0), new Vec3i(-1, 0, 0), 3, 9),
		
		SIZE_5X2("5x2", new Vec3i(2, 1, 0), new Vec3i(-2, 0, 0), 4, 9),
		SIZE_5X3("5x3", new Vec3i(2, 2, 0), new Vec3i(-2, 0, 0), 4, 9),
		SIZE_5X4("5x4", new Vec3i(2, 3, 0), new Vec3i(-2, 0, 0), 4, 9),
		SIZE_5X5("5x5", new Vec3i(2, 4, 0), new Vec3i(-2, 0, 0), 3, 9);
		
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
