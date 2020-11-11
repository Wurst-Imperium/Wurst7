package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;

import static net.minecraft.util.Hand.MAIN_HAND;
import static net.wurstclient.Category.COMBAT;
import static net.wurstclient.settings.SliderSetting.ValueDisplay.DECIMAL;

@SearchTags({"crystal aura", "AutoCrystal", "auto crystal"})
public final class CrystalAuraHack extends Hack implements UpdateListener {

    SliderSetting rSetting = new SliderSetting(
            "Range",
            "Range where player would hit the end crystal. Note that high range might not work.",
            5,
            1,
            20,
            1,
            DECIMAL
    );

    SliderSetting cpsSetting = new SliderSetting(
            "Clicks Per Second",
            "How many clicks per second. Note that setting higher values might get you kicked on some servers.",
            12,
            1,
            25,
            1,
            DECIMAL
    );

    int i = 0;

    public CrystalAuraHack(String name, String description) {
        super(name, description);
        this.setCategory(COMBAT);
        this.addSetting(
                rSetting
        );
        this.addSetting(
                cpsSetting
        );
    }

    @Override
    public void onEnable(){
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    public void onDisable(){
        EVENTS.remove(UpdateListener.class, this);
    }

    float cpsValue = this.cpsSetting.getValueF();
    float rangeValue = this.rSetting.getValueF();

    @Override
    public void onUpdate() {
        i++;
        int clickDelay = Math.round(20/cpsValue);

        for(Entity entity: MC.world.getEntities()){
            if(entity instanceof EndCrystalEntity){
                if(MC.player.distanceTo(entity) <= rangeValue){
                    if(i > clickDelay || i == 0){
                        MC.player.networkHandler.sendPacket(
                                new PlayerInteractEntityC2SPacket(
                                        entity,
                                        MC.player.isSneaking()
                                )
                        );
                        MC.player.attack(entity);
                        MC.player.swingHand(MAIN_HAND);
                        i = 0;
                    }
                }
            }
        }
    }
}
