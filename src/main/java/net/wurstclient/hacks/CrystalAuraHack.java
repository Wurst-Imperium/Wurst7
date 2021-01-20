
package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

import static net.minecraft.util.Hand.MAIN_HAND;
import static net.wurstclient.Category.COMBAT;

@SearchTags({"crystal aura", "AutoCrystal", "auto crystal"})
public final class CrystalAuraHack extends Hack implements UpdateListener {

    SliderSetting range = new SliderSetting(
            "Range",
            "Range where player would hit the end crystal. Note that high range might not work.",
            5,
            1,
            20,
            1,
            ValueDisplay.DECIMAL
    );

    SliderSetting speed = new SliderSetting(
            "Speed",
            "How many clicks per second. Note that setting higher values might get you kicked on some servers.",
            12,
            0.1,
            20,
            1,
            ValueDisplay.DECIMAL
    );

    int timer = 0;

    public CrystalAuraHack() {
        super("CrystalAura", 	"Automatically hits end crystals. Also called AutoCrystal in some clients");
        this.setCategory(COMBAT);
        this.addSetting(
                range
        );
        this.addSetting(
                speed
        );
    }

    @Override
    public void onEnable(){
        WURST.getHax().clickAuraHack.setEnabled(false);
        WURST.getHax().fightBotHack.setEnabled(false);
        WURST.getHax().killauraLegitHack.setEnabled(false);
        WURST.getHax().multiAuraHack.setEnabled(false);
        WURST.getHax().protectHack.setEnabled(false);
        WURST.getHax().triggerBotHack.setEnabled(false);
        WURST.getHax().tpAuraHack.setEnabled(false);

        EVENTS.add(UpdateListener.class, this);
        timer = 0;
    }

    @Override
    public void onDisable(){
        EVENTS.remove(UpdateListener.class, this);
    }

    @Override
    public void onUpdate() {
        float cpsValue = this.speed.getValueF();
        float rangeValue = this.range.getValueF();
        timer++;
        int clickDelay = Math.round(20/cpsValue);

        for(Entity entity: MC.world.getEntities()){
            if(entity instanceof EndCrystalEntity){
                if(MC.player.distanceTo(entity) <= rangeValue){
                    if(timer > clickDelay || timer == 0){
                        MC.player.networkHandler.sendPacket(
                                new PlayerInteractEntityC2SPacket(
                                        entity,
                                        MC.player.isSneaking()
                                )
                        );
                        MC.player.attack(entity);
                        MC.player.swingHand(MAIN_HAND);
                        timer = 0;
                    }
                }
            }
        }
    }
}
