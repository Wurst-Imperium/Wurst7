package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"AutoCrytal", "auto crytal", "AutoRespawnAnchor", "auto respawn anchor", "Crystal hack", "respawn anchor", "pvp"})
public final class AutoCrystalHack extends Hack implements UpdateListener
{
	private int ticksPassed;
	private int prevSelected;

	private final SliderSetting tickDelay = new SliderSetting("Tick Delay",
		"How long to wait in between placing in ticks.\n",
		4, 1, 20, 1, v -> (int)v + "");
	
	public AutoCrystalHack()
	{
		super("AutoCrystal", "does crystal pvp for you");
		setCategory(Category.COMBAT);
		addSetting(tickDelay);
	}
	
	@Override
	public void onEnable()
	{
		prevSelected = MC.player.getInventory().selectedSlot;
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
		ticksPassed++;
		//Gets all players to crytal fight
		ArrayList<AbstractClientPlayerEntity> players = StreamSupport.stream(MC.world.getPlayers().spliterator(), true)
			.filter(e -> !WURST.getFriends().contains(e.getEntityName()))
			.filter(e -> e != MC.player)
			.filter(e -> e.getPos().distanceTo(MC.player.getPos()) < 6)
			.collect(Collectors.toCollection(ArrayList::new));

		for(AbstractClientPlayerEntity targetPlayer : players)
		{
			BlockPos targetPos = targetPlayer.getBlockPos();
			
			ArrayList<BlockPos> layerBlocks = BlockUtils.getAllInBox(new BlockPos(MC.player.getBlockX()-6,MC.player.getBlockY()-6,MC.player.getBlockZ()-6), new BlockPos(MC.player.getBlockX()+6,MC.player.getBlockY()+6,MC.player.getBlockZ()+6));
			//use anchor
			ArrayList<BlockPos> blockFilter = filterType(layerBlocks, Blocks.RESPAWN_ANCHOR);
			if(!MC.player.getEntityWorld().getRegistryKey().equals(World.NETHER))
			{
				for(BlockPos block : blockFilter)
				{
					if(BlockUtils.getState(block).equals(Blocks.RESPAWN_ANCHOR.getDefaultState()))
					{
						selectBestSlot(Items.GLOWSTONE, false);
					}
					else
					{
						selectBestSlot(Items.GLOWSTONE, true);
					}
					rightClickBlockSimple(block);
					MC.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
				}
				blockFilter = filterType(layerBlocks, Blocks.AIR);
				blockFilter = filterCanPlace(blockFilter);
				if(!blockFilter.isEmpty() || ticksPassed > tickDelay.getValueI()-1)
				{
					ticksPassed = 0;
					BlockPos bestBlock = blockFilter.get(0);
					for(BlockPos curBlock : blockFilter)
					{
						if(new Vec3d(curBlock.getX()+.5, curBlock.getY()+.5, curBlock.getZ()+.5).distanceTo(targetPlayer.getPos()) < new Vec3d(bestBlock.getX()+.5, bestBlock.getY()+.5, bestBlock.getZ()+.5).distanceTo(targetPlayer.getPos()))
						{
							bestBlock = curBlock;
						}
					}
					selectBestSlot(Items.RESPAWN_ANCHOR, false);
					placeBlockSimple(bestBlock);
					MC.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
					System.out.println("BlockPlaced:" + bestBlock);
				}
			}
		}			
    }
	private void placeBlockSimple(BlockPos pos)
	{
		Direction side = null;
		Direction[] sides = Direction.values();
		
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
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
			if(MC.world.raycastBlock(eyesPos, hitVecs[i], neighbor,
				neighborShape, neighborState) != null)
				continue;
			
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
				if(distanceSqPosVec > eyesPos.squaredDistanceTo(hitVecs[i]))
					continue;
				
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
	public boolean selectBestSlot(Item itemType, boolean useNegitive)
	{
		Inventory inventory = MC.player.getInventory();
		int hasBlock = -1;
		for(int slot = 0; slot < 9; slot++)
		{
		 	ItemStack stack = inventory.getStack(slot);

			if((!useNegitive && stack.getItem() == itemType) || (useNegitive && stack.getItem() != itemType))
			{
				MC.player.getInventory().selectedSlot = slot;
				return true;
			}
		}
		if(hasBlock != -1)
		{
			MC.player.getInventory().selectedSlot = hasBlock;
			return true;
		}
		else
		{
			return false;
		}
	}
    private ArrayList<BlockPos> filterType(ArrayList<BlockPos> blocks, Block blockType)
    {
		ArrayList<BlockPos> returnlist = new ArrayList<>();
        for(BlockPos block : blocks)
        {
            if(BlockUtils.getBlock(block).equals(blockType) || ((blockType.equals(Blocks.AIR)) && BlockUtils.getState(block).getMaterial().isReplaceable()))
            {
                returnlist.add(block);
            }
        }
        return returnlist;
    }
	private ArrayList<BlockPos> filterCanPlace(ArrayList<BlockPos> blocks)
	{
		ArrayList<BlockPos> returnlist = new ArrayList<>();
		ArrayList<Entity> entities = StreamSupport.stream(MC.world.getEntities().spliterator(), true)
			.filter(e -> e.getPos().distanceTo(MC.player.getPos()) < 6)
            .collect(Collectors.toCollection(ArrayList::new));
		for(BlockPos block : blocks)
		{
			boolean canReturn = true;
			for(Entity entity : entities)
			{
				if(entity.getBoundingBox().intersects(block.getX(), block.getY(), block.getZ(), block.getX()+1f, block.getY()+1f, block.getZ()+1f))
				{
					canReturn = false;
				}
			}
			if(canReturn)
			{
				returnlist.add(block);
			}
		}
		return returnlist;
	}
}
	
