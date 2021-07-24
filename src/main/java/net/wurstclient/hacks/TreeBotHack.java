/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathPos;
import net.wurstclient.ai.PathProcessor;
import net.wurstclient.commands.PathCmd;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"tree bot"})
@DontSaveState
public final class TreeBotHack extends Hack
	implements UpdateListener, RenderListener
{
	private static final List<Block> LOG_BLOCKS =
		Arrays.asList(Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG,
			Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG);
	
	private static final List<Block> LEAVES_BLOCKS = Arrays.asList(
		Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
		Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES);
	
	private final SliderSetting range = new SliderSetting("Range",
		"How far TreeBot will reach to break blocks.", 4.5, 1, 6, 0.05,
		ValueDisplay.DECIMAL);
	
	private TreeFinder treeFinder;
	private AngleFinder angleFinder;
	private TreeBotPathProcessor processor;
	private Tree tree;
	
	private BlockPos currentBlock;
	private float progress;
	private float prevProgress;
	
	public TreeBotHack()
	{
		super("TreeBot",
			"An experimental bot that automatically\n"
				+ "walks around and chops down trees.\n"
				+ "Limited to small trees for now.");
		setCategory(Category.BLOCKS);
		addSetting(range);
	}
	
	@Override
	public String getRenderName()
	{
		if(treeFinder != null && !treeFinder.isDone() && !treeFinder.isFailed())
			return getName() + " [Searching]";
		
		if(processor != null && !processor.isDone())
			return getName() + " [Going]";
		
		if(tree != null && !tree.getLogs().isEmpty())
			return getName() + " [Chopping]";
		
		return getName();
	}
	
	@Override
	public void onEnable()
	{
		treeFinder = new TreeFinder();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		PathProcessor.releaseControls();
		treeFinder = null;
		angleFinder = null;
		processor = null;
		
		tree.close();
		tree = null;
		
		if(currentBlock != null)
		{
			IMC.getInteractionManager().setBreakingBlock(true);
			MC.interactionManager.cancelBlockBreaking();
			currentBlock = null;
		}
	}
	
	@Override
	public void onUpdate()
	{
		if(treeFinder != null)
		{
			goToTree();
			return;
		}
		
		if(tree == null)
		{
			treeFinder = new TreeFinder();
			return;
		}
		
		tree.getLogs().removeIf(Predicate.not(this::isLog));
		tree.compileBuffer();
		
		if(tree.getLogs().isEmpty())
		{
			tree.close();
			tree = null;
			return;
		}
		
		if(angleFinder != null)
		{
			goToAngle();
			return;
		}
		
		ArrayList<BlockPos> logsInRange = getLogsInRange();
		
		if(!logsInRange.isEmpty())
		{
			breakBlocks(logsInRange);
			return;
		}
		
		if(angleFinder == null)
			angleFinder = new AngleFinder();
	}
	
	private void goToTree()
	{
		// find path
		if(!treeFinder.isDoneOrFailed())
		{
			PathProcessor.lockControls();
			treeFinder.findPath();
			return;
		}
		
		// process path
		if(processor != null && !processor.isDone())
		{
			processor.goToGoal();
			return;
		}
		
		PathProcessor.releaseControls();
		treeFinder = null;
	}
	
	private void goToAngle()
	{
		// find path
		if(!angleFinder.isDone() && !angleFinder.isFailed())
		{
			PathProcessor.lockControls();
			angleFinder.findPath();
			return;
		}
		
		// process path
		if(processor != null && !processor.isDone())
		{
			processor.goToGoal();
			return;
		}
		
		PathProcessor.releaseControls();
		angleFinder = null;
	}
	
	private ArrayList<BlockPos> getLogsInRange()
	{
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double rangeSq = Math.pow(range.getValue(), 2);
		
		return tree.getLogs().stream()
			.filter(pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(this::hasLineOfSight)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private boolean hasLineOfSight(BlockPos pos)
	{
		return getLineOfSightSide(RotationUtils.getEyesPos(), pos) != null;
	}
	
	private void breakBlocks(ArrayList<BlockPos> blocksInRange)
	{
		for(BlockPos pos : blocksInRange)
			if(breakBlock(pos))
			{
				WURST.getHax().autoToolHack.equipBestTool(pos, false, true,
					false);
				currentBlock = pos;
				break;
			}
		
		if(currentBlock == null)
			MC.interactionManager.cancelBlockBreaking();
		
		if(currentBlock != null && BlockUtils.getHardness(currentBlock) < 1)
		{
			prevProgress = progress;
			progress = IMC.getInteractionManager().getCurrentBreakingProgress();
			
			if(progress < prevProgress)
				prevProgress = progress;
			
		}else
		{
			progress = 1;
			prevProgress = 1;
		}
	}
	
	private boolean breakBlock(BlockPos pos)
	{
		Direction side = getLineOfSightSide(RotationUtils.getEyesPos(), pos);
		
		Vec3d relCenter = BlockUtils.getBoundingBox(pos)
			.offset(-pos.getX(), -pos.getY(), -pos.getZ()).getCenter();
		Vec3d center = Vec3d.of(pos).add(relCenter);
		
		Vec3i dirVec = side.getVector();
		Vec3d relHitVec = new Vec3d(relCenter.x * dirVec.getX(),
			relCenter.y * dirVec.getY(), relCenter.z * dirVec.getZ());
		Vec3d hitVec = center.add(relHitVec);
		
		// face block
		WURST.getRotationFaker().faceVectorPacket(hitVec);
		
		// damage block
		if(!MC.interactionManager.updateBlockBreakingProgress(pos, side))
			return false;
		
		// swing arm
		MC.player.networkHandler
			.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
		
		return true;
	}
	
	private Direction getLineOfSightSide(Vec3d eyesPos, BlockPos pos)
	{
		Direction[] sides = Direction.values();
		
		Vec3d relCenter = BlockUtils.getBoundingBox(pos)
			.offset(-pos.getX(), -pos.getY(), -pos.getZ()).getCenter();
		Vec3d center = Vec3d.of(pos).add(relCenter);
		
		Vec3d[] hitVecs = new Vec3d[sides.length];
		for(int i = 0; i < sides.length; i++)
		{
			Vec3i dirVec = sides[i].getVector();
			Vec3d relHitVec = new Vec3d(relCenter.x * dirVec.getX(),
				relCenter.y * dirVec.getY(), relCenter.z * dirVec.getZ());
			hitVecs[i] = center.add(relHitVec);
		}
		
		double[] distancesSq = new double[sides.length];
		boolean[] linesOfSight = new boolean[sides.length];
		
		double distanceSqToCenter = eyesPos.squaredDistanceTo(center);
		for(int i = 0; i < sides.length; i++)
		{
			distancesSq[i] = eyesPos.squaredDistanceTo(hitVecs[i]);
			
			// no need to raytrace the rear sides,
			// they can't possibly have line of sight
			if(distancesSq[i] >= distanceSqToCenter)
				continue;
			
			linesOfSight[i] = hasLineOfSight(eyesPos, hitVecs[i]);
		}
		
		Direction side = null;
		for(int i = 0; i < sides.length; i++)
		{
			// require line of sight
			if(!linesOfSight[i])
				continue;
			
			// start with the first side that has LOS
			if(side == null)
			{
				side = sides[i];
				continue;
			}
			
			// then pick the closest side
			if(distancesSq[i] < distancesSq[side.ordinal()])
				side = sides[i];
		}
		
		// will be null if no LOS was found
		return side;
	}
	
	private boolean hasLineOfSight(Vec3d from, Vec3d to)
	{
		RaycastContext context =
			new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, MC.player);
		
		return MC.world.raycast(context).getType() == HitResult.Type.MISS;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		if(treeFinder != null)
			drawTreeFinder(matrixStack);
		if(angleFinder != null)
		{
			PathCmd pathCmd = WURST.getCmds().pathCmd;
			angleFinder.renderPath(matrixStack, pathCmd.isDebugMode(),
				pathCmd.isDepthTest());
		}
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		if(tree != null)
			drawTree(matrixStack);
		
		if(currentBlock != null)
			drawCurrentBlock(matrixStack, partialTicks);
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private void drawTreeFinder(MatrixStack matrixStack)
	{
		PathCmd pathCmd = WURST.getCmds().pathCmd;
		treeFinder.renderPath(matrixStack, pathCmd.isDebugMode(),
			pathCmd.isDepthTest());
	}
	
	private void drawTree(MatrixStack matrixStack)
	{
		RenderSystem.setShaderColor(0, 1, 0, 0.5F);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack,
			MC.world.getChunk(tree.getStump()));
		
		Matrix4f viewMatrix = matrixStack.peek().getModel();
		Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
		Shader shader = RenderSystem.getShader();
		
		tree.getVertexBuffer().setShader(viewMatrix, projMatrix, shader);
		
		matrixStack.pop();
	}
	
	private void drawCurrentBlock(MatrixStack matrixStack, float partialTicks)
	{
		matrixStack.push();
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		Box box = new Box(BlockPos.ORIGIN);
		float p = prevProgress + (progress - prevProgress) * partialTicks;
		float red = p * 2F;
		float green = 2 - red;
		
		matrixStack.translate(currentBlock.getX() - regionX,
			currentBlock.getY(), currentBlock.getZ() - regionZ);
		if(p < 1)
		{
			matrixStack.translate(0.5, 0.5, 0.5);
			matrixStack.scale(p, p, p);
			matrixStack.translate(-0.5, -0.5, -0.5);
		}
		
		RenderSystem.setShaderColor(red, green, 0, 0.25F);
		RenderUtils.drawSolidBox(box, matrixStack);
		
		RenderSystem.setShaderColor(red, green, 0, 0.5F);
		RenderUtils.drawOutlinedBox(box, matrixStack);
		
		matrixStack.pop();
	}
	
	private boolean isTreeStump(BlockPos pos)
	{
		if(!isLog(pos))
			return false;
		
		if(isLog(pos.down()))
			return false;
		
		analyzeTree(pos);
		
		// ignore large trees (for now)
		if(tree.getLogs().size() > 6)
			return false;
		
		return true;
	}
	
	private boolean isLog(BlockPos pos)
	{
		return LOG_BLOCKS.contains(BlockUtils.getBlock(pos));
	}
	
	private boolean isLeaves(BlockPos pos)
	{
		return LEAVES_BLOCKS.contains(BlockUtils.getBlock(pos));
	}
	
	private void analyzeTree(BlockPos stump)
	{
		ArrayList<BlockPos> logs = new ArrayList<>(Arrays.asList(stump));
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(Arrays.asList(stump));
		
		for(int i = 0; i < 1024; i++)
		{
			if(queue.isEmpty())
				break;
			
			BlockPos current = queue.pollFirst();
			
			for(BlockPos next : getNeighbors(current))
			{
				if(logs.contains(next))
					continue;
				
				logs.add(next);
				queue.add(next);
			}
		}
		
		tree = new Tree(stump, logs);
	}
	
	private ArrayList<BlockPos> getNeighbors(BlockPos pos)
	{
		return BlockUtils
			.getAllInBoxStream(pos.add(-1, -1, -1), pos.add(1, 1, 1))
			.filter(this::isLog)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private abstract class TreeBotPathFinder extends PathFinder
	{
		public TreeBotPathFinder(BlockPos goal)
		{
			super(goal);
		}
		
		public TreeBotPathFinder(TreeBotPathFinder pathFinder)
		{
			super(pathFinder);
		}
		
		public void findPath()
		{
			think();
			
			if(isDoneOrFailed())
			{
				// set processor
				formatPath();
				processor = new TreeBotPathProcessor(this);
			}
		}
		
		public boolean isDoneOrFailed()
		{
			return isDone() || isFailed();
		}
		
		public abstract void reset();
	}
	
	private class TreeBotPathProcessor
	{
		private final TreeBotPathFinder pathFinder;
		private final PathProcessor processor;
		
		public TreeBotPathProcessor(TreeBotPathFinder pathFinder)
		{
			this.pathFinder = pathFinder;
			processor = pathFinder.getProcessor();
		}
		
		public void goToGoal()
		{
			if(!pathFinder.isPathStillValid(processor.getIndex())
				|| processor.getTicksOffPath() > 20)
			{
				pathFinder.reset();
				return;
			}
			
			ArrayList<BlockPos> leaves = getLeavesInRange(pathFinder.getPath());
			if(!leaves.isEmpty())
			{
				breakBlocks(leaves);
				return;
			}
			
			processor.process();
		}
		
		private ArrayList<BlockPos> getLeavesInRange(List<PathPos> path)
		{
			Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
			double rangeSq = Math.pow(range.getValue(), 2);
			
			path = path.subList(processor.getIndex(), path.size());
			
			return path.stream().flatMap(pos -> Stream.of(pos, pos.up()))
				.distinct().filter(TreeBotHack.this::isLeaves)
				.filter(
					pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
				.filter(TreeBotHack.this::hasLineOfSight)
				.collect(Collectors.toCollection(ArrayList::new));
		}
		
		public final boolean isDone()
		{
			return processor.isDone();
		}
	}
	
	private class TreeFinder extends TreeBotPathFinder
	{
		public TreeFinder()
		{
			super(new BlockPos(WurstClient.MC.player.getPos()));
		}
		
		public TreeFinder(TreeBotPathFinder pathFinder)
		{
			super(pathFinder);
		}
		
		@Override
		protected boolean isMineable(BlockPos pos)
		{
			return isLeaves(pos);
		}
		
		@Override
		protected boolean checkDone()
		{
			return done = isNextToTreeStump(current);
		}
		
		private boolean isNextToTreeStump(PathPos pos)
		{
			return isTreeStump(pos.north()) || isTreeStump(pos.east())
				|| isTreeStump(pos.south()) || isTreeStump(pos.west());
		}
		
		@Override
		public void reset()
		{
			treeFinder = new TreeFinder(treeFinder);
		}
	}
	
	private class AngleFinder extends TreeBotPathFinder
	{
		public AngleFinder()
		{
			super(new BlockPos(WurstClient.MC.player.getPos()));
			setThinkSpeed(512);
			setThinkTime(1);
		}
		
		public AngleFinder(TreeBotPathFinder pathFinder)
		{
			super(pathFinder);
		}
		
		@Override
		protected boolean isMineable(BlockPos pos)
		{
			return isLeaves(pos);
		}
		
		@Override
		protected boolean checkDone()
		{
			return done = hasAngle(current);
		}
		
		private boolean hasAngle(PathPos pos)
		{
			ClientPlayerEntity player = WurstClient.MC.player;
			Vec3d eyes = Vec3d.ofBottomCenter(pos).add(0,
				player.getEyeHeight(player.getPose()), 0);
			
			Vec3d eyesVec = eyes.subtract(0.5, 0.5, 0.5);
			double rangeSq = Math.pow(range.getValue(), 2);
			
			for(BlockPos log : tree.getLogs())
			{
				if(eyesVec.squaredDistanceTo(Vec3d.of(log)) > rangeSq)
					continue;
				
				if(getLineOfSightSide(eyes, log) != null)
					return true;
			}
			
			return false;
		}
		
		@Override
		public void reset()
		{
			angleFinder = new AngleFinder(angleFinder);
		}
	}
	
	private static class Tree implements AutoCloseable
	{
		private final BlockPos stump;
		private final ArrayList<BlockPos> logs;
		private VertexBuffer vertexBuffer;
		
		public Tree(BlockPos stump, ArrayList<BlockPos> logs)
		{
			this.stump = stump;
			this.logs = logs;
			compileBuffer();
		}
		
		public void compileBuffer()
		{
			if(vertexBuffer != null)
				vertexBuffer.close();
			
			vertexBuffer = new VertexBuffer();
			
			int regionX = (stump.getX() >> 9) * 512;
			int regionZ = (stump.getZ() >> 9) * 512;
			
			double boxMin = 1 / 16.0;
			double boxMax = 15 / 16.0;
			Box box = new Box(boxMin, boxMin, boxMin, boxMax, boxMax, boxMax);
			
			BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
			bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
				VertexFormats.POSITION);
			
			RenderUtils.drawCrossBox(
				box.offset(stump).offset(-regionX, 0, -regionZ), bufferBuilder);
			
			for(BlockPos log : logs)
				RenderUtils.drawOutlinedBox(
					box.offset(log).offset(-regionX, 0, -regionZ),
					bufferBuilder);
			
			bufferBuilder.end();
			vertexBuffer.upload(bufferBuilder);
		}
		
		@Override
		public void close()
		{
			vertexBuffer.close();
		}
		
		public BlockPos getStump()
		{
			return stump;
		}
		
		public ArrayList<BlockPos> getLogs()
		{
			return logs;
		}
		
		public VertexBuffer getVertexBuffer()
		{
			return vertexBuffer;
		}
	}
}
