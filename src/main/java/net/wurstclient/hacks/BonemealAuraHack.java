/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"bonemeal aura", "bone meal aura", "AutoBone", "auto bone"})
public final class BonemealAuraHack extends Hack implements UpdateListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 4.25, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"\u00a7lFast\u00a7r mode can use bone meal on\n"
			+ "multiple blocks at once.\n"
			+ "\u00a7lLegit\u00a7r mode can bypass NoCheat+.",
		Mode.values(), Mode.FAST);
	
	private final EnumSetting<AutomationLevel> automationLevel =
		new EnumSetting<>("Automation",
			"How much of the bone-mealing process to automate.\n"
				+ "\u00a7lRight Click\u00a7r simply right clicks plants with the bone\n"
				+ "meal in your hand.\n"
				+ "\u00a7lHotbar\u00a7r selects bone meal in your hotbar and then\n"
				+ "uses it on plants.\n"
				+ "\u00a7lInventory\u00a7r finds bone meal in your inventory,\n"
				+ "moves it to your hotbar and then uses it.",
			AutomationLevel.values(), AutomationLevel.RIGHT_CLICK);
	
	private final CheckboxSetting saplings =
		new CheckboxSetting("Saplings", true);
	
	private final CheckboxSetting crops = new CheckboxSetting("Crops",
		"Wheat, carrots, potatoes and beetroots.", true);
	
	private final CheckboxSetting stems =
		new CheckboxSetting("Stems", "Pumpkins and melons.", true);
	
	private final CheckboxSetting cocoa = new CheckboxSetting("Cocoa", true);
	
	private final CheckboxSetting other = new CheckboxSetting("Other", false);
	
	public BonemealAuraHack()
	{
		super("BonemealAura",
			"Automatically uses bone meal on specific types of plants.\n"
				+ "Use the checkboxes to specify the types of plants.");
		
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(mode);
		addSetting(automationLevel);
		
		addSetting(saplings);
		addSetting(crops);
		addSetting(stems);
		addSetting(cocoa);
		addSetting(other);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// wait for right click timer
		if(IMC.getItemUseCooldown() > 0)
			return;
		
		// get valid blocks
		ArrayList<BlockPos> validBlocks =
			getValidBlocks(range.getValue(), (p) -> isCorrectBlock(p));
		
		if(validBlocks.isEmpty())
			return;
		
		// wait for AutoFarm
		if(WURST.getHax().autoFarmHack.isBusy())
			return;
		
		// check held item
		ItemStack stack = MC.player.inventory.getMainHandStack();
		if(stack.isEmpty() || stack.getItem() != Items.BONE_MEAL)
		{
			selectBonemeal();
			return;
		}
		
		if(mode.getSelected() == Mode.LEGIT)
		{
			// legit mode
			
			// use bone meal on next valid block
			for(BlockPos pos : validBlocks)
				if(rightClickBlockLegit(pos))
					break;
				
		}else
		{
			// fast mode
			
			boolean shouldSwing = false;
			
			// use bone meal on all valid blocks
			for(BlockPos pos : validBlocks)
				if(rightClickBlockSimple(pos))
					shouldSwing = true;
				
			// swing arm
			if(shouldSwing)
				MC.player.swingHand(Hand.MAIN_HAND);
		}
	}
	
	private void selectBonemeal()
	{
		ClientPlayerEntity player = MC.player;
		int maxInvSlot = automationLevel.getSelected().maxInvSlot;
		
		for(int slot = 0; slot < maxInvSlot; slot++)
		{
			if(slot == player.inventory.selectedSlot)
				continue;
			
			ItemStack stack = player.inventory.getStack(slot);
			if(stack.isEmpty() || stack.getItem() != Items.BONE_MEAL)
				continue;
			
			if(slot < 9)
				player.inventory.selectedSlot = slot;
			else if(player.inventory.getEmptySlot() < 9)
				IMC.getInteractionManager().windowClick_QUICK_MOVE(slot);
			else if(player.inventory.getEmptySlot() != -1)
			{
				IMC.getInteractionManager()
					.windowClick_QUICK_MOVE(player.inventory.selectedSlot + 36);
				IMC.getInteractionManager().windowClick_QUICK_MOVE(slot);
			}else
			{
				IMC.getInteractionManager()
					.windowClick_PICKUP(player.inventory.selectedSlot + 36);
				IMC.getInteractionManager().windowClick_PICKUP(slot);
				IMC.getInteractionManager()
					.windowClick_PICKUP(player.inventory.selectedSlot + 36);
			}
			
			return;
		}
	}
	
	private ArrayList<BlockPos> getValidBlocks(double range,
		Predicate<BlockPos> validator)
	{
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double rangeSq = Math.pow(range + 0.5, 2);
		int rangeI = (int)Math.ceil(range);
		
		BlockPos center = new BlockPos(RotationUtils.getEyesPos());
		BlockPos min = center.add(-rangeI, -rangeI, -rangeI);
		BlockPos max = center.add(rangeI, rangeI, rangeI);
		
		Comparator<BlockPos> c = Comparator.<BlockPos> comparingDouble(
			pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos))).reversed();
		
		return BlockUtils.getAllInBox(min, max).stream()
			.filter(pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(validator).sorted(c)
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
	}
	
	private boolean isCorrectBlock(BlockPos pos)
	{
		Block block = BlockUtils.getBlock(pos);
		BlockState state = BlockUtils.getState(pos);
		ClientWorld world = MC.world;
		
		if(!(block instanceof Fertilizable) || block instanceof GrassBlock
			|| !((Fertilizable)block).canGrow(world, MC.world.random, pos,
				state))
			return false;
		
		if(block instanceof SaplingBlock
			&& ((SaplingBlock)block).isFertilizable(world, pos, state, true))
			return saplings.isChecked();
		else if(block instanceof CropBlock
			&& ((CropBlock)block).isFertilizable(world, pos, state, true))
			return crops.isChecked();
		else if(block instanceof StemBlock
			&& ((StemBlock)block).isFertilizable(world, pos, state, true))
			return stems.isChecked();
		else if(block instanceof CocoaBlock
			&& ((CocoaBlock)block).isFertilizable(world, pos, state, true))
			return cocoa.isChecked();
		else
			return other.isChecked();
	}
	
	private boolean rightClickBlockLegit(BlockPos pos)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		for(Direction side : Direction.values())
		{
			Vec3d hitVec = posVec.add(Vec3d.of(side.getVector()).multiply(0.5));
			double distanceSqHitVec = eyesPos.squaredDistanceTo(hitVec);
			
			// check if hitVec is within range (4.25 blocks)
			if(distanceSqHitVec > 18.0625)
				continue;
			
			// check if side is facing towards player
			if(distanceSqHitVec >= distanceSqPosVec)
				continue;
			
			// check line of sight
			if(MC.world
				.raycast(new RaycastContext(eyesPos, hitVec,
					RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, MC.player))
				.getType() != HitResult.Type.MISS)
				continue;
			
			// face block
			WURST.getRotationFaker().faceVectorPacket(hitVec);
			
			// place block
			IMC.getInteractionManager().rightClickBlock(pos, side, hitVec);
			MC.player.swingHand(Hand.MAIN_HAND);
			IMC.setItemUseCooldown(4);
			
			return true;
		}
		
		return false;
	}
	
	private boolean rightClickBlockSimple(BlockPos pos)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		for(Direction side : Direction.values())
		{
			Vec3d hitVec = posVec.add(Vec3d.of(side.getVector()).multiply(0.5));
			double distanceSqHitVec = eyesPos.squaredDistanceTo(hitVec);
			
			// check if hitVec is within range (6 blocks)
			if(distanceSqHitVec > 36)
				continue;
			
			// check if side is facing towards player
			if(distanceSqHitVec >= distanceSqPosVec)
				continue;
			
			// place block
			IMC.getInteractionManager().rightClickBlock(pos, side, hitVec);
			
			return true;
		}
		
		return false;
	}
	
	private enum Mode
	{
		FAST("Fast"),
		
		LEGIT("Legit");
		
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
	
	private enum AutomationLevel
	{
		RIGHT_CLICK("Right Click", 0),
		
		HOTBAR("Hotbar", 9),
		
		INVENTORY("Inventory", 36);
		
		private final String name;
		private final int maxInvSlot;
		
		private AutomationLevel(String name, int maxInvSlot)
		{
			this.name = name;
			this.maxInvSlot = maxInvSlot;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
