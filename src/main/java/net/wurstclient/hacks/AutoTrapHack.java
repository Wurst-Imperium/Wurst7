package net.wurstclient.hacks;

import net.wurstclient.hack.Hack;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import net.minecraft.util.math.Direction;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;

@SearchTags({"AutoTrap", "trap hack", "auto trap", "Crystal trap"})
public final class AutoTrapHack extends Hack implements UpdateListener
{
    private ArrayList<AbstractClientPlayerEntity> trapPlayers;
	
	public AutoTrapHack()
	{
		super("AutoTrap", "traps players near you in blocks");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	public void onEnable()
	{
        trapPlayers = StreamSupport.stream(MC.world.getPlayers().spliterator(), true)
            .filter(e -> !WURST.getFriends().contains(e.getEntityName()))
            .filter(e -> e != MC.player)
			.filter(e -> e.getPos().distanceTo(MC.player.getPos()) < 6)
            .collect(Collectors.toCollection(ArrayList::new));
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
        if(!trapPlayers.isEmpty())
        {
			ArrayList<BlockPos> trapBlocks = new ArrayList<>();
            AbstractClientPlayerEntity target = trapPlayers.get(0);

			Vec3d farthestDirection = getDirection(target);
			Box targetBox = target.getBoundingBox();

			for(BlockPos block : startingBlock(farthestDirection, target.getBlockPos(), targetBox))
			{
				trapBlocks.add(block);
			}
			ArrayList<BlockPos> upBlocks = findInDirection(new Vec3d(0,1,0), trapBlocks.get(trapBlocks.size()-1), targetBox, 6, 0, 6, true);
			for(BlockPos block : upBlocks)
			{
				trapBlocks.add(block);
			}

			ArrayList<BlockPos> topBlocks = findInDirection(farthestDirection.multiply(-1), trapBlocks.get(trapBlocks.size()-1), targetBox, 0, 6, 0, true);
			boolean shouldPlaceExtra = true;
			for(int i = 0; i < topBlocks.size(); i++)
			{
				BlockPos block = topBlocks.get(i);
				trapBlocks.add(block);
				if(i < topBlocks.size()-1)
				{
					ArrayList<BlockPos> sideList1 = findInDirection(new Vec3d(farthestDirection.getZ(), 0, farthestDirection.getX()), trapBlocks.get(trapBlocks.size()-1), targetBox, 0, 6, 0, shouldPlaceExtra);
					ArrayList<BlockPos> sideList2 = findInDirection(new Vec3d(-farthestDirection.getZ(), 0, -farthestDirection.getX()), trapBlocks.get(trapBlocks.size()-1), targetBox, 0, 6, 0, shouldPlaceExtra);
					for(BlockPos sideBlock : sideList1)
					{
						trapBlocks.add(sideBlock);
					}
					for(BlockPos sideBlock : sideList2)
					{
						trapBlocks.add(sideBlock);
					}
					if(shouldPlaceExtra)
					{
						trapBlocks.add(sideList1.get(sideList1.size()-1).down());
						ArrayList<BlockPos> sideMid1 = findInDirection(farthestDirection, trapBlocks.get(trapBlocks.size()-1), targetBox, farthestDirection.getZ()*6, 6, farthestDirection.getX()*6, false);
						ArrayList<BlockPos> sideMid2 = findInDirection(farthestDirection.multiply(-1), trapBlocks.get(trapBlocks.size()-1), targetBox, farthestDirection.getZ()*6, 6, farthestDirection.getX()*6, false);
						for(BlockPos sideMidBlock : sideMid1)
						{
							trapBlocks.add(sideMidBlock);
						}
						for(BlockPos sideMidBlock : sideMid2)
						{
							trapBlocks.add(sideMidBlock);
						}
						trapBlocks.add(sideList2.get(sideList2.size()-1).down());
						sideMid1 = findInDirection(farthestDirection, trapBlocks.get(trapBlocks.size()-1), targetBox, farthestDirection.getZ()*6, 6, farthestDirection.getX()*6, false);
						sideMid2 = findInDirection(farthestDirection.multiply(-1), trapBlocks.get(trapBlocks.size()-1), targetBox, farthestDirection.getZ()*6, 6, farthestDirection.getX()*6, false);
						for(BlockPos sideMidBlock : sideMid1)
						{
							trapBlocks.add(sideMidBlock);
						}
						for(BlockPos sideMidBlock : sideMid2)
						{
							trapBlocks.add(sideMidBlock);
						}
					}
				}
				shouldPlaceExtra = false;
			}
			trapBlocks.add(topBlocks.get(topBlocks.size()-1).down());
			ArrayList<BlockPos> sideMid1 = findInDirection(new Vec3d(farthestDirection.getZ(), 0, farthestDirection.getX()), trapBlocks.get(trapBlocks.size()-1), targetBox, farthestDirection.getX()*6, 6, farthestDirection.getZ()*6, false);
			ArrayList<BlockPos> sideMid2 = findInDirection(new Vec3d(-farthestDirection.getZ(), 0, -farthestDirection.getX()), trapBlocks.get(trapBlocks.size()-1), targetBox, farthestDirection.getX()*6, 6, farthestDirection.getZ()*6, false);
			for(BlockPos sideMidBlock : sideMid1)
			{
				trapBlocks.add(sideMidBlock);
			}
			for(BlockPos sideMidBlock : sideMid2)
			{
				trapBlocks.add(sideMidBlock);
			}

			ArrayList<BlockPos> sideMidBack1 = findInDirection(new Vec3d(farthestDirection.getZ(), 0, farthestDirection.getX()), upBlocks.get(upBlocks.size()-1).down(), targetBox, farthestDirection.getX()*6, 6, farthestDirection.getZ()*6, false);
			ArrayList<BlockPos> sideMidBack2 = findInDirection(new Vec3d(-farthestDirection.getZ(), 0, -farthestDirection.getX()), upBlocks.get(upBlocks.size()-1).down(), targetBox, farthestDirection.getX()*6, 6, farthestDirection.getZ()*6, false);
			for(BlockPos sideMidBlock : sideMidBack1)
			{
				trapBlocks.add(sideMidBlock);
			}
			for(BlockPos sideMidBlock : sideMidBack2)
			{
				trapBlocks.add(sideMidBlock);
			}

			for(BlockPos block : trapBlocks)
			{
				if(BlockUtils.getState(block).getMaterial().isReplaceable())
				{
					placeBlockSimple(block);
				}
			}
			trapPlayers.remove(0);
        }
		else
		{
			setEnabled(false);
			return;
		}
    }
	private ArrayList<BlockPos> startingBlock(Vec3d direction, BlockPos targetPos, Box target)
	{
		ArrayList<BlockPos> blockPlacements = new ArrayList<>();
		for(int i = 1; i <= 2; i++)
		{
			BlockPos currentBlock = targetPos.add(new BlockPos(direction.multiply(i)));
			if(!target.intersects(new Box(currentBlock)))
			{
				//This is because I am too lazy to make a function right now
				if(BlockUtils.getState(currentBlock.add(0, -1, 0)).getMaterial().isReplaceable())
				{
						if(BlockUtils.getState(currentBlock.add(0, -2, 0)).getMaterial().isReplaceable())
						{
								blockPlacements.add(currentBlock.add(0, -2, 0));	
						}
						blockPlacements.add(currentBlock.add(0, -1, 0));
				}
				blockPlacements.add(currentBlock);
				return blockPlacements;
			}
		}
		return blockPlacements;
	}
    private ArrayList<BlockPos> findInDirection(Vec3d direction, BlockPos startingBlock, Box target, double xStretch, double yStretch, double zStretch, boolean shouldPlaceExtra)
    {
        ArrayList<BlockPos> blockPlacements = new ArrayList<>();
        for(int i = 1; i <= 20; i++)
        {
			BlockPos currentBlock = startingBlock.add(new BlockPos(direction.multiply(i)));
			if(shouldPlaceExtra || target.stretch(xStretch, yStretch, zStretch).stretch(-xStretch, -yStretch, -zStretch).intersects(new Box(currentBlock)))
			{
            blockPlacements.add(currentBlock);
			}
            if(!target.stretch(xStretch, yStretch, zStretch).stretch(-xStretch, -yStretch, -zStretch).intersects(new Box(currentBlock)))
            {
				return blockPlacements;
            }
        }
		return blockPlacements;
    }
	private Vec3d getDirection(AbstractClientPlayerEntity target)
	{
		Vec3d direction = new Vec3d(0, 0, 0);
		for(int i = 0; i < 4; i++)
		{
			Vec3d additionVector = new Vec3d(1, 0, 0);
			if (i == 1)
			{
				additionVector = new Vec3d(-1, 0, 0);
			}
			else if (i == 2)
			{
				additionVector = new Vec3d(0, 0, 1);
			}
			else if (i == 3)
			{
				additionVector = new Vec3d(0, 0, -1);
			}

			if(MC.player.getPos().distanceTo(target.getPos().add(additionVector)) > MC.player.getPos().distanceTo(target.getPos().add(direction)))
			{
				direction=additionVector;
			}
		}
		return direction;
	}

    private void placeBlockSimple(BlockPos pos)
	{
		Direction side = null;
		Direction[] sides = Direction.values();
		
		//Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		//double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		Vec3d[] hitVecs = new Vec3d[sides.length];
		for(int i = 0; i < sides.length; i++)
			hitVecs[i] =
				posVec.add(Vec3d.of(sides[i].getVector()).multiply(0.5));
		
		for(int i = 0; i < sides.length; i++)
		{
			// check if neighbor can be right clicked
			BlockPos neighbor = pos.offset(sides[i]);
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			// check line of sight
			BlockState neighborState = BlockUtils.getState(neighbor);
			VoxelShape neighborShape =
				neighborState.getOutlineShape(MC.world, neighbor);
			//if(MC.world.raycastBlock(eyesPos, hitVecs[i], neighbor,
			//	neighborShape, neighborState) != null)
			//	continue;
			
			side = sides[i];
			break;
		}
		
		if(side == null)
			for(int i = 0; i < sides.length; i++)
			{
				// check if neighbor can be right clicked
				if(!BlockUtils.canBeClicked(pos.offset(sides[i])))
					continue;
				
				// check if side is facing away from player
				//if(distanceSqPosVec > eyesPos.squaredDistanceTo(hitVecs[i]))
				//	continue;
				
				side = sides[i];
				break;
			}
		
		if(side == null)
			return;
		
		Vec3d hitVec = hitVecs[side.ordinal()];
		
		// face block
		// WURST.getRotationFaker().faceVectorPacket(hitVec);
		// if(RotationUtils.getAngleToLastReportedLookVec(hitVec) > 1)
		// return;
		
		// check timer
		// if(IMC.getItemUseCooldown() > 0)
		// return;
		
		// place block
		IMC.getInteractionManager().rightClickBlock(pos.offset(side),
			side.getOpposite(), hitVec);
		
		// swing arm
		MC.player.networkHandler
			.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
		
		// reset timer
		IMC.setItemUseCooldown(4);
	}
}
