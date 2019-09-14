/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"Velocity", "velocity", "no knockback", "antikb", "antiknockback"})
public final class VelocityHack extends Hack implements UpdateListener{
    public final EnumSetting type =
            new EnumSetting("Mode", Test.values(), Test.NORMAL);
    public final SliderSetting veloH =
            new SliderSetting("Horizontal", 0, -2, 2, 0.05, SliderSetting.ValueDisplay.DECIMAL);
    public final SliderSetting veloV =
            new SliderSetting("Vertical", 0, 0, 2, 0.05, SliderSetting.ValueDisplay.DECIMAL);
    public final SliderSetting ticks =
            new SliderSetting("Ticks", 2, 0, 10, 1, SliderSetting.ValueDisplay.INTEGER);
    public final SliderSetting ticksE =
            new SliderSetting("Ticks equals", 0, 0, 10, 1, SliderSetting.ValueDisplay.INTEGER);

    private enum Test{
        NORMAL("Normal", 0),
        REVERSE("Reverse", 1),
        TPBACK("Teleport back", 2),
        SNEAK("Sneak", 2);

        private final String name;
        private int type = 0;

        Test(String name, int type)
        {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    public VelocityHack(){
        super("Velocity",
                "Changes how to game handles velocity.");
        setCategory(Category.MOVEMENT);
        addSetting(type);
        addSetting(veloH);
        addSetting(veloV);
        addSetting(ticks);
        addSetting(ticksE);
    }

    @Override
    public void onEnable(){
        WURST.getEventManager().add(UpdateListener.class, this);
    }

    @Override
    public void onDisable(){
        WURST.getEventManager().remove(UpdateListener.class, this);
    }

    double x;
    double z;

    @Override
    public void onUpdate(){
        if(MC.player.hurtTime == 0)
            return;
        if(type.getSelected().toString().equalsIgnoreCase("NORMAL")){
            if(MC.player.hurtTime == 9){
                MC.player.setVelocity(MC.player.getVelocity().x * veloH.getValue(), MC.player.getVelocity().y
                        * veloV.getValue(), MC.player.getVelocity().z * veloH.getValue());
            }
        }
        if(type.getSelected().toString().equalsIgnoreCase("REVERSE")){
            if(MC.player.hurtTime > 0)
                MC.player.onGround = true;
        }

        if(type.getSelected().toString().equalsIgnoreCase("Teleport Back")){
            if(MC.player.hurtTime == 10){
                x = MC.player.getPos().x;
                z = MC.player.getPos().z;
            }
            if(MC.player.hurtTime % ticks.getValue() == ticksE.getValue()){
                MC.player.setPosition(x, MC.player.getPos().y ,z);
            }
        }
        if(type.getSelected().toString().equalsIgnoreCase("SNEAK")){
            if(MC.player.hurtTime % ticks.getValue() == ticksE.getValue()){
                MC.player.setVelocity(-MC.player.getVelocity().x, MC.player.getVelocity().y, -MC.player.getVelocity().z);
            }
        }
    }
}
