/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.HandleBlockBreakingListener;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.nukers.NukerModeSetting;
import net.wurstclient.hacks.nukers.NukerModeSetting.NukerMode;
import net.wurstclient.hacks.nukers.NukerMultiIdListSetting;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockBreaker.BlockBreakingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.OverlayRenderer;
import net.wurstclient.util.RotationUtils;

@SearchTags({"LegitNuker", "nuker legit", "legit nuker"})
public final class NukerLegitHack extends Hack implements UpdateListener,
	LeftClickListener, HandleBlockBreakingListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 4.25, 1, 4.5, 0.05, ValueDisplay.DECIMAL);
	
	private final NukerModeSetting mode = new NukerModeSetting();
	
	private final BlockSetting id =
		new BlockSetting("ID", "The type of block to break in ID mode.\n"
			+ "air = won't break anything", "minecraft:air", true);
	
	private final CheckboxSetting lockId = new CheckboxSetting("Lock ID",
		"Prevents changing the ID by clicking on blocks or restarting NukerLegit.",
		false);
	
	private final NukerMultiIdListSetting multiIdList =
		new NukerMultiIdListSetting();
	
	private final OverlayRenderer overlay = new OverlayRenderer();
	private BlockPos currentBlock;
	
	public NukerLegitHack()
	{
		super("NukerLegit");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(mode);
		addSetting(id);
		addSetting(lockId);
		addSetting(multiIdList);
	}
	
	@Override
	public String getRenderName()
	{
		switch(mode.getSelected())
		{
			default:
			case NORMAL:
			return getName();
			
			case ID:
			return "IDNukerLegit [" + id.getShortBlockName() + "]";
			
			case MULTI_ID:
			int ids = multiIdList.size();
			return "MultiIDNukerLegit [" + ids + (ids == 1 ? " ID]" : " IDs]");
			
			case FLAT:
			return "FlatNukerLegit";
			
			case SMASH:
			return "SmashNukerLegit";
		}
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().speedNukerHack.setEnabled(false);
		WURST.getHax().tunnellerHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(LeftClickListener.class, this);
		EVENTS.add(HandleBlockBreakingListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(LeftClickListener.class, this);
		EVENTS.remove(HandleBlockBreakingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		// resets
		IKeyBinding.get(MC.options.attackKey).resetPressedState();
		MC.interactionManager.cancelBlockBreaking();
		overlay.resetProgress();
		currentBlock = null;
		if(!lockId.isChecked())
			id.setBlock(Blocks.AIR);
	}
	
	@Override
	public void onUpdate()
	{
		currentBlock = null;
		
		// abort if using ID mode without an ID being set
		if(mode.getSelected() == NukerMode.ID && id.getBlock() == Blocks.AIR)
		{
			overlay.resetProgress();
			return;
		}
		
		// Ignore the attack cooldown because opening any screen
		// will set it to 10k ticks.
		
		if(MC.player.isRiding())
		{
			overlay.resetProgress();
			MC.interactionManager.cancelBlockBreaking();
			return;
		}
		
		Vec3d eyesVec = RotationUtils.getEyesPos();
		BlockPos eyesBlock = BlockPos.ofFloored(eyesVec);
		double rangeSq = range.getValueSq();
		int blockRange = range.getValueCeil();
		
		Stream<BlockBreakingParams> stream = BlockUtils
			.getAllInBoxStream(eyesBlock, blockRange)
			.filter(this::shouldBreakBlock)
			.map(BlockBreaker::getBlockBreakingParams).filter(Objects::nonNull)
			.filter(BlockBreakingParams::lineOfSight)
			.filter(params -> params.distanceSq() <= rangeSq).sorted(
				Comparator.comparingDouble(BlockBreakingParams::distanceSq));
		
		// Break the first valid block
		currentBlock = stream.filter(this::breakBlock)
			.map(BlockBreakingParams::pos).findFirst().orElse(null);
		
		// reset if no block was found
		if(currentBlock == null)
		{
			IKeyBinding.get(MC.options.attackKey).resetPressedState();
			overlay.resetProgress();
		}
		
		overlay.updateProgress();
	}
	
	private boolean shouldBreakBlock(BlockPos pos)
	{
		switch(mode.getSelected())
		{
			default:
			case NORMAL:
			return true;
			
			case ID:
			return BlockUtils.getName(pos).equals(id.getBlockName());
			
			case MULTI_ID:
			return multiIdList.contains(BlockUtils.getBlock(pos));
			
			case FLAT:
			return pos.getY() >= MC.player.getY();
			
			case SMASH:
			return BlockUtils.getHardness(pos) >= 1;
		}
	}
	
	private boolean breakBlock(BlockBreakingParams params)
	{
		ClientPlayerInteractionManager im = MC.interactionManager;
		
		WURST.getRotationFaker().faceVectorClient(params.hitVec());
		HitResult hitResult = MC.crosshairTarget;
		if(hitResult == null || hitResult.getType() != HitResult.Type.BLOCK
			|| !(hitResult instanceof BlockHitResult bHitResult))
		{
			im.cancelBlockBreaking();
			return true;
		}
		
		BlockPos pos = bHitResult.getBlockPos();
		BlockState state = MC.world.getBlockState(pos);
		Direction side = bHitResult.getSide();
		if(state.isAir() || !params.pos().equals(pos)
			|| !params.side().equals(side))
		{
			im.cancelBlockBreaking();
			return true;
		}
		
		WURST.getHax().autoToolHack.equipIfEnabled(params.pos());
		
		if(MC.player.isUsingItem())
			// This case doesn't cancel block breaking in vanilla Minecraft.
			return true;
		
		if(im.updateBlockBreakingProgress(pos, side))
		{
			MC.particleManager.addBlockBreakingParticles(pos, side);
			MC.player.swingHand(Hand.MAIN_HAND);
			MC.options.attackKey.setPressed(true);
		}
		
		return true;
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(lockId.isChecked() || mode.getSelected() != NukerMode.ID)
			return;
		
		if(!(MC.crosshairTarget instanceof BlockHitResult bHitResult)
			|| bHitResult.getType() != HitResult.Type.BLOCK)
			return;
		
		id.setBlockName(BlockUtils.getName(bHitResult.getBlockPos()));
	}
	
	@Override
	public void onHandleBlockBreaking(HandleBlockBreakingEvent event)
	{
		// Cancel vanilla block breaking so we don't send the packets twice.
		if(currentBlock != null)
			event.cancel();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		overlay.render(matrixStack, partialTicks, currentBlock);
	}
}
