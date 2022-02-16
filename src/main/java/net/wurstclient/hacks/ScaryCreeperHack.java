/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

import java.util.List;

@SearchTags({"AutoCreeper", "LeaveCreeper", "leave creeper", "creeper", "scary creeper"})
public class ScaryCreeperHack extends Hack implements UpdateListener
{
    private final CheckboxSetting doNotTriggerWhenDefending = new CheckboxSetting("Do not trigger when defending", "Hack won't trigger if you're using shield", true);
    private final CheckboxSetting disableAfterTriggering = new CheckboxSetting("Auto disable", "Disables hack once it gets triggered", false);

    public ScaryCreeperHack()
    {
        super("ScaryCreeper");
        setCategory(Category.OTHER);
        addSetting(doNotTriggerWhenDefending);
        addSetting(disableAfterTriggering);
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

    @Override
    public void onUpdate()
    {
        PlayerEntity player = MC.player;

        if (!player.isAlive())
            return;

        if (doNotTriggerWhenDefending.isChecked() && player.isUsingItem() && player.getActiveItem().getItem().equals(Items.SHIELD))
            return;

        List<CreeperEntity> creepers = player.getWorld().getEntitiesByClass(CreeperEntity.class, new Box(player.getPos().subtract(5,5,5), player.getPos().add(5,5,5)), EntityPredicates.VALID_ENTITY);

        for (CreeperEntity creeper : creepers)
        {
            if (creeper.getClientFuseTime(1.0f) > 0.5)
            {
                //leave
                MC.world.disconnect();

                //disable hack if needed
                if (disableAfterTriggering.isChecked())
                    setEnabled(false);
            }
        }
    }
}
