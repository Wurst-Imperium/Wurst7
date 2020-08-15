/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 * 
 * Auto MLG By Dj-jom2x
 */

package net.wurstclient.hacks;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;

@SearchTags({ "mlg", "water", "mlg water" })
public final class AutoMLGHack extends Hack implements UpdateListener {

    private final SliderSetting mfall = new SliderSetting("Minimum Fall", "Minimum Fall Distance", 3, 3, 15, 1, ValueDisplay.INTEGER);
    
    private int timer = 10;
    
    private BlockPos last_pos = null;

    public AutoMLGHack() {
        super("AutoMLG", "Auto MLG Save");
        setCategory(Category.MOVEMENT);
        addSetting(mfall);
    }

    @Override
    public void onEnable() {
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    public void onDisable() {
        EVENTS.remove(UpdateListener.class, this);
    }

    private boolean placeBlock(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(MC.player.getX(), MC.player.getY() + MC.player.getEyeHeight(MC.player.getPose()),
                MC.player.getZ());

        Vec3d posVec = Vec3d.ofCenter(pos);

        WURST.getRotationFaker().faceVectorClient(posVec);

        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.offset(side);
            Direction side2 = side.getOpposite();

            // check if side is visible (facing away from player)
            if (eyesPos.squaredDistanceTo(Vec3d.ofCenter(pos)) >= eyesPos.squaredDistanceTo(Vec3d.ofCenter(neighbor)))
                continue;

            // check if neighbor can be right clicked
            if (!BlockUtils.canBeClicked(neighbor))
                continue;

            Vec3d hitVec = Vec3d.ofCenter(neighbor).add(Vec3d.of(side2.getVector()).multiply(0.5));

            // check if hitVec is within range (4.25 blocks)
            if (eyesPos.squaredDistanceTo(hitVec) > 18.0625)
                continue;

            // place block
            Rotation rotation = RotationUtils.getNeededRotations(hitVec);
            PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(rotation.getYaw(),
                    rotation.getPitch(), MC.player.isOnGround());
            MC.player.networkHandler.sendPacket(packet);
            IMC.getInteractionManager().rightClickBlock(neighbor, side2, hitVec);
            MC.player.swingHand(Hand.MAIN_HAND);
            IMC.setItemUseCooldown(4);

            return true;
        }

        return false;
    }
    

    @Override
    public void onUpdate() {

        final ClientPlayerEntity player = MC.player;
        
        if(timer == 1 && last_pos != null){
            placeBlock(last_pos);
            last_pos = null;
        }

        if (player.fallDistance <= mfall.getValueF()) {
            return;
        }

        final BlockPos bp = new BlockPos(player.getPos()).down(2);
        Block block = BlockUtils.getBlock(bp);

        if(!(block instanceof AirBlock) && !(block instanceof FluidBlock)){
            int slot = player.inventory.getSlotWithStack(new ItemStack(Items.WATER_BUCKET));
            if (slot >= 0 && slot < 9) {
                player.pitch = 90;
                player.inventory.selectedSlot = slot;
                if(placeBlock(bp)){
                    last_pos = bp;
                    timer = 1;
                    return;
                }
            }
        }
     
        timer++;
    }
}
