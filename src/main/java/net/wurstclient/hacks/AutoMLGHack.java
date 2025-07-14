/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.MouseUpdateListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockPlacer;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.InteractionSimulator;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.util.Rotation;
import net.wurstclient.util.RotationUtils;

@SearchTags({"mlg", "clutch", "nofall", "autoclutch", "autowater"})
public final class AutoMLGHack extends Hack
	implements UpdateListener, MouseUpdateListener
{
	private enum MlgMode
	{
		LEGIT("Legit"),
		PACKET("Packet");
		
		private final String name;
		
		MlgMode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	private final EnumSetting<MlgMode> mode = new EnumSetting<>("Mode",
		"Legit: Smoothly rotates your camera like a player\n"
			+ "Packet: Instantly snaps serverside\nNOTE: THIS MIGHT FAIL WITH BUCKETS",
		MlgMode.values(), MlgMode.LEGIT);
	
	private final SliderSetting minFallDistance =
		new SliderSetting("Min fall distance",
			"How far you have to fall before the hack will activate", 10, 4,
			256, 1, ValueDisplay.INTEGER);
	
	private final CheckboxSetting pickupAfter =
		new CheckboxSetting("Pickup after landing",
			"Picks up the water after you land safely", true);
	
	private final SliderSetting pickupDelay = new SliderSetting("Pickup delay",
		"Custom pickup delay after landing", 0, 0, 20, 1, ValueDisplay.INTEGER);
	
	private final CheckboxSetting predictLanding = new CheckboxSetting(
		"Better landing prediction",
		"Predicts your landing position based on your velocity, hitbox (more computation intensive)",
		true);
	
	private final CheckboxSetting ignoreSolidCheck = new CheckboxSetting(
		"Place on nonsolid",
		"Allows the hack to try placing on non-solid blocks like slabs", true);
	
	private final CheckboxSetting onlyWhenHolding =
		new CheckboxSetting("Active only when holding",
			"Only attempts an MLG if you are already holding a supported item",
			false);
	
	private final CheckboxSetting pauseOnElytra =
		new CheckboxSetting("Pause on elytra",
			"Automatically pauses the hack while you are flying with an elytra",
			true);
	
	private final CheckboxSetting pauseForMace =
		new CheckboxSetting("Pause for mace",
			"Automatically pauses the hack while you are holding a mace", true);
	
	private final CheckboxSetting useWater = new CheckboxSetting(
		"Use Water Bucket", "Allows using a water bucket", true);
	
	private final CheckboxSetting usePowderedSnow =
		new CheckboxSetting("Use Powdered Snow",
			"Allows using a powdered snow bucket (Nether-safe!)", false);
	
	private final CheckboxSetting useTwistingVines =
		new CheckboxSetting("Use Twisting Vines",
			"Allows using twisting vines (Nether-safe!)", true);
	
	private final CheckboxSetting useSlime = new CheckboxSetting(
		"Use Slime Block", "Allows using a slime block", true);
	private final CheckboxSetting useCobweb =
		new CheckboxSetting("Use Cobweb", "Allows using a cobweb", false);
	private final CheckboxSetting useHoney = new CheckboxSetting(
		"Use Honey Block", "Allows using a honey block", false);
	private final CheckboxSetting useHay =
		new CheckboxSetting("Use Hay Bale", "Allows using a hay bale", false);
	
	private enum State
	{
		IDLE,
		ACTIVE,
		PLACED,
		LANDED
	}
	
	private State state = State.IDLE;
	private MlgSolution currentSolution;
	private Rotation targetRotation;
	private int originalSlot = -1;
	private int pickupDelayTicks = 0;
	
	public AutoMLGHack()
	{
		super("AutoMLG");
		setCategory(Category.MOVEMENT);
		addSetting(mode);
		addSetting(minFallDistance);
		addSetting(pickupAfter);
		addSetting(pickupDelay);
		addSetting(predictLanding);
		addSetting(ignoreSolidCheck);
		addSetting(onlyWhenHolding);
		addSetting(pauseOnElytra);
		addSetting(pauseForMace);
		addSetting(useWater);
		addSetting(usePowderedSnow);
		addSetting(useTwistingVines);
		addSetting(useSlime);
		addSetting(useCobweb);
		addSetting(useHoney);
		addSetting(useHay);
	}
	
	@Override
	public String getRenderName()
	{
		ClientPlayerEntity player = MC.player;
		if(player == null)
			return getName();
		
		if((pauseOnElytra.isChecked() && player.isGliding())
			|| (pauseForMace.isChecked() && isHoldingMace(player))
			|| player.getAbilities().creativeMode)
			return getName() + " (paused)";
		
		if(state != State.IDLE)
			return getName() + " [" + state.name() + "]";
		
		return getName();
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(MouseUpdateListener.class, this);
		reset();
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(MouseUpdateListener.class, this);
		if(originalSlot != -1 && MC.player != null)
			MC.player.getInventory().setSelectedSlot(originalSlot);
		reset();
	}
	
	private void reset()
	{
		state = State.IDLE;
		currentSolution = null;
		targetRotation = null;
		originalSlot = -1;
		pickupDelayTicks = 0;
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		if(state == State.LANDED)
		{
			if(pickupDelayTicks > 0)
			{
				pickupDelayTicks--;
				return;
			}
			pickup();
			return;
		}
		
		if(state == State.PLACED && player.fallDistance == 0)
		{
			state = State.LANDED;
			pickupDelayTicks = pickupDelay.getValueI();
			return;
		}
		
		if(player.isOnGround())
		{
			reset();
			return;
		}
		
		if(player.getAbilities().creativeMode || player.isSpectator()
			|| (pauseOnElytra.isChecked() && player.isGliding())
			|| (pauseForMace.isChecked() && isHoldingMace(player))
			|| player.fallDistance < minFallDistance.getValueI())
		{
			if(state == State.IDLE)
				reset();
			return;
		}
		
		currentSolution = findSolution();
		if(currentSolution == null)
		{
			reset();
			return;
		}
		
		state = State.ACTIVE;
		targetRotation =
			RotationUtils.getNeededRotations(currentSolution.params().hitVec());
		
		if(mode.getSelected() == MlgMode.PACKET)
			targetRotation.sendPlayerLookPacket();
		
		if(originalSlot == -1)
			originalSlot = player.getInventory().getSelectedSlot();
		
		if(!player.getMainHandStack().isOf(currentSolution.item()))
		{
			if(!InventoryUtils.selectItem(currentSolution.item()))
			{
				reset();
				return;
			}
			return;
		}
		
		if(currentSolution.timeToImpact() <= 2)
		{
			InteractionSimulator
				.rightClickBlock(currentSolution.params().toHitResult());
			state = State.PLACED;
		}
	}
	
	@Override
	public void onMouseUpdate(MouseUpdateEvent e)
	{
		if(MC.player == null || mode.getSelected() != MlgMode.LEGIT
			|| targetRotation == null || state != State.ACTIVE)
			return;
		
		Rotation next = RotationUtils.slowlyTurnTowards(targetRotation, 1440F);
		
		int diffYaw = (int)(next.yaw() - MC.player.getYaw());
		int diffPitch = (int)(next.pitch() - MC.player.getPitch());
		
		if(MathHelper.abs(diffYaw) > 0 || MathHelper.abs(diffPitch) > 0)
		{
			e.setDeltaX(e.getDefaultDeltaX() + diffYaw);
			e.setDeltaY(e.getDefaultDeltaY() + diffPitch);
		}
	}
	
	private MlgSolution findSolution()
	{
		Item item = findMlgItem();
		if(item == null)
			return null;
		
		PredictionResult prediction = predictLanding.isChecked()
			? findLandingSurfacePredicted() : findLandingSurfaceVertical();
		
		if(prediction == null)
			return null;
		
		BlockPos placeOn = prediction.pos().up();
		BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(placeOn);
		
		if(params == null)
			return null;
		
		return new MlgSolution(placeOn, item, params, prediction.ticks());
	}
	
	private PredictionResult findLandingSurfaceVertical()
	{
		BlockPos playerPos = MC.player.getBlockPos();
		for(int yOffset = 1; yOffset < playerPos.getY()
			- MC.world.getBottomY(); yOffset++)
		{
			BlockPos checkPos = playerPos.down(yOffset);
			if(isValidSurface(checkPos))
			{
				double y = MC.player.getY();
				double velY = MC.player.getVelocity().y;
				int ticks = 0;
				while(y > checkPos.getY() + 1 && ticks < 400)
				{
					y += velY;
					velY = (velY - 0.08) * 0.98;
					ticks++;
				}
				return new PredictionResult(checkPos, ticks);
			}
		}
		return null;
	}
	
	private PredictionResult findLandingSurfacePredicted()
	{
		ClientPlayerEntity player = MC.player;
		float halfWidth = player.getWidth() / 2F;
		
		Vec3d[] offsets =
			{new Vec3d(0, 0, 0), new Vec3d(-halfWidth, 0, -halfWidth),
				new Vec3d(-halfWidth, 0, halfWidth),
				new Vec3d(halfWidth, 0, -halfWidth),
				new Vec3d(halfWidth, 0, halfWidth)};
		
		Vec3d[] positions = new Vec3d[offsets.length];
		for(int i = 0; i < offsets.length; i++)
			positions[i] = player.getPos().add(offsets[i]);
		
		Vec3d vel = player.getVelocity();
		
		// NOTE: 600 ticks limit is used because the game becomes sometimes
		// laggy if more (800 works too), theoretically 200-400 works too, but
		// 600 is more stable (400 in findLandingSurfaceVertical)
		for(int i = 0; i < 600; i++)
		{
			for(int j = 0; j < positions.length; j++)
			{
				if(positions[j] == null)
					continue;
				
				Vec3d pos = positions[j];
				Vec3d nextPos = pos.add(vel);
				
				BlockHitResult hit = MC.world.raycast(new RaycastContext(pos,
					nextPos, RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, player));
				
				if(hit.getType() == HitResult.Type.BLOCK)
				{
					BlockPos hitPos = hit.getBlockPos();
					if(isValidSurface(hitPos))
						return new PredictionResult(hitPos, i);
					
					positions[j] = null;
				}else
				{
					positions[j] = nextPos;
					if(nextPos.getY() < MC.world.getBottomY())
						positions[j] = null;
				}
			}
			
			// 0.98 is (drag coefficient) -> air resistance of 2%
			// 0.08 is (gravity force) -> subtracted from Y velocity each tick
			vel = new Vec3d(vel.x * 0.98, (vel.y - 0.08) * 0.98, vel.z * 0.98);
			
			boolean allLanded = true;
			for(Vec3d pos : positions)
				if(pos != null)
				{
					allLanded = false;
					break;
				}
			
			if(allLanded)
				return null;
		}
		
		return null;
	}
	
	private boolean isValidSurface(BlockPos pos)
	{
		BlockState state = BlockUtils.getState(pos);
		
		if(state.isAir())
			return false;
		
		boolean canPlaceOn = ignoreSolidCheck.isChecked()
			|| state.isSideSolidFullSquare(MC.world, pos, Direction.UP);
		
		if(!canPlaceOn)
			return false;
		
		BlockPos posAbove = pos.up();
		BlockState stateAbove = BlockUtils.getState(posAbove);
		
		return stateAbove.isReplaceable() && !stateAbove.isOf(Blocks.LAVA);
	}
	
	private Item findMlgItem()
	{
		List<Item> preferredItems = new ArrayList<>();
		
		if(useWater.isChecked() && !(MC.world.getRegistryKey() == World.NETHER))
			preferredItems.add(Items.WATER_BUCKET);
		if(usePowderedSnow.isChecked())
			preferredItems.add(Items.POWDER_SNOW_BUCKET);
		if(useTwistingVines.isChecked())
			preferredItems.add(Items.TWISTING_VINES);
		if(useSlime.isChecked())
			preferredItems.add(Items.SLIME_BLOCK);
		if(useCobweb.isChecked())
			preferredItems.add(Items.COBWEB);
		if(useHoney.isChecked())
			preferredItems.add(Items.HONEY_BLOCK);
		if(useHay.isChecked())
			preferredItems.add(Items.HAY_BLOCK);
		
		if(onlyWhenHolding.isChecked())
		{
			Item heldItem = MC.player.getMainHandStack().getItem();
			return preferredItems.contains(heldItem) ? heldItem : null;
		}
		
		for(Item item : preferredItems)
			if(InventoryUtils.indexOf(item) != -1)
				return item;
			
		return null;
	}
	
	private void pickup()
	{
		if(!pickupAfter.isChecked() || currentSolution == null)
		{
			reset();
			return;
		}
		
		Item mlgItem = currentSolution.item();
		BlockPos mlgBlockPos = currentSolution.mlgPos();
		
		if((mlgItem == Items.WATER_BUCKET
			&& BlockUtils.getBlock(mlgBlockPos) == Blocks.WATER)
			|| (mlgItem == Items.POWDER_SNOW_BUCKET
				&& BlockUtils.getBlock(mlgBlockPos) == Blocks.POWDER_SNOW))
		{
			if(InventoryUtils.selectItem(Items.BUCKET))
			{
				WurstClient.INSTANCE.getRotationFaker()
					.faceVectorPacket(mlgBlockPos.toCenterPos());
				
				BlockHitResult hitResult =
					new BlockHitResult(mlgBlockPos.toCenterPos(), Direction.UP,
						mlgBlockPos, false);
				InteractionSimulator.rightClickBlock(hitResult, Hand.MAIN_HAND);
			}
		}
		
		if(originalSlot != -1)
		{
			MC.player.getInventory().setSelectedSlot(originalSlot);
		}
		
		reset();
	}
	
	private boolean isHoldingMace(ClientPlayerEntity player)
	{
		return player.getMainHandStack().isOf(Items.MACE);
	}
	
	private record MlgSolution(BlockPos mlgPos, Item item,
		BlockPlacingParams params, int timeToImpact)
	{}
	
	private record PredictionResult(BlockPos pos, int ticks)
	{}
}
