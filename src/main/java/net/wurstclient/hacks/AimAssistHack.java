package net.wurstclient.hacks;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.settings.filters.*;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.Rotation;
import net.wurstclient.util.RotationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class AimAssistHack extends Hack implements UpdateListener, RenderListener {
    private final SliderSetting range = new SliderSetting("Range", 4.5, 1, 6, 0.05, SliderSetting.ValueDisplay.DECIMAL);
    private final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed", 600, 10, 3600, 10, SliderSetting.ValueDisplay.DEGREES.withSuffix("/s"));
    private final SliderSetting fov = new SliderSetting("FOV", "Field Of View", 120, 30, 360, 10, SliderSetting.ValueDisplay.DEGREES);
    private final CheckboxSetting checkLOS = new CheckboxSetting("Check line of sight", true);
    private final EntityFilterList entityFilters = new EntityFilterList(
            FilterPlayersSetting.genericCombat(false),
            FilterSleepingSetting.genericCombat(false),
            FilterFlyingSetting.genericCombat(0),
            FilterHostileSetting.genericCombat(false),
            FilterNeutralSetting.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
            FilterPassiveSetting.genericCombat(true),
            FilterPassiveWaterSetting.genericCombat(true),
            FilterBabiesSetting.genericCombat(true),
            FilterBatsSetting.genericCombat(true),
            FilterSlimesSetting.genericCombat(true),
            FilterPetsSetting.genericCombat(true),
            FilterVillagersSetting.genericCombat(true),
            FilterZombieVillagersSetting.genericCombat(true),
            FilterGolemsSetting.genericCombat(false),
            FilterPiglinsSetting.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
            FilterZombiePiglinsSetting.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
            FilterEndermenSetting.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
            FilterShulkersSetting.genericCombat(false),
            FilterInvisibleSetting.genericCombat(true),
            FilterNamedSetting.genericCombat(false),
            FilterShulkerBulletSetting.genericCombat(false),
            FilterArmorStandsSetting.genericCombat(true),
            FilterCrystalsSetting.genericCombat(true)
    );

    private Entity target;
    private float nextYaw;
    private float nextPitch;

    public AimAssistHack() {
        super("AimAssist", Category.COMBAT);
        addSettings();
    }

    private void addSettings() {
        addSetting(range);
        addSetting(rotationSpeed);
        addSetting(fov);
        addSetting(checkLOS);
        entityFilters.forEach(this::addSetting);
    }

    @Override
    protected void onEnable() {
        disableIncompatibleHacks();
        registerListeners();
    }

    private void disableIncompatibleHacks() {
        getIncompatibleHacks().forEach(Hack::setEnabled);
    }

    private void registerListeners() {
        EVENTS.add(UpdateListener.class, this);
        EVENTS.add(RenderListener.class, this);
    }

    @Override
    protected void onDisable() {
        unregisterListeners();
        target = null;
    }

    private void unregisterListeners() {
        EVENTS.remove(UpdateListener.class, this);
        EVENTS.remove(RenderListener.class, this);
    }

    @Override
    public void onUpdate() {
        if (isContainerOpen())
            return;

        target = findTarget();

        if (target == null)
            return;

        Vec3d hitVec = target.getBoundingBox().getCenter();
        if (shouldCheckLOS() && !BlockUtils.hasLineOfSight(hitVec)) {
            target = null;
            return;
        }

        handleTarget();
    }

    private boolean isContainerOpen() {
        return MC.currentScreen instanceof HandledScreen;
    }

    private Entity findTarget() {
        return filterEntities(EntityUtils.getAttackableEntities())
                .min(Comparator.comparingDouble(e -> RotationUtils.getAngleToLookVec(e.getBoundingBox().getCenter())))
                .orElse(null);
    }

    private Stream<Entity> filterEntities(Stream<Entity> entityStream) {
        return entityStream.filter(e -> MC.player.squaredDistanceTo(e) <= Math.pow(range.getValue(), 2))
                .filter(e -> fov.getValue() == 360 || RotationUtils.getAngleToLookVec(e.getBoundingBox().getCenter()) <= fov.getValue() / 2.0)
                .filter(entityFilters::passes);
    }

    private boolean shouldCheckLOS() {
        return checkLOS.isChecked();
    }

    private void handleTarget() {
        WURST.getHax().autoSwordHack.setSlot(target);
        faceEntityClient(target);
    }

    private void faceEntityClient(Entity entity) {
        Box box = entity.getBoundingBox();
        Rotation needed = RotationUtils.getNeededRotations(box.getCenter());
        Rotation next = RotationUtils.slowlyTurnTowards(needed, rotationSpeed.getValueI() / 20F);
        nextYaw = next.yaw();
        nextPitch = next.pitch();
        if (!(RotationUtils.isAlreadyFacing(needed) || RotationUtils.isFacingBox(box, range.getValue()))) {
            interpolatePlayerRotation();
        }
    }

    private void interpolatePlayerRotation() {
        float oldYaw = MC.player.prevYaw;
        float oldPitch = MC.player.prevPitch;
        MC.player.setYaw(MathHelper.lerp(MC.getTickDelta(), oldYaw, nextYaw));
        MC.player.setPitch(MathHelper.lerp(MC.getTickDelta(), oldPitch, nextPitch));
    }

    @Override
    public void onRender(MatrixStack matrixStack, float partialTicks) {
        if (target == null)
            return;

        interpolatePlayerRotation();
    }

    private List<Hack> getIncompatibleHacks() {
        return List.of(
                WURST.getHax().autoFishHack,
                WURST.getHax().clickAuraHack,
                WURST.getHax().crystalAuraHack,
                WURST.getHax().fightBotHack,
                WURST.getHax().killauraHack,
                WURST.getHax().killauraLegitHack,
                WURST.getHax().multiAuraHack,
                WURST.getHax().protectHack,
                WURST.getHax().tpAuraHack
        );
    }
}
