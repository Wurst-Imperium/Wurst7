/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.stream.Stream;

import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.NukerMultiIdListSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockBreaker.BlockBreakingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.OverlayRenderer;
import net.wurstclient.util.RotationUtils;

@SearchTags({"LegitNuker", "nuker legit", "legit nuker"})
public final class NukerLegitHack extends Hack
	implements UpdateListener, LeftClickListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 4.25, 1, 4.25, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"\u00a7lNormal\u00a7r mode simply breaks everything around you.\n"
			+ "\u00a7lID\u00a7r mode only breaks the selected block type. Left-click on a block to select it.\n"
			+ "\u00a7lMultiID\u00a7r mode only breaks the block types in your MultiID List.\n"
			+ "\u00a7lFlat\u00a7r mode flattens the area around you, but won't dig down.\n"
			+ "\u00a7lSmash\u00a7r mode only breaks blocks that can be destroyed instantly (e.g. tall grass).",
		Mode.values(), Mode.NORMAL);
	
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
			int ids = multiIdList.getBlockNames().size();
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
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(LeftClickListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		// resets
		MC.options.attackKey.setPressed(false);
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
		if(mode.getSelected() == Mode.ID && id.getBlock() == Blocks.AIR)
		{
			overlay.resetProgress();
			return;
		}
		
		Vec3d eyesVec = RotationUtils.getEyesPos();
		BlockPos eyesBlock = BlockPos.ofFloored(eyesVec);
		
		Stream<BlockPos> stream =
			BlockUtils.getAllInBoxStream(eyesBlock, range.getValueCeil())
				.filter(BlockUtils::canBeClicked).filter(this::shouldBreakBlock)
				.sorted(Comparator
					.comparingDouble(pos -> pos.getSquaredDistance(eyesVec)));
		
		// Break the first valid block
		currentBlock = stream.filter(this::breakBlock).findFirst().orElse(null);
		
		// reset if no block was found
		if(currentBlock == null)
		{
			MC.options.attackKey.setPressed(false);
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
	
	private boolean breakBlock(BlockPos pos)
	{
		BlockBreakingParams params = BlockBreaker.getBlockBreakingParams(pos);
		if(!params.lineOfSight() || params.distanceSq() > range.getValueSq())
			return false;
		
		// face block
		WURST.getRotationFaker().faceVectorClient(params.hitVec());
		
		WURST.getHax().autoToolHack.equipIfEnabled(pos);
		
		if(!MC.interactionManager.isBreakingBlock())
			MC.interactionManager.attackBlock(pos, params.side());
			
		// if attack key is down but nothing happens,
		// release it for one tick
		if(MC.options.attackKey.isPressed()
			&& !MC.interactionManager.isBreakingBlock())
		{
			MC.options.attackKey.setPressed(false);
			return true;
		}
		
		// damage block
		MC.options.attackKey.setPressed(true);
		return true;
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(lockId.isChecked() || mode.getSelected() != Mode.ID)
			return;
		
		if(!(MC.crosshairTarget instanceof BlockHitResult bHitResult)
			|| bHitResult.getType() != HitResult.Type.BLOCK)
			return;
		
		id.setBlockName(BlockUtils.getName(bHitResult.getBlockPos()));
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		overlay.render(matrixStack, partialTicks, currentBlock);
	}
	
	private enum Mode
	{
		NORMAL("Normal"),
		ID("ID"),
		MULTI_ID("MultiID"),
		FLAT("Flat"),
		SMASH("Smash");
		
		private final String name;
		
		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
