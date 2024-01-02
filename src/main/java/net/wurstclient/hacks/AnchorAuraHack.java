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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.FacingSetting;
import net.wurstclient.settings.FacingSetting.Facing;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.AnchorAuraFilterList;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"anchor aura", "CrystalAura", "crystal aura"})
public final class AnchorAuraHack extends Hack implements UpdateListener
{
	private final SliderSetting range = new SliderSetting("Range",
		"Determines how far AnchorAura will reach to place, charge and detonate anchors.",
		6, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting autoPlace = new CheckboxSetting(
		"Auto-place anchors",
		"When enabled, AnchorAura will automatically place anchors near valid entities.\n"
			+ "When disabled, AnchorAura will only charge and detonate manually placed anchors.",
		true);
	
	private final FacingSetting faceBlocks =
		FacingSetting.withPacketSpam("Face anchors",
			"Whether or not AnchorAura should face the correct direction when"
				+ " placing and right-clicking respawn anchors.\n\n"
				+ "Slower but can help with anti-cheat plugins.",
			Facing.OFF);
	
	private final CheckboxSetting checkLOS = new CheckboxSetting(
		"Check line of sight",
		"Ensures that you don't reach through blocks when placing or right-clicking respawn anchors.\n\n"
			+ "Slower but can help with anti-cheat plugins.",
		false);
	
	private final EnumSetting<TakeItemsFrom> takeItemsFrom = new EnumSetting<>(
		"Take items from", "Where to look for respawn anchors and glowstone.",
		TakeItemsFrom.values(), TakeItemsFrom.INVENTORY);
	
	private final EntityFilterList entityFilters =
		AnchorAuraFilterList.create();
	
	public AnchorAuraHack()
	{
		super("AnchorAura");
		
		setCategory(Category.COMBAT);
		addSetting(range);
		addSetting(autoPlace);
		addSetting(faceBlocks);
		addSetting(checkLOS);
		addSetting(takeItemsFrom);
		
		entityFilters.forEach(this::addSetting);
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
		if(MC.world.getDimension().respawnAnchorWorks())
		{
			ChatUtils.error("Respawn anchors don't explode in this dimension.");
			setEnabled(false);
		}
		
		ArrayList<BlockPos> anchors = getNearbyAnchors();
		
		Map<Boolean, ArrayList<BlockPos>> anchorsByCharge = anchors.stream()
			.collect(Collectors.partitioningBy(this::isChargedAnchor,
				Collectors.toCollection(ArrayList::new)));
		
		ArrayList<BlockPos> chargedAnchors = anchorsByCharge.get(true);
		ArrayList<BlockPos> unchargedAnchors = anchorsByCharge.get(false);
		
		if(!chargedAnchors.isEmpty())
		{
			detonate(chargedAnchors);
			return;
		}
		
		int maxInvSlot = takeItemsFrom.getSelected().maxInvSlot;
		
		if(!unchargedAnchors.isEmpty()
			&& InventoryUtils.indexOf(Items.GLOWSTONE, maxInvSlot) >= 0)
		{
			charge(unchargedAnchors);
			// TODO: option to wait until next tick?
			detonate(unchargedAnchors);
			return;
		}
		
		if(!autoPlace.isChecked()
			|| InventoryUtils.indexOf(Items.RESPAWN_ANCHOR, maxInvSlot) == -1)
			return;
		
		ArrayList<Entity> targets = getNearbyTargets();
		ArrayList<BlockPos> newAnchors = placeAnchorsNear(targets);
		
		if(!newAnchors.isEmpty()
			&& InventoryUtils.indexOf(Items.GLOWSTONE, maxInvSlot) >= 0)
		{
			// TODO: option to wait until next tick?
			charge(newAnchors);
			detonate(newAnchors);
		}
	}
	
	private ArrayList<BlockPos> placeAnchorsNear(ArrayList<Entity> targets)
	{
		ArrayList<BlockPos> newAnchors = new ArrayList<>();
		
		boolean shouldSwing = false;
		for(Entity target : targets)
		{
			ArrayList<BlockPos> freeBlocks = getFreeBlocksNear(target);
			
			for(BlockPos pos : freeBlocks)
				if(placeAnchor(pos))
				{
					shouldSwing = true;
					newAnchors.add(pos);
					
					// TODO optional speed limit(?)
					break;
				}
		}
		
		if(shouldSwing)
			MC.player.swingHand(Hand.MAIN_HAND);
		
		return newAnchors;
	}
	
	private void detonate(ArrayList<BlockPos> chargedAnchors)
	{
		if(isSneaking())
			return;
		
		InventoryUtils.selectItem(stack -> !stack.isOf(Items.GLOWSTONE),
			takeItemsFrom.getSelected().maxInvSlot);
		if(MC.player.isHolding(Items.GLOWSTONE))
			return;
		
		boolean shouldSwing = false;
		
		for(BlockPos pos : chargedAnchors)
			if(rightClickBlock(pos))
				shouldSwing = true;
			
		if(shouldSwing)
			MC.player.swingHand(Hand.MAIN_HAND);
	}
	
	private void charge(ArrayList<BlockPos> unchargedAnchors)
	{
		if(isSneaking())
			return;
		
		InventoryUtils.selectItem(Items.GLOWSTONE,
			takeItemsFrom.getSelected().maxInvSlot);
		if(!MC.player.isHolding(Items.GLOWSTONE))
			return;
		
		boolean shouldSwing = false;
		
		for(BlockPos pos : unchargedAnchors)
			if(rightClickBlock(pos))
				shouldSwing = true;
			
		if(shouldSwing)
			MC.player.swingHand(Hand.MAIN_HAND);
	}
	
	private boolean rightClickBlock(BlockPos pos)
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
			
			if(checkLOS.isChecked()
				&& !BlockUtils.hasLineOfSight(eyesPos, hitVec))
				continue;
			
			faceBlocks.getSelected().face(hitVec);
			
			// place block
			IMC.getInteractionManager().rightClickBlock(pos, side, hitVec);
			
			return true;
		}
		
		return false;
	}
	
	private boolean placeAnchor(BlockPos pos)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		double rangeSq = range.getValueSq();
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.offset(side);
			
			// check if neighbor can be right clicked
			if(!isClickableNeighbor(neighbor))
				continue;
			
			Vec3d dirVec = Vec3d.of(side.getVector());
			Vec3d hitVec = posVec.add(dirVec.multiply(0.5));
			
			// check if hitVec is within range
			if(eyesPos.squaredDistanceTo(hitVec) > rangeSq)
				continue;
			
			// check if side is visible (facing away from player)
			if(distanceSqPosVec > eyesPos.squaredDistanceTo(posVec.add(dirVec)))
				continue;
			
			if(checkLOS.isChecked()
				&& !BlockUtils.hasLineOfSight(eyesPos, hitVec))
				continue;
			
			InventoryUtils.selectItem(Items.RESPAWN_ANCHOR,
				takeItemsFrom.getSelected().maxInvSlot);
			if(!MC.player.isHolding(Items.RESPAWN_ANCHOR))
				return false;
			
			faceBlocks.getSelected().face(hitVec);
			
			// place block
			IMC.getInteractionManager().rightClickBlock(neighbor,
				side.getOpposite(), hitVec);
			
			return true;
		}
		
		return false;
	}
	
	private ArrayList<BlockPos> getNearbyAnchors()
	{
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		BlockPos center = BlockPos.ofFloored(RotationUtils.getEyesPos());
		int rangeI = range.getValueCeil();
		double rangeSq = MathHelper.square(range.getValue() + 0.5);
		
		Comparator<BlockPos> furthestFromPlayer =
			Comparator.<BlockPos> comparingDouble(
				pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos))).reversed();
		
		return BlockUtils.getAllInBoxStream(center, rangeI)
			.filter(pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(pos -> BlockUtils.getBlock(pos) == Blocks.RESPAWN_ANCHOR)
			.sorted(furthestFromPlayer)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private ArrayList<Entity> getNearbyTargets()
	{
		double rangeSq = range.getValueSq();
		
		Comparator<Entity> furthestFromPlayer = Comparator
			.<Entity> comparingDouble(e -> MC.player.squaredDistanceTo(e))
			.reversed();
		
		Stream<Entity> stream =
			StreamSupport.stream(MC.world.getEntities().spliterator(), false)
				.filter(e -> !e.isRemoved())
				.filter(e -> e instanceof LivingEntity
					&& ((LivingEntity)e).getHealth() > 0)
				.filter(e -> e != MC.player)
				.filter(e -> !(e instanceof FakePlayerEntity))
				.filter(
					e -> !WURST.getFriends().contains(e.getName().getString()))
				.filter(e -> MC.player.squaredDistanceTo(e) <= rangeSq);
		
		stream = entityFilters.applyTo(stream);
		
		return stream.sorted(furthestFromPlayer)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private ArrayList<BlockPos> getFreeBlocksNear(Entity target)
	{
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double rangeSq = MathHelper.square(range.getValue() + 0.5);
		
		BlockPos center = target.getBlockPos();
		int rangeI = 2;
		
		Box targetBB = target.getBoundingBox();
		Vec3d targetEyesVec =
			target.getPos().add(0, target.getEyeHeight(target.getPose()), 0);
		
		Comparator<BlockPos> closestToTarget =
			Comparator.<BlockPos> comparingDouble(
				pos -> targetEyesVec.squaredDistanceTo(Vec3d.ofCenter(pos)));
		
		return BlockUtils.getAllInBoxStream(center, rangeI)
			.filter(pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(this::isReplaceable).filter(this::hasClickableNeighbor)
			.filter(pos -> !targetBB.intersects(new Box(pos)))
			.sorted(closestToTarget)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private boolean isReplaceable(BlockPos pos)
	{
		return BlockUtils.getState(pos).isReplaceable();
	}
	
	private boolean hasClickableNeighbor(BlockPos pos)
	{
		return isClickableNeighbor(pos.up()) || isClickableNeighbor(pos.down())
			|| isClickableNeighbor(pos.north())
			|| isClickableNeighbor(pos.east())
			|| isClickableNeighbor(pos.south())
			|| isClickableNeighbor(pos.west());
	}
	
	private boolean isClickableNeighbor(BlockPos pos)
	{
		return BlockUtils.canBeClicked(pos)
			&& !BlockUtils.getState(pos).isReplaceable();
	}
	
	private boolean isChargedAnchor(BlockPos pos)
	{
		return BlockUtils.getState(pos).getOrEmpty(RespawnAnchorBlock.CHARGES)
			.orElse(0) > 0;
	}
	
	private boolean isSneaking()
	{
		return MC.player.isSneaking() || WURST.getHax().sneakHack.isEnabled();
	}
	
	private enum TakeItemsFrom
	{
		HOTBAR("Hotbar", 9),
		
		INVENTORY("Inventory", 36);
		
		private final String name;
		private final int maxInvSlot;
		
		private TakeItemsFrom(String name, int maxInvSlot)
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
