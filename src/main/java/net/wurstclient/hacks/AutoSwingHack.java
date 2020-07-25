package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.wurstclient.Category;
import net.wurstclient.events.BlockBreakingProgressListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

public class AutoSwingHack extends Hack implements UpdateListener {

    private final CheckboxSetting swingAnimation = new CheckboxSetting("Swing Animation",
            "Whether or not to do the\n" + "swing animation.", true);

    public AutoSwingHack() {
        super("AutoSwing", "Continuously swings if you can hit an entity");
        setCategory(Category.COMBAT);
        addSetting(swingAnimation);
    }

    @Override
    public void onUpdate() {
        if (MC.options.keyAttack.isPressed() && MC.player != null && MC.player.getAttackCooldownProgress(0.0f) >= 1.0f && MC.crosshairTarget.getType() == HitResult.Type.ENTITY && swingAnimation.isChecked()) {
            Entity e = ((EntityHitResult)MC.crosshairTarget).getEntity();
            if(e.isAlive() && e.isAttackable()){
                MC.interactionManager.attackEntity(MC.player, e);
                if(swingAnimation.isChecked()) {
                    MC.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }
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
}
