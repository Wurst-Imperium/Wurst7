/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Npc;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixin.MinecraftClientInvoker;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;

@DontSaveState
@SearchTags({"auto clicker", "clicker"})
public class AutoClickerHack extends Hack implements UpdateListener {
    private int timer;

    private final SliderSetting reach = new SliderSetting("Max reach",
            5, 1, 6, 0.5, SliderSetting.ValueDisplay.DECIMAL);

    private final SliderSetting ClickInterval = new SliderSetting("Click interval", "seconds between clicks",
            1, 0.05, 60, 0.05, SliderSetting.ValueDisplay.DECIMAL.withLabel(0.05, "Every tick"));

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.values(), Mode.ATTACK);

    private final CheckboxSetting freecamCompatibility = new CheckboxSetting("Freecam compatibility",
            "Ray cast from server side player position. May not work with 3rd party freecam mods", true);

    private final CheckboxSetting openUI = new CheckboxSetting("Open user interfaces",
            "open chests and other things. Not recommended to turn on", false);

    public AutoClickerHack() {
        super("AutoClicker");
        setCategory(Category.OTHER);
        addSetting(reach);
        addSetting(ClickInterval);
        addSetting(mode);
        addSetting(freecamCompatibility);
        addSetting(openUI);
    }

    @Override
    public String getRenderName() {
        return getName() + " [" + mode.getSelected() + "]";
    }

    @Override
    public void onEnable() {
        EVENTS.add(UpdateListener.class, this);
        timer = 0;
    }

    @Override
    public void onDisable() {
        EVENTS.remove(UpdateListener.class, this);
    }

    public boolean isTimeToAttack(){
        if (ClickInterval.getValue() > 0)
            return timer >= (20 * ClickInterval.getValue());

        return MC.player.getAttackCooldownProgress(0) >= 1;
    }

    @Override
    public void onUpdate(){
        timer += 1;
        if (!isTimeToAttack())
            return;
        timer = 0;

        HitResult target = getTarget();
        if (target == null)
            return;
        MC.crosshairTarget = target;

        if (mode.getSelected() == Mode.ATTACK || mode.getSelected() == Mode.BOTH)
            ((MinecraftClientInvoker) MC).callDoAttack();

        if (mode.getSelected() == Mode.USE || mode.getSelected() == Mode.BOTH){
            if (!openUI.isChecked() && !willOpenUI(target))
                ((MinecraftClientInvoker) MC).callDoItemUse();
        }
    }

    public boolean willOpenUI(HitResult hitResult) {
        if (hitResult == null)
            return false;

        if (hitResult instanceof BlockHitResult blockHitResult) {
            boolean holdingItem = !MC.player.getMainHandStack().isEmpty() || !MC.player.getOffHandStack().isEmpty();

            return MC.world.getBlockState(blockHitResult.getBlockPos())
                    .createScreenHandlerFactory(MC.world, blockHitResult.getBlockPos()) != null &&
                    !(MC.player.shouldCancelInteraction() && holdingItem);

        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            return entity instanceof Npc || entity instanceof StorageMinecartEntity ||
                    (MC.player.shouldCancelInteraction() && entity instanceof RideableInventory);

        }

        return false;
    }

    public HitResult getTarget() {
        if (freecamCompatibility.isChecked()) {
            FreecamHack freecam = WURST.getHax().freecamHack;
            Entity entity = freecam.isEnabled() ? freecam.getFreecam() : MC.player;
            double reach = this.reach.getValue();

            HitResult block = entity.raycast(reach, 1, false);
            Vec3d startPos = entity.getCameraPosVec(1);

            double reachSquared = block != null ? block.getPos().squaredDistanceTo(startPos) : reach * reach;

            Vec3d rot = entity.getRotationVec(1).multiply(reach);
            Vec3d endPos = startPos.add(rot);
            Box box = entity.getBoundingBox().stretch(rot).expand(1);

            EntityHitResult target = ProjectileUtil.raycast(entity, startPos, endPos, box,
                    e -> !e.isSpectator() && e.canHit() && e != MC.player, reachSquared);

            return target == null ? block : target;
        }

        if (MC.crosshairTarget.squaredDistanceTo(MC.player) < reach.getValue() * reach.getValue())
            return MC.crosshairTarget;

        return null;
    }

    private enum Mode {
        ATTACK("attack"),
        USE("use"),
        BOTH("both");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
