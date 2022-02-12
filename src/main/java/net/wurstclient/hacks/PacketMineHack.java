/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.AirBlock;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.BlockBreakingProgressListener;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import net.minecraft.util.math.Box;

import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.stream.Stream;
import net.minecraft.client.util.math.MatrixStack;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.events.RenderListener;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.BlockUtils;

@SearchTags({ "FastMine", "SpeedMine", "SpeedyGonzales",
		"fast mine", "speed mine", "speedy gonzales", "NoBreakDelay",
		"no break delay", "PacketMine", "packet mine" })
public final class PacketMineHack extends Hack
		implements UpdateListener, BlockBreakingProgressListener, PacketOutputListener,
		RenderListener {
	private final class MiningOperation {
		public BlockPos pos;
		public Direction dir;
		public boolean started;
		public boolean abort;

		public MiningOperation(BlockPos pos, Direction dir)
	{
			this.pos = pos;
			this.dir = dir;
			this.started = false;
			this.abort = false;
		}
	}

	private final int miningProcessSize = 2;
	private final MiningOperation[] miningProcess = new MiningOperation[miningProcessSize];
	private final BlockingQueue<MiningOperation> miningQueue = new LinkedBlockingQueue<MiningOperation>();
	private final Set<BlockPos> blockToMine = Collections.synchronizedSet(new HashSet<BlockPos>());
	private final BlockingQueue<Box> blockBoundingBoxes = new LinkedBlockingQueue<Box>();
	private final ArrayList<Box> miningBoundingBoxes = new ArrayList<Box>();
	private VertexBuffer solidBox;
	private VertexBuffer outlinedBox;

	private final BlockPos invalidPos = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
	private final ColorSetting queueColor = new ColorSetting("Queue color",
			"Block in queue will be\n" + "highlighted in this color.", Color.YELLOW);
	private final ColorSetting miningColor = new ColorSetting("Mining color",
			"Block that are being mined will\n" + "be highlighted in this color.", Color.GREEN);

	public PacketMineHack()
	{
		super("PacketMine");
		setCategory(Category.BLOCKS);
	}

	@Override
	protected void onEnable()
	{
		miningQueue.clear();
		blockToMine.clear();
		blockBoundingBoxes.clear();
		for (int i = 0; i < miningProcessSize; i++)
		{
			miningProcess[i] = null;
		}

		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(BlockBreakingProgressListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(RenderListener.class, this);

		Stream.of(solidBox, outlinedBox).filter(Objects::nonNull)
				.forEach(VertexBuffer::close);

		solidBox = new VertexBuffer();
		outlinedBox = new VertexBuffer();

		Box box = new Box(BlockPos.ORIGIN);
		RenderUtils.drawSolidBox(box, solidBox);
		RenderUtils.drawOutlinedBox(box, outlinedBox);
	}

	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(BlockBreakingProgressListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		EVENTS.remove(RenderListener.class, this);

		Stream.of(solidBox, outlinedBox).filter(Objects::nonNull)
				.forEach(VertexBuffer::close);

		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		for (int i = 0; i < 2; i++)
		{
			if (miningProcess[i] == null)
				continue;

			im.sendPlayerActionC2SPacket(
					PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
					miningProcess[i].pos,
					miningProcess[i].dir);
		}
	}

	@Override
	public void onUpdate()
	{
		IMC.getInteractionManager().setBlockHitDelay(0);
		IClientPlayerInteractionManager im = IMC.getInteractionManager();

		for (int i = 0; i < miningProcessSize; i++)
		{
			if (miningProcess[i] == null && miningQueue.size() > 0)
			{
				miningProcess[i] = miningQueue.remove();
				blockBoundingBoxes.remove();
			}
		}

		for (int i = 0; i < miningProcessSize; i++)
		{
			if (miningProcess[i] == null)
				continue;

			if (MC.world.getBlockState(miningProcess[i].pos).getBlock() instanceof AirBlock)
			{
				blockToMine.remove(miningProcess[i].pos);
				miningProcess[i] = null;
				return;
			}
		}
		for (int i = 0; i < miningProcessSize; i++)
		{
			if (miningProcess[i] == null)
				continue;

			if (miningProcess[i].started)
			{
				double distance = MC.player.getEyePos().distanceTo(new Vec3d(
					miningProcess[i].pos.getX(),
					miningProcess[i].pos.getY(),
					miningProcess[i].pos.getZ()
				));
				if (distance > 5)
				{
					miningProcess[i].abort = true;
					im.sendPlayerActionC2SPacket(
						PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
						miningProcess[i].pos,
						miningProcess[i].dir);
					blockToMine.remove(miningProcess[i].pos);
					miningProcess[i] = null;
					break;
				}
				im.sendPlayerActionC2SPacket(
						PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
						miningProcess[i].pos,
						miningProcess[i].dir);
			}
		}
		for (int i = 0; i < miningProcessSize; i++)
		{
			if (miningProcess[i] == null)
				continue;

			if (!miningProcess[i].started)
			{
				im.sendPlayerActionC2SPacket(
						PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
						miningProcess[i].pos,
						miningProcess[i].dir);
				break;
			}
		}
	}

	private final Box scaleBoundingBox(Box bb)
	{
		double minX, minY, minZ;
		double maxX, maxY, maxZ;

		double lenX, lenY, lenZ;

		lenX = bb.getXLength() * 0.05;
		lenY = bb.getYLength() * 0.05;
		lenZ = bb.getZLength() * 0.05;

		minX = bb.getMin(Direction.Axis.X) + lenX;
		minY = bb.getMin(Direction.Axis.Y) + lenY;
		minZ = bb.getMin(Direction.Axis.Z) + lenZ;

		maxX = bb.getMax(Direction.Axis.X) - lenX;
		maxY = bb.getMax(Direction.Axis.Y) - lenY;
		maxZ = bb.getMax(Direction.Axis.Z) - lenZ;

		return new Box(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public void onBlockBreakingProgress(BlockBreakingProgressEvent event)
	{
		BlockPos blockPos = event.getBlockPos();
		Direction direction = event.getDirection();
		if (!blockToMine.contains(blockPos))
		{
			blockToMine.add(blockPos);
			miningQueue.add(new MiningOperation(blockPos, direction));
			blockBoundingBoxes.add(scaleBoundingBox(BlockUtils.getBoundingBox(blockPos)));
		}

		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		if (im.getCurrentBreakingProgress() >= 1)
			return;

		im.setBreakingBlock(true);
		MC.interactionManager.cancelBlockBreaking();
	}

	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if (event.getPacket() instanceof PlayerActionC2SPacket)
		{
			PlayerActionC2SPacket packet = (PlayerActionC2SPacket) event.getPacket();

			if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK ||
				packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK ||
				packet.getAction() == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK)
			{
				BlockPos[] miningPositions = new BlockPos[miningProcessSize];
				for (int i = 0; i < miningProcessSize; i++)
				{
					miningPositions[i] = invalidPos;
				}

				for (int i = 0; i < miningProcessSize; i++)
				{
					if (miningProcess[i] != null)
						miningPositions[i] = miningProcess[i].pos;
				}

				int posIndex = -1;
				for (int i = 0; i < miningProcessSize; i++)
				{
					if (packet.getPos().equals(miningPositions[i]))
					{
						posIndex = i;
						break;
					}
				}
				if (posIndex != -1) {
					if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK)
						miningProcess[posIndex].started = true;
					if (packet.getAction() == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK)
						if (!miningProcess[posIndex].abort)
							event.cancel();
				}
				else
					event.cancel();
			}
		}
	}

	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);

		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);

		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;

		RenderSystem.setShader(GameRenderer::getPositionShader);
		renderBoxes(matrixStack, blockBoundingBoxes.toArray(new Box[0]), queueColor.getColorF(),
				regionX, regionZ);

		miningBoundingBoxes.clear();
		for (int i = 0; i < miningProcessSize; i++)
		{
			if (miningProcess[i] != null &&
				!(MC.world.getBlockState(miningProcess[i].pos).getBlock() instanceof AirBlock))
			{
				miningBoundingBoxes.add(scaleBoundingBox(BlockUtils.getBoundingBox(miningProcess[i].pos)));
			}
		}
		renderBoxes(matrixStack, miningBoundingBoxes.toArray(new Box[0]), miningColor.getColorF(),
				regionX, regionZ);

		matrixStack.pop();

		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}

	private void renderBoxes(MatrixStack matrixStack, Box[] boxes,
			float[] colorF, int regionX, int regionZ)
	{
		for (Box box : boxes)
		{
			matrixStack.push();

			matrixStack.translate(box.minX - regionX, box.minY,
					box.minZ - regionZ);

			matrixStack.scale((float) (box.maxX - box.minX),
					(float) (box.maxY - box.minY), (float) (box.maxZ - box.minZ));

			RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.25F);

			Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
			Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
			Shader shader = RenderSystem.getShader();
			solidBox.setShader(viewMatrix, projMatrix, shader);

			RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
			outlinedBox.setShader(viewMatrix, projMatrix, shader);

			matrixStack.pop();
		}
	}
}
