/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.wurstclient.Category;
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
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.OverlayRenderer;
import net.wurstclient.util.RotationUtils;

public final class NukerHack extends Hack
	implements UpdateListener, LeftClickListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
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
		"Prevents changing the ID by clicking on blocks or restarting Nuker.",
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
	
	private final ArrayDeque<Set<BlockPos>> prevBlocks = new ArrayDeque<>();
	private final OverlayRenderer renderer = new OverlayRenderer();
	private BlockPos currentBlock;
	
	public NukerHack()
	{
		super("Nuker");
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
	protected void onEnable()
	{
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
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
		
		if(currentBlock != null)
		{
			MC.interactionManager.breakingBlock = true;
			MC.interactionManager.cancelBlockBreaking();
			currentBlock = null;
		}
		
		prevBlocks.clear();
		renderer.resetProgress();
		
		if(!lockId.isChecked())
			id.setBlock(Blocks.AIR);
	}
	
	@Override
	public void onUpdate()
	{
		currentBlock = null;
		
		// abort if user is mining manually
		if(MC.options.attackKey.isPressed())
			return;
		
		// abort if using IDNuker without an ID being set
		if(mode.getSelected() == Mode.ID && id.getBlock() == Blocks.AIR)
			return;
		
		ClientPlayerEntity player = MC.player;
		Vec3d eyesPos = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		BlockPos eyesBlock = BlockPos.ofFloored(RotationUtils.getEyesPos());
		double rangeSq = Math.pow(range.getValue(), 2);
		int blockRange = (int)Math.ceil(range.getValue());
		
		Vec3i rangeVec = new Vec3i(blockRange, blockRange, blockRange);
		BlockPos min = eyesBlock.subtract(rangeVec);
		BlockPos max = eyesBlock.add(rangeVec);
		
		ArrayList<BlockPos> blocks = BlockUtils.getAllInBox(min, max);
		Stream<BlockPos> stream = blocks.parallelStream();
		
		List<BlockPos> blocks2 = stream
			.filter(pos -> eyesPos.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(BlockUtils::canBeClicked)
			.filter(mode.getSelected().getValidator(this))
			.sorted(Comparator.comparingDouble(
				pos -> eyesPos.squaredDistanceTo(Vec3d.of(pos))))
			.collect(Collectors.toList());
		
		if(player.getAbilities().creativeMode)
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
			renderer.resetProgress();
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
			renderer.updateProgress();
		else
			renderer.resetProgress();
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(mode.getSelected() != Mode.ID)
			return;
		
		if(lockId.isChecked())
			return;
		
		if(MC.crosshairTarget == null
			|| MC.crosshairTarget.getType() != HitResult.Type.BLOCK)
			return;
		
		BlockHitResult blockHitResult = (BlockHitResult)MC.crosshairTarget;
		BlockPos pos = new BlockPos(blockHitResult.getBlockPos());
		id.setBlockName(BlockUtils.getName(pos));
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		renderer.render(matrixStack, partialTicks, currentBlock);
	}
	
	private enum Mode
	{
		NORMAL("Normal", NukerHack::getName, (n, p) -> true),
		
		ID("ID",
			n -> "IDNuker [" + n.id.getBlockName().replace("minecraft:", "")
				+ "]",
			(n, p) -> BlockUtils.getName(p).equals(n.id.getBlockName())),
		
		MULTI_ID("MultiID",
			n -> "MultiIDNuker [" + n.multiIdList.getBlockNames().size()
				+ (n.multiIdList.getBlockNames().size() == 1 ? " ID]"
					: " IDs]"),
			(n, p) -> n.multiIdList.getBlockNames()
				.contains(BlockUtils.getName(p))),
		
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
