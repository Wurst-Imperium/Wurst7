package net.wurstclient.hacks;

import java.util.Iterator;
import java.util.Optional;

import net.minecraft.block.Material;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

public final class AutoShootHack extends Hack implements UpdateListener
{
	private boolean shouldRelease = false;
	private int shouldUse = -1;
	
	public AutoShootHack()
	{
		super("AutoShoot", "Automatically shoots, when the arrow is going to hit.");
		setCategory(Category.COMBAT);
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
		ClientPlayerEntity player = MC.player;
		ItemStack stack = player.getMainHandStack();
		Item item = stack.getItem();
		
		if(shouldRelease)
		{
			if(item == Items.CROSSBOW && !CrossbowItem.isCharged(stack))
				return;
			shouldRelease = false;
			shouldUse = 0;
			MC.options.keyUse.setPressed(false);
			return;
		}
		
		if(shouldUse == 1)
		{
			shouldUse = -1;
			MC.options.keyUse.setPressed(true);
			return;
		}
		
		if(shouldUse != -1)
			shouldUse++;
		
		// check if item is throwable
		if(stack.isEmpty() || !(item instanceof RangedWeaponItem))
			return;
		
		Vec3d cameraPos = BlockEntityRenderDispatcher.INSTANCE.camera.getPos();
		
		double posX = cameraPos.getX()
			- MathHelper.cos(player.yaw / 180.0F * (float)Math.PI)
				* 0.16F;
		double posY = cameraPos.getY() + player.getStandingEyeHeight()
			- 0.10000000149011612D;
		double posZ = cameraPos.getZ()
			-MathHelper.sin(player.yaw / 180.0F * (float)Math.PI)
				* 0.16F;
		double motionX =
			-MathHelper.sin(player.yaw / 180.0F * (float)Math.PI)
				* MathHelper.cos(player.pitch / 180.0F * (float)Math.PI)
				* 1.0;
		double motionY =
			-MathHelper.sin(player.pitch / 180.0F * (float)Math.PI) * 1;
		double motionZ =
			MathHelper.cos(player.yaw / 180.0F * (float)Math.PI)
				* MathHelper.cos(player.pitch / 180.0F * (float)Math.PI)
				* 1.0;
		
		if(player.getItemUseTimeLeft() == 0)
			return;
		
		float power = (72000 - player.getItemUseTimeLeft()) / 20F;
		power = (power * power + power * 2F) / 3F;
		
		if(power < 0.1D)
			return;
		if(power > 1.0F)
			power = 1.0F;
		
		float distance = (float)Math
			.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
		motionX /= distance;
		motionY /= distance;
		motionZ /= distance;
		
		motionX *= power * 2 * 1.5;
		motionY *= power * 2 * 1.5;
		motionZ *= power * 2 * 1.5;
		
		HitResult landingPosition = null;
		boolean hasLanded = false;
		boolean hitEntity = false;
		float gravity = 0.05F;
		float size = 0.3F;
		Vec3d eyesPos = RotationUtils.getEyesPos();
		
		for(int limit = 0; !hasLanded && limit < 300; limit++)
		{
			Vec3d posBefore =
				new Vec3d(posX, posY, posZ);
			Vec3d posAfter =
				new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
			landingPosition = MC.world.rayTrace(new RayTraceContext(eyesPos, posAfter,
				RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, MC.player));
			
			if(landingPosition.getType() != HitResult.Type.MISS)
			{
				hasLanded = true;
				posAfter = new Vec3d(landingPosition.getPos().x,
					landingPosition.getPos().y,
					landingPosition.getPos().z);
			}
			
			Box arrowBox = new Box(posX - size, posY - size,
				posZ - size, posX + size, posY + size, posZ + size);
			Iterator<Entity> entityList = MC.world.getEntities(MC.player, arrowBox.expand(motionX, motionY, motionZ).expand(1.0D), (e) ->
				!e.isSpectator() && e.isAlive() && e.collides()).iterator();
			
			while(entityList.hasNext())
			{
				Entity possibleEntity = entityList.next();
				Box expand = possibleEntity.getBoundingBox();
				Optional<Vec3d> optional = expand.rayTrace(posBefore, posAfter);
				if(optional.isPresent())
				{
					hitEntity = true;
					hasLanded = true;
					landingPosition = new EntityHitResult(possibleEntity, optional.get());
				}
			}
			
			posX += motionX;
			posY += motionY;
			posZ += motionZ;
			
			BlockPos pos = new BlockPos(posX, posY, posZ);
			
			if(BlockUtils.getState(pos).getMaterial() == Material.WATER)
			{
				motionX *= 0.6;
				motionY *= 0.6;
				motionZ *= 0.6;
			}else
			{
				motionX *= 0.99;
				motionY *= 0.99;
				motionZ *= 0.99;
			}
			motionY -= gravity;
		}
		if(landingPosition != null)
			if(hitEntity)
				shouldRelease = true;
	}
}
