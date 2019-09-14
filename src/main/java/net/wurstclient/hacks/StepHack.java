/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;

public final class StepHack extends Hack implements UpdateListener{

    public final SliderSetting height =
            new SliderSetting("Height", 1, 0.5, 10, 0.5, SliderSetting.ValueDisplay.DECIMAL);
    public final SliderSetting jumpMotion =
            new SliderSetting("Jump Motion", 0.42, 0.38, 1, 0.01, SliderSetting.ValueDisplay.DECIMAL);
    public static EnumSetting mode =
            new EnumSetting("Mode", Mode.values(), Mode.VANILLA);
    private CheckboxSetting resetJump = new CheckboxSetting("Reset Y",
            "Sets the motion Y to 0 when no longer collided (for jump setting)",
            false);
    private enum Mode{
        VANILLA("Vanilla", 0),
        JUMP("Jump", 2);

        private final String name;
        private int type = 0;

        Mode(String name, int type)
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
    public StepHack()
    {
        super("Step", "Steps up more than 0.6 blocks.");
        setCategory(Category.MOVEMENT);
        addSetting(mode);
        addSetting(height);
        addSetting(jumpMotion);
        addSetting(resetJump);
    }

    @Override
    public void onEnable()
    {
        WURST.getEventManager().add(UpdateListener.class, this);
    }

    @Override
    public void onDisable()
    {
        WURST.getEventManager().remove(UpdateListener.class, this);
        MC.player.stepHeight = 0.5F;
    }

    boolean step = false;

    @Override
    public void onUpdate(){
        ClientPlayerEntity p = MC.player;
        if(mode.getSelected().toString().equalsIgnoreCase("vanilla")){
            p.stepHeight = height.getValueF();
        }
        if(mode.getSelected().toString().equalsIgnoreCase("jump")){
            if(p.horizontalCollision&&p.onGround){
                step = true;
                p.setVelocity(p.getVelocity().x, jumpMotion.getValueF(), p.getVelocity().z);
            }
            if(step&&!p.horizontalCollision&&resetJump.isChecked()){
                p.setVelocity(p.getVelocity().x, -1, p.getVelocity().z);
                step=false;
            }
        }
    }
}
