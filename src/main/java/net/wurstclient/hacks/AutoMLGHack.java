/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.SeaPickleBlock;
import net.minecraft.block.SeagrassBlock;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;

import java.util.Arrays;
import java.util.List;

@SearchTags({ "mlg", "water", "mlg water", "mlg powder snow"})
public final class AutoMLGHack extends Hack implements UpdateListener {

    private final SliderSetting minFall = new SliderSetting("Minimum fall", "Minimum fall distance", 3, 3, 15, 1, ValueDisplay.INTEGER);
    private final CheckboxSetting anchor = new CheckboxSetting("Anchor", "Teleport you to the middle of a block", true);
    private final CheckboxSetting doSneak = new CheckboxSetting("Do sneak", "Sneak to avoid interacting with the block underneath", false);
    private final CheckboxSetting allowInv = new CheckboxSetting("Allow inventory", "Allow taking buckets out of your inventory", false);
    private final CheckboxSetting snowFirst = new CheckboxSetting("Snow first", "Try powder snow first, which is less reliable", false);
    private final CheckboxSetting checkElytra = new CheckboxSetting("Check elytra", "Disable the hack when flying with an elytra", false);

    private boolean placedWater = false;

    private boolean waterBucket = true;
    private boolean offHand = false;
    private int pickupRetry = 0;
    
    private BlockPos targetBlock = null;
    private Vec2f lastRot = null;
    private Vec3d lastPlayerPos = null;
    private static final List<ItemStack> BUCKETS = Arrays.asList(
        new ItemStack(Items.WATER_BUCKET),
        new ItemStack(Items.POWDER_SNOW_BUCKET)
    );

    public AutoMLGHack() {
        super("AutoMLG");
        setCategory(Category.MOVEMENT);
        addSetting(minFall);
        addSetting(anchor);
        addSetting(doSneak);
        addSetting(allowInv);
        addSetting(snowFirst);
        addSetting(checkElytra);
    }
    
    @Override
    public void onEnable() {
        placedWater = false;
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    public void onDisable() {
        EVENTS.remove(UpdateListener.class, this);
    }

    private boolean rightClickBlock(BlockPos pos) {
        Vec3d hitVec = Vec3d.ofCenter(pos).add(Vec3d.of(Direction.UP.getVector()).multiply(0.5));

        Rotation rotation = RotationUtils.getNeededRotations(hitVec);
        MC.player.setYaw(rotation.getYaw());
        MC.player.setPitch(rotation.getPitch());
        PlayerMoveC2SPacket.LookAndOnGround packet = new PlayerMoveC2SPacket.LookAndOnGround(rotation.getYaw(),
            rotation.getPitch(), MC.player.isOnGround());
        MC.player.networkHandler.sendPacket(packet);

        Vec3d eyesPos = new Vec3d(MC.player.getX(), MC.player.getY() + MC.player.getEyeHeight(
                MC.player.getPose()), MC.player.getZ());
        // check if hitVec is within range (4.25 blocks)
        if (eyesPos.squaredDistanceTo(hitVec) > 18.0625)
            return false;

        if (!placedWater && !waterBucket) {
            MC.interactionManager.interactBlock(MC.player, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND,
                new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false));
        }
        else {
            MC.interactionManager.interactItem(MC.player, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        }
        return true;
    }

    private void restoreRotation() {
        if (lastRot == null) return;
        MC.player.setPitch(lastRot.x);
        MC.player.setYaw(lastRot.y);
        lastRot = null;
    }
    
    @Override
    public void onUpdate() {
        if (MC.player.getAbilities().creativeMode) return;
        if (!placedWater) {
            float minfallDistance = minFall.getValueF();
            if (MC.player.fallDistance > minfallDistance - 2.0f && !MC.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !(checkElytra.isChecked() && MC.player.isFallFlying())) {
                Vec3d playerPos = MC.player.getPos();
                if (lastPlayerPos == null) lastPlayerPos = playerPos;
                if (playerPos.y - lastPlayerPos.y < 0.0D) {
                    BlockHitResult result = MC.world.raycast(new RaycastContext(MC.player.getPos(), playerPos.subtract(0, 5, 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, MC.player));
                    BlockPos bp = result.getBlockPos();
                    if (result != null && result.getType() == HitResult.Type.BLOCK
                            && Math.max(0.0, (float)playerPos.y - (float)(bp.getY())) - 1.3f + MC.player.fallDistance > minfallDistance
                            && causeFallDamage(BlockUtils.getState(result.getBlockPos()))
                            && causeFallDamage(BlockUtils.getState(result.getBlockPos().up()))
                    ) {
                        for (ItemStack bucket : (snowFirst.isChecked() ? Lists.reverse(BUCKETS) : BUCKETS)) {
                            if (MC.world.getDimension().ultrawarm() && bucket.getItem().equals(Items.WATER_BUCKET))
                                continue;
                            int location = takeStackOut(bucket);
                            if (location == 0) continue;
                            offHand = location < 0;
                            waterBucket = bucket.getItem().equals(Items.WATER_BUCKET);
                            if (anchor.isChecked()) {
                                double x = MathHelper.floor(MC.player.getX()) + 0.5;
                                double z = MathHelper.floor(MC.player.getZ()) + 0.5;
                                if ((Math.abs(MC.player.getX() - x) > 1e-5) || (Math.abs(MC.player.getZ() - z) > 1e-5)) {
                                    MC.player.setPosition(x, MC.player.getY(), z);
                                    MC.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(MC.player.getX(), MC.player.getY(), MC.player.getZ(), MC.player.isOnGround()));
                                }
                                MC.player.setVelocity(0, MC.player.getVelocity().y, 0);
                            }
                            if (lastRot == null) lastRot = MC.player.getRotationClient();
                            if (rightClickBlock(bp)) {
                                placedWater = true;
                                targetBlock = bp;
                                pickupRetry = 0;
                            }
                            break;
                        }
                    }
                }
                lastPlayerPos = playerPos;
            }
            else {
                lastRot = null;
            }
        } else {
            if (MC.player.isOnGround() || MC.player.isTouchingWater() && MC.itemUseCooldown < 1) {
                int location = takeStackOut(new ItemStack(Items.BUCKET));
                if (location != 0) {
                    offHand = location < 0;
                    rightClickBlock(targetBlock);
                    if ((offHand ? MC.player.getOffHandStack() : MC.player.getInventory().getMainHandStack()).getItem().equals(Items.BUCKET)) {
                        pickupRetry++;
                        if (pickupRetry <= 10) {
                            return;
                        }
                    }
                }
                if (doSneak.isChecked()) {
                    IKeyBinding sneakKey = (IKeyBinding) MC.options.sneakKey;
                    ((KeyBinding) sneakKey).setPressed(sneakKey.isActallyPressed());
                }
                restoreRotation();
                lastPlayerPos = null;
                placedWater = false;
            }
        }
    }
    private boolean causeFallDamage(BlockState bs) {
        if (bs.isAir()) return true;
        Block block = bs.getBlock();
        if (block instanceof FluidBlock || block instanceof SeaPickleBlock || block instanceof SeagrassBlock) return false;
        // todo slabs and more?
        return true;
    }
    
    private int takeStackOut(ItemStack stack) {
        PlayerInventory inv = MC.player.getInventory();
        int slot = inv.getSlotWithStack(stack);
        if (slot >= 0 && slot <= 8) {
            // bucket in hotbar
            inv.selectedSlot = slot;
            return 1;
        }
        else if (slot >= 9 && slot <= 35) {
            // bucket in inventory
            if (allowInv.isChecked()) {
                IMC.getInteractionManager().windowClick_SWAP(slot, inv.selectedSlot);
                return 1;
            }
        }
        else {
            // no bucket in inventory
            if (ItemStack.areItemsEqual(MC.player.getOffHandStack(), stack)) {
                // use bucket in offhand
                return -1;
            }
        }
        return 0;
    }
}
