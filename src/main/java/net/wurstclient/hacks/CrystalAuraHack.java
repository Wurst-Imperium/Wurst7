/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.FaceTargetSetting;
import net.wurstclient.settings.FaceTargetSetting.FaceTarget;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.settings.filterlists.CrystalAuraFilterList;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"crystal aura"})
public final class CrystalAuraHack extends Hack implements UpdateListener
{
	private final SliderSetting range = new SliderSetting("Range",
		"Determines how far CrystalAura will reach to place and detonate crystals.",
		6, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting autoPlace = new CheckboxSetting(
		"Auto-place crystals",
		"When enabled, CrystalAura will automatically place crystals near valid entities.\n"
			+ "When disabled, CrystalAura will only detonate manually placed crystals.",
		true);
	
	private final CheckboxSetting checkLOS = new CheckboxSetting(
		"Check line of sight",
		"Ensures that you don't reach through blocks when placing or left-clicking end crystals.\n\n"
			+ "Slower but can help with anti-cheat plugins.",
		false);
	
	private final FaceTargetSetting faceTarget =
		FaceTargetSetting.withPacketSpam(this, FaceTarget.OFF);
	
	private final SwingHandSetting swingHand =
		new SwingHandSetting(this, SwingHand.CLIENT);
	
	private final EnumSetting<TakeItemsFrom> takeItemsFrom =
		new EnumSetting<>("Take items from", "Where to look for end crystals.",
			TakeItemsFrom.values(), TakeItemsFrom.INVENTORY);
	
	private final EntityFilterList entityFilters =
		CrystalAuraFilterList.create();
	
	public CrystalAuraHack()
	{
		super("CrystalAura");
		
		setCategory(Category.COMBAT);
		addSetting(range);
		addSetting(autoPlace);
		addSetting(checkLOS);
		addSetting(faceTarget);
		addSetting(swingHand);
		addSetting(takeItemsFrom);
		
		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		// disable other killauras
		WURST.getHax().aimAssistHack.setEnabled(false);
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().triggerBotHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ArrayList<Entity> crystals = getNearbyCrystals();
		
		if(!crystals.isEmpty())
		{
			detonate(crystals);
			return;
		}
		
		if(!autoPlace.isChecked())
			return;
		
		if(InventoryUtils.indexOf(Items.END_CRYSTAL,
			takeItemsFrom.getSelected().maxInvSlot) == -1)
			return;
		
		ArrayList<Entity> targets = getNearbyTargets();
		placeCrystalsNear(targets);
	}
	
	private ArrayList<BlockPos> placeCrystalsNear(ArrayList<Entity> targets)
	{
		ArrayList<BlockPos> newCrystals = new ArrayList<>();
		
		boolean shouldSwing = false;
		for(Entity target : targets)
		{
			ArrayList<BlockPos> freeBlocks = getFreeBlocksNear(target);
			
			for(BlockPos pos : freeBlocks)
				if(placeCrystal(pos))
				{
					shouldSwing = true;
					newCrystals.add(pos);
					
					// TODO optional speed limit(?)
					break;
				}
		}
		
		if(shouldSwing)
			swingHand.swing(InteractionHand.MAIN_HAND);
		
		return newCrystals;
	}
	
	private void detonate(ArrayList<Entity> crystals)
	{
		for(Entity e : crystals)
		{
			faceTarget.face(e.getBoundingBox().getCenter());
			MC.gameMode.attack(MC.player, e);
		}
		
		if(!crystals.isEmpty())
			swingHand.swing(InteractionHand.MAIN_HAND);
	}
	
	private boolean placeCrystal(BlockPos pos)
	{
		Vec3 eyesPos = RotationUtils.getEyesPos();
		double rangeSq = Math.pow(range.getValue(), 2);
		Vec3 posVec = Vec3.atCenterOf(pos);
		double distanceSqPosVec = eyesPos.distanceToSqr(posVec);
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.relative(side);
			
			// check if neighbor can be right clicked
			if(!isClickableNeighbor(neighbor))
				continue;
			
			Vec3 dirVec = Vec3.atLowerCornerOf(side.getUnitVec3i());
			Vec3 hitVec = posVec.add(dirVec.scale(0.5));
			
			// check if hitVec is within range
			if(eyesPos.distanceToSqr(hitVec) > rangeSq)
				continue;
			
			// check if side is visible (facing away from player)
			if(distanceSqPosVec > eyesPos.distanceToSqr(posVec.add(dirVec)))
				continue;
			
			if(checkLOS.isChecked()
				&& !BlockUtils.hasLineOfSight(eyesPos, hitVec))
				continue;
			
			InventoryUtils.selectItem(Items.END_CRYSTAL,
				takeItemsFrom.getSelected().maxInvSlot);
			if(!MC.player.isHolding(Items.END_CRYSTAL))
				return false;
			
			faceTarget.face(hitVec);
			
			// place block
			IMC.getInteractionManager().rightClickBlock(neighbor,
				side.getOpposite(), hitVec);
			
			return true;
		}
		
		return false;
	}
	
	private ArrayList<Entity> getNearbyCrystals()
	{
		LocalPlayer player = MC.player;
		double rangeSq = Math.pow(range.getValue(), 2);
		
		Comparator<Entity> furthestFromPlayer =
			Comparator.<Entity> comparingDouble(e -> MC.player.distanceToSqr(e))
				.reversed();
		
		return StreamSupport
			.stream(MC.level.entitiesForRendering().spliterator(), true)
			.filter(EndCrystal.class::isInstance).filter(e -> !e.isRemoved())
			.filter(e -> player.distanceToSqr(e) <= rangeSq)
			.sorted(furthestFromPlayer)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private ArrayList<Entity> getNearbyTargets()
	{
		double rangeSq = Math.pow(range.getValue(), 2);
		
		Comparator<Entity> furthestFromPlayer =
			Comparator.<Entity> comparingDouble(e -> MC.player.distanceToSqr(e))
				.reversed();
		
		Stream<Entity> stream = StreamSupport
			.stream(MC.level.entitiesForRendering().spliterator(), false)
			.filter(e -> !e.isRemoved())
			.filter(e -> e instanceof LivingEntity
				&& ((LivingEntity)e).getHealth() > 0)
			.filter(e -> e != MC.player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> !WURST.getFriends().contains(e.getName().getString()))
			.filter(e -> MC.player.distanceToSqr(e) <= rangeSq);
		
		stream = entityFilters.applyTo(stream);
		
		return stream.sorted(furthestFromPlayer)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private ArrayList<BlockPos> getFreeBlocksNear(Entity target)
	{
		Vec3 eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double rangeD = range.getValue();
		double rangeSq = Math.pow(rangeD + 0.5, 2);
		int rangeI = 2;
		
		BlockPos center = target.blockPosition();
		BlockPos min = center.offset(-rangeI, -rangeI, -rangeI);
		BlockPos max = center.offset(rangeI, rangeI, rangeI);
		AABB targetBB = target.getBoundingBox();
		
		Vec3 targetEyesVec =
			target.position().add(0, target.getEyeHeight(target.getPose()), 0);
		
		Comparator<BlockPos> closestToTarget =
			Comparator.<BlockPos> comparingDouble(
				pos -> targetEyesVec.distanceToSqr(Vec3.atCenterOf(pos)));
		
		return BlockUtils.getAllInBoxStream(min, max).filter(
			pos -> eyesVec.distanceToSqr(Vec3.atLowerCornerOf(pos)) <= rangeSq)
			.filter(this::isReplaceable).filter(this::hasCrystalBase)
			.filter(pos -> !targetBB.intersects(new AABB(pos)))
			.sorted(closestToTarget)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private boolean isReplaceable(BlockPos pos)
	{
		return BlockUtils.getState(pos).canBeReplaced();
	}
	
	private boolean hasCrystalBase(BlockPos pos)
	{
		Block block = BlockUtils.getBlock(pos.below());
		return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN;
	}
	
	private boolean isClickableNeighbor(BlockPos pos)
	{
		return BlockUtils.canBeClicked(pos)
			&& !BlockUtils.getState(pos).canBeReplaced();
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
