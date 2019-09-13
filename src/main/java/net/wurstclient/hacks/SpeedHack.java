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
import net.wurstclient.settings.SliderSetting;

public final class SpeedHack extends Hack implements UpdateListener {

    public final SliderSetting jumpHeight =
            new SliderSetting("Jump Height", 0.42, 0, 1, 0.01, SliderSetting.ValueDisplay.DECIMAL);
    public final SliderSetting jumpMotionAdd =
            new SliderSetting("Jump Speed Add", 0.2, 0, 1, 0.01, SliderSetting.ValueDisplay.DECIMAL);
    public final SliderSetting jumpMotionTimes =
            new SliderSetting("Jump Speed times", 1, 0.5, 2, 0.01, SliderSetting.ValueDisplay.DECIMAL);
    public SpeedHack()
    {
        super("Speed", "Makes you faster than legit.");
        setCategory(Category.MOVEMENT);
        addSetting(jumpHeight);
        addSetting(jumpMotionAdd);
        addSetting(jumpMotionTimes);
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

    @Override
    public void onUpdate()  {
        if(!MC.options.keyForward.isPressed())
            return;
        ClientPlayerEntity p = MC.player;
        p.setSprinting(true);
        double playerYaw = ((p.getHeadYaw()+90) * Math.PI / 180);
        if(p.onGround){
            p.setVelocity(p.getVelocity().x+(Math.cos(playerYaw)*jumpMotionAdd.getValue()), jumpHeight.getValue(), p.getVelocity().z+(Math.sin(playerYaw)*jumpMotionAdd.getValue()));
            p.setVelocity(p.getVelocity().x*jumpMotionTimes.getValue(), p.getVelocity().y, p.getVelocity().z*jumpMotionTimes.getValue());
        }
    }
}
