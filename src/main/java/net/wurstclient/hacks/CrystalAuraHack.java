package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"crystal aura"})
public class CrystalAuraHack extends Hack implements UpdateListener {

    private final SliderSetting range = new SliderSetting("CrystalAura Range", "How far CrystalAura will reach", 4.25, 1, 10, 0.05, SliderSetting.ValueDisplay.DECIMAL);

    private int delay = 0;

    public CrystalAuraHack() {
        super("CrystalAura", "Automatically hits end crystals");
        this.setCategory(Category.COMBAT);
        this.addSetting(range);
    }

    @Override
    public void onEnable(){
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    public void onDisable(){
        EVENTS.remove(UpdateListener.class, this);
    }

    @Override
    public void onUpdate() {
        delay++;
        int requiredDelay = (int) Math.round(20/16);

        for(Entity e: MC.world.getEntities()){
            if (e instanceof EndCrystalEntity && MC.player.distanceTo(e) < range.getValueF()) {
                if (delay > requiredDelay || requiredDelay == 0) {
                    MC.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(e, MC.player.isSneaking()));
                    MC.player.attack(e);
                    MC.player.swingHand(Hand.MAIN_HAND);
                    delay=0;
        }
    }
}}}
