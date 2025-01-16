/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filterlists;

import java.util.ArrayList;
import java.util.List;

import net.wurstclient.settings.filters.*;

public final class ElytraLockFilterList extends EntityFilterList {
    private ElytraLockFilterList(List<EntityFilter> filters) {
        super(filters);
    }

    public static ElytraLockFilterList create() {
        ArrayList<EntityFilter> builder = new ArrayList<>();

        builder.add(new FilterPlayersSetting(
                "Won't target other players when locking in.", false));

        builder.add(new FilterHostileSetting("Won't target hostile mobs like"
                + " zombies and creepers when locking in.", true));

        builder.add(new FilterNeutralSetting("Won't target neutral mobs like"
                + " endermen and wolves when locking in.", AttackDetectingEntityFilter.Mode.ON));

        builder.add(new FilterPassiveSetting("Won't target animals like pigs"
                + " and cows, ambient mobs like bats, and water mobs like fish,"
                + " squid and dolphins when locking in.",
                true));

        builder.add(new FilterPassiveWaterSetting("Won't target passive water"
                + " mobs like fish, squid, dolphins and axolotls when auto-placing", true));

        builder.add(new FilterBatsSetting("Won't target bats and any other"
                + " \"ambient\" mobs when locking in.", true));

        builder.add(new FilterSlimesSetting("Won't target slimes when"
                + " locking in.", true));

        builder.add(new FilterVillagersSetting("Won't target villagers and"
                + " wandering traders when locking in.",
                true));

        builder.add(new FilterZombieVillagersSetting("Won't target zombified"
                + " villagers when locking in.", true));

        builder.add(new FilterGolemsSetting("Won't target iron golems and snow"
                + " golems when locking in.", true));

        builder.add(new FilterPiglinsSetting("Won't target piglins when"
                + " locking in.",
                AttackDetectingEntityFilter.Mode.ON));

        builder.add(new FilterZombiePiglinsSetting("Won't target"
                + " zombified piglins when locking in.",
                AttackDetectingEntityFilter.Mode.ON));

        builder.add(new FilterShulkersSetting("Won't target shulkers when"
                + " locking in.", true));

        builder.add(new FilterAllaysSetting(
                "Won't target allays when locking in.",
                true));

        builder.add(new FilterInvisibleSetting(
                "Won't target invisible entities when locking in.", false));

        builder.add(new FilterNamedSetting(
                "Won't target name-tagged entities when locking in.", false));

        builder.add(new FilterArmorStandsSetting(
                "Won't target armor stands when locking in.", true));

        return new ElytraLockFilterList(builder);
    }
}