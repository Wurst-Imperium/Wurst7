/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.function.Predicate;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"ArrowTrajectories", "ArrowPrediction", "aim assist",
	"arrow trajectories", "bow trajectories"})
public final class TrajectoriesHack extends Hack implements RenderListener
{
	private final ColorSetting missColor = new ColorSetting("Miss Color",
		"Color of the trajectory when it doesn't hit anything.", Color.GRAY);
	
	private final ColorSetting entityHitColor =
		new ColorSetting("Entity Hit Color",
			"Color of the trajectory when it hits an entity.", Color.RED);
	
	private final ColorSetting blockHitColor =
		new ColorSetting("Block Hit Color",
			"Color of the trajectory when it hits a block.", Color.GREEN);
	
	public TrajectoriesHack()
	{
		super("Trajectories");
		setCategory(Category.RENDER);
		addSetting(missColor);
		addSetting(entityHitColor);
		addSetting(blockHitColor);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		Trajectory trajectory = getTrajectory(partialTicks);
		if(trajectory.isEmpty())
			return;
		
		ColorSetting color = getColor(trajectory);
		int lineColor = color.getColorI(0xC0);
		int quadColor = color.getColorI(0x40);
		
		Box endBox = trajectory.getEndBox();
		ArrayList<Vec3d> path = trajectory.path();
		
		RenderUtils.drawSolidBox(matrixStack, endBox, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, endBox, lineColor, false);
		
		RenderUtils.drawCurvedLine(matrixStack, path, lineColor, false);
	}
	
	private Trajectory getTrajectory(float partialTicks)
	{
		ClientPlayerEntity player = MC.player;
		ArrayList<Vec3d> path = new ArrayList<>();
		HitResult.Type type = HitResult.Type.MISS;
		
		// Find the hand with a throwable item
		Hand hand = Hand.MAIN_HAND;
		ItemStack stack = player.getMainHandStack();
		if(!isThrowable(stack))
		{
			hand = Hand.OFF_HAND;
			stack = player.getOffHandStack();
			
			// If neither hand has a throwable item, return empty path
			if(!isThrowable(stack))
				return new Trajectory(path, type);
		}
		
		// Calculate item-specific values
		Item item = stack.getItem();
		double throwPower = getThrowPower(item);
		double gravity = getProjectileGravity(item);
		FluidHandling fluidHandling = getFluidHandling(item);
		
		// Prepare yaw and pitch
		double yaw = Math.toRadians(player.getYaw());
		double pitch = Math.toRadians(player.getPitch());
		
		// Calculate starting position
		Vec3d arrowPos = EntityUtils.getLerpedPos(player, partialTicks)
			.add(getHandOffset(hand, yaw));
		
		// Calculate starting motion
		Vec3d arrowMotion = getStartingMotion(yaw, pitch, throwPower);
		
		// Build the path
		for(int i = 0; i < 1000; i++)
		{
			// Add to path
			path.add(arrowPos);
			
			// Apply motion
			arrowPos = arrowPos.add(arrowMotion.multiply(0.1));
			
			// Apply air friction
			arrowMotion = arrowMotion.multiply(0.999);
			
			// Apply gravity
			arrowMotion = arrowMotion.add(0, -gravity * 0.1, 0);
			
			Vec3d lastPos = path.size() > 1 ? path.get(path.size() - 2)
				: RotationUtils.getEyesPos();
			
			// Check for block collision
			BlockHitResult bResult =
				BlockUtils.raycast(lastPos, arrowPos, fluidHandling);
			if(bResult.getType() != HitResult.Type.MISS)
			{
				// Replace last pos with the collision point
				type = HitResult.Type.BLOCK;
				path.set(path.size() - 1, bResult.getPos());
				break;
			}
			
			// Check for entity collision
			Box box = new Box(lastPos, arrowPos);
			Predicate<Entity> predicate = e -> !e.isSpectator() && e.canHit();
			double maxDistSq = 64 * 64;
			EntityHitResult eResult = ProjectileUtil.raycast(player, lastPos,
				arrowPos, box, predicate, maxDistSq);
			if(eResult != null && eResult.getType() != HitResult.Type.MISS)
			{
				// Replace last pos with the collision point
				type = HitResult.Type.ENTITY;
				path.set(path.size() - 1, eResult.getPos());
				break;
			}
		}
		
		return new Trajectory(path, type);
	}
	
	private boolean isThrowable(ItemStack stack)
	{
		if(stack.isEmpty())
			return false;
		
		Item item = stack.getItem();
		return item instanceof RangedWeaponItem || item instanceof SnowballItem
			|| item instanceof EggItem || item instanceof EnderPearlItem
			|| item instanceof ThrowablePotionItem
			|| item instanceof FishingRodItem || item instanceof TridentItem;
	}
	
	private double getThrowPower(Item item)
	{
		// Use a static 1.5x for snowballs and such
		if(!(item instanceof RangedWeaponItem))
			return 1.5;
		
		// Calculate bow power
		float bowPower = (72000 - MC.player.getItemUseTimeLeft()) / 20F;
		bowPower = bowPower * bowPower + bowPower * 2F;
		
		// Clamp value if fully charged or not charged at all
		if(bowPower > 3 || bowPower <= 0.3F)
			bowPower = 3;
		
		return bowPower;
	}
	
	private double getProjectileGravity(Item item)
	{
		if(item instanceof RangedWeaponItem)
			return 0.05;
		
		if(item instanceof ThrowablePotionItem)
			return 0.4;
		
		if(item instanceof FishingRodItem)
			return 0.15;
		
		if(item instanceof TridentItem)
			return 0.015;
		
		return 0.03;
	}
	
	private FluidHandling getFluidHandling(Item item)
	{
		if(item instanceof FishingRodItem)
			return FluidHandling.ANY;
		
		return FluidHandling.NONE;
	}
	
	private Vec3d getHandOffset(Hand hand, double yaw)
	{
		Arm mainArm = MC.options.getMainArm().getValue();
		
		boolean rightSide = mainArm == Arm.RIGHT && hand == Hand.MAIN_HAND
			|| mainArm == Arm.LEFT && hand == Hand.OFF_HAND;
		
		double sideMultiplier = rightSide ? -1 : 1;
		double handOffsetX = Math.cos(yaw) * 0.16 * sideMultiplier;
		double handOffsetY = MC.player.getStandingEyeHeight() - 0.1;
		double handOffsetZ = Math.sin(yaw) * 0.16 * sideMultiplier;
		
		return new Vec3d(handOffsetX, handOffsetY, handOffsetZ);
	}
	
	private Vec3d getStartingMotion(double yaw, double pitch, double throwPower)
	{
		double cosOfPitch = Math.cos(pitch);
		
		double arrowMotionX = -Math.sin(yaw) * cosOfPitch;
		double arrowMotionY = -Math.sin(pitch);
		double arrowMotionZ = Math.cos(yaw) * cosOfPitch;
		
		return new Vec3d(arrowMotionX, arrowMotionY, arrowMotionZ).normalize()
			.multiply(throwPower);
	}
	
	private ColorSetting getColor(Trajectory trajectory)
	{
		return switch(trajectory.type())
		{
			case MISS -> missColor;
			case ENTITY -> entityHitColor;
			case BLOCK -> blockHitColor;
		};
	}
	
	private record Trajectory(ArrayList<Vec3d> path, HitResult.Type type)
	{
		public boolean isEmpty()
		{
			return path.isEmpty();
		}
		
		public Box getEndBox()
		{
			Vec3d end = path.get(path.size() - 1);
			return new Box(end.subtract(0.5), end.add(0.5));
		}
	}
}
