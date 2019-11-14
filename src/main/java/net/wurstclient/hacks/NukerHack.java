/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.TextFormat;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.wurstclient.Category;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class NukerHack extends Hack
	implements UpdateListener, LeftClickListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		TextFormat.BOLD + "Normal" + TextFormat.RESET
			+ " mode simply breaks everything\n" + "around you.\n"
			+ TextFormat.BOLD + "ID" + TextFormat.RESET
			+ " mode only breaks the selected block\n"
			+ "type. Left-click on a block to select it.\n" + TextFormat.BOLD
			+ "Flat" + TextFormat.RESET
			+ " mode flattens the area around you,\n" + "but won't dig down.\n"
			+ TextFormat.BOLD + "Smash" + TextFormat.RESET
			+ " mode only breaks blocks that\n"
			+ "can be destroyed instantly (e.g. tall grass).",
		Mode.values(), Mode.NORMAL);
	
	private final ArrayDeque<Set<BlockPos>> prevBlocks = new ArrayDeque<>();
	private BlockPos currentBlock;
	private float progress;
	private float prevProgress;
	private String id;
	
	public NukerHack()
	{
		super("Nuker", "Automatically breaks blocks around you.");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(mode);
	}
	
	@Override
	public String getRenderName()
	{
		return mode.getSelected().getRenderName(this);
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().autoMineHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(LeftClickListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(LeftClickListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		if(currentBlock != null)
		{
			IMC.getInteractionManager().setBreakingBlock(true);
			MC.interactionManager.cancelBlockBreaking();
			currentBlock = null;
		}
		
		prevBlocks.clear();
		id = null;
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		currentBlock = null;
		Vec3d eyesPos = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		BlockPos eyesBlock = new BlockPos(RotationUtils.getEyesPos());
		double rangeSq = Math.pow(range.getValue(), 2);
		int blockRange = (int)Math.ceil(range.getValue());
		
		Vec3i rangeVec = new Vec3i(blockRange, blockRange, blockRange);
		BlockPos min = eyesBlock.subtract(rangeVec);
		BlockPos max = eyesBlock.add(rangeVec);
		
		ArrayList<BlockPos> blocks = BlockUtils.getAllInBox(min, max);
		Stream<BlockPos> stream = blocks.parallelStream();
		
		List<BlockPos> blocks2 = stream
			.filter(pos -> eyesPos.squaredDistanceTo(new Vec3d(pos)) <= rangeSq)
			.filter(pos -> BlockUtils.canBeClicked(pos))
			.filter(mode.getSelected().getValidator(this))
			.sorted(Comparator.comparingDouble(
				pos -> eyesPos.squaredDistanceTo(new Vec3d(pos))))
			.collect(Collectors.toList());
		
		if(player.abilities.creativeMode)
		{
			Stream<BlockPos> stream2 = blocks2.parallelStream();
			for(Set<BlockPos> set : prevBlocks)
				stream2 = stream2.filter(pos -> !set.contains(pos));
			List<BlockPos> blocks3 = stream2.collect(Collectors.toList());
			
			prevBlocks.addLast(new HashSet<>(blocks3));
			while(prevBlocks.size() > 5)
				prevBlocks.removeFirst();
			
			if(!blocks3.isEmpty())
				currentBlock = blocks3.get(0);
			
			MC.interactionManager.cancelBlockBreaking();
			progress = 1;
			prevProgress = 1;
			BlockBreaker.breakBlocksWithPacketSpam(blocks3);
			return;
		}
		
		for(BlockPos pos : blocks2)
			if(BlockBreaker.breakOneBlock(pos))
			{
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
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(mode.getSelected() != Mode.ID)
			return;
		
		if(MC.crosshairTarget == null
			|| MC.crosshairTarget.getType() != HitResult.Type.BLOCK)
			return;
		
		BlockHitResult blockHitResult = (BlockHitResult)MC.crosshairTarget;
		BlockPos pos = new BlockPos(blockHitResult.getBlockPos());
		id = BlockUtils.getName(pos);
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		if(currentBlock == null)
			return;
		
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
		
		Box box = new Box(BlockPos.ORIGIN);
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
		
		GL11.glColor4f(red, green, 0, 0.25F);
		RenderUtils.drawSolidBox(box);
		
		GL11.glColor4f(red, green, 0, 0.5F);
		RenderUtils.drawOutlinedBox(box);
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private enum Mode
	{
		NORMAL("Normal", n -> n.getName(), (n, p) -> true),
		
		ID("ID", n -> "IDNuker [" + n.id + "]",
			(n, p) -> BlockUtils.getName(p).equals(n.id)),
		
		FLAT("Flat", n -> "FlatNuker",
			(n, p) -> p.getY() >= MC.player.getPos().getY()),
		
		SMASH("Smash", n -> "SmashNuker",
			(n, p) -> BlockUtils.getHardness(p) >= 1);
		
		private final String name;
		private final Function<NukerHack, String> renderName;
		private final BiPredicate<NukerHack, BlockPos> validator;
		
		private Mode(String name, Function<NukerHack, String> renderName,
			BiPredicate<NukerHack, BlockPos> validator)
		{
			this.name = name;
			this.renderName = renderName;
			this.validator = validator;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		public String getRenderName(NukerHack n)
		{
			return renderName.apply(n);
		}
		
		public Predicate<BlockPos> getValidator(NukerHack n)
		{
			return p -> validator.test(n, p);
		}
	}
}
