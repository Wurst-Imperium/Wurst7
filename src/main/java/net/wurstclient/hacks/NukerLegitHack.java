/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockBreaker.BlockBreakingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.OverlayRenderer;
import net.wurstclient.util.RotationUtils;

@SearchTags({"LegitNuker", "nuker legit", "legit nuker"})
public final class NukerLegitHack extends Hack
	implements LeftClickListener, RenderListener, UpdateListener
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
	
	private final BlockListSetting multiIdList = new BlockListSetting(
		"MultiID List", "The types of blocks to break in MultiID mode.",
		"minecraft:ancient_debris", "minecraft:bone_block",
		"minecraft:coal_ore", "minecraft:copper_ore",
		"minecraft:deepslate_coal_ore", "minecraft:deepslate_copper_ore",
		"minecraft:deepslate_diamond_ore", "minecraft:deepslate_emerald_ore",
		"minecraft:deepslate_gold_ore", "minecraft:deepslate_iron_ore",
		"minecraft:deepslate_lapis_ore", "minecraft:deepslate_redstone_ore",
		"minecraft:diamond_ore", "minecraft:emerald_ore", "minecraft:glowstone",
		"minecraft:gold_ore", "minecraft:iron_ore", "minecraft:lapis_ore",
		"minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
		"minecraft:raw_copper_block", "minecraft:raw_gold_block",
		"minecraft:raw_iron_block", "minecraft:redstone_ore");
	
	private final OverlayRenderer renderer = new OverlayRenderer();
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
		return mode.getSelected().getRenderName(this);
	}
	
	@Override
	public void onEnable()
	{
		// disable other nukers
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().speedNukerHack.setEnabled(false);
		WURST.getHax().tunnellerHack.setEnabled(false);
		
		// add listeners
		EVENTS.add(LeftClickListener.class, this);
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		// remove listeners
		EVENTS.remove(LeftClickListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		// resets
		MC.options.attackKey.setPressed(false);
		renderer.resetProgress();
		currentBlock = null;
		if(!lockId.isChecked())
			id.setBlock(Blocks.AIR);
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		// check mode
		if(mode.getSelected() != Mode.ID)
			return;
		
		if(lockId.isChecked())
			return;
		
		// check hitResult
		if(MC.crosshairTarget == null
			|| !(MC.crosshairTarget instanceof BlockHitResult))
			return;
		
		// check pos
		BlockPos pos = ((BlockHitResult)MC.crosshairTarget).getBlockPos();
		if(pos == null || BlockUtils.getBlock(pos) == Blocks.AIR)
			return;
		
		// set id
		id.setBlockName(BlockUtils.getName(pos));
	}
	
	@Override
	public void onUpdate()
	{
		currentBlock = null;
		
		// abort if using IDNuker without an ID being set
		if(mode.getSelected() == Mode.ID && id.getBlock() == Blocks.AIR)
		{
			renderer.resetProgress();
			return;
		}
		
		// get valid blocks
		Iterable<BlockPos> validBlocks = getValidBlocks(range.getValueI(),
			mode.getSelected().getValidator(this));
		
		// find closest valid block
		for(BlockPos pos : validBlocks)
		{
			// break block
			if(!breakBlockExtraLegit(pos))
				continue;
			
			// set currentBlock if successful
			currentBlock = pos;
			break;
		}
		
		// reset if no block was found
		if(currentBlock == null)
		{
			MC.options.attackKey.setPressed(false);
			renderer.resetProgress();
		}
		
		renderer.updateProgress();
	}
	
	private ArrayList<BlockPos> getValidBlocks(int range,
		Predicate<BlockPos> validator)
	{
		Vec3d eyesVec = RotationUtils.getEyesPos();
		BlockPos center = BlockPos.ofFloored(eyesVec);
		
		return BlockUtils.getAllInBoxStream(center, range)
			.filter(BlockUtils::canBeClicked).filter(validator)
			.sorted(Comparator.comparingDouble(
				pos -> eyesVec.squaredDistanceTo(Vec3d.ofCenter(pos))))
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private boolean breakBlockExtraLegit(BlockPos pos)
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
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		renderer.render(matrixStack, partialTicks, currentBlock);
	}
	
	private enum Mode
	{
		NORMAL("Normal", n -> "NukerLegit", (n, p) -> true),
		
		ID("ID",
			n -> "IDNukerLegit ["
				+ n.id.getBlockName().replace("minecraft:", "") + "]",
			(n, p) -> BlockUtils.getName(p).equals(n.id.getBlockName())),
		
		MULTI_ID("MultiID",
			n -> "MultiIDNuker [" + n.multiIdList.getBlockNames().size()
				+ (n.multiIdList.getBlockNames().size() == 1 ? " ID]"
					: " IDs]"),
			(n, p) -> n.multiIdList.getBlockNames()
				.contains(BlockUtils.getName(p))),
		
		FLAT("Flat", n -> "FlatNukerLegit",
			(n, p) -> p.getY() >= MC.player.getPos().getY()),
		
		SMASH("Smash", n -> "SmashNukerLegit",
			(n, p) -> BlockUtils.getHardness(p) >= 1);
		
		private final String name;
		private final Function<NukerLegitHack, String> renderName;
		private final BiPredicate<NukerLegitHack, BlockPos> validator;
		
		private Mode(String name, Function<NukerLegitHack, String> renderName,
			BiPredicate<NukerLegitHack, BlockPos> validator)
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
		
		public String getRenderName(NukerLegitHack n)
		{
			return renderName.apply(n);
		}
		
		public Predicate<BlockPos> getValidator(NukerLegitHack n)
		{
			return p -> validator.test(n, p);
		}
	}
}
