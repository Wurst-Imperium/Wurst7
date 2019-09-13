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

    private enum Test{
        NORMAL("Normal", 0),
        REVERSE("Reverse", 1);

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
                "Changes velocity.");
        setCategory(Category.MOVEMENT);
        addSetting(type);
        addSetting(veloH);
        addSetting(veloV);
    }

    @Override
    public void onEnable(){
        WURST.getEventManager().add(UpdateListener.class, this);
    }

    @Override
    public void onDisable(){
        WURST.getEventManager().remove(UpdateListener.class, this);
    }

    @Override
    public void onUpdate(){
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
    }
}
