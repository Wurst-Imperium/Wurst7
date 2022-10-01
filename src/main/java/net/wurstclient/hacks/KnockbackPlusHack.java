/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"KnockbackPlus", "Knockback plus", "Super knockback", "Knockback+"})
public final class KnockbackPlusHack extends Hack implements LeftClickListener {

    public KnockbackPlusHack() {
        super("KnockbackPlus");
        setCategory(Category.COMBAT);
    }

    @Override
    public String getRenderName() {
        return getName();
    }

    @Override
    public void onEnable() {
        EVENTS.add(LeftClickListener.class, this);
    }

    @Override
    public void onDisable() {
        EVENTS.remove(LeftClickListener.class, this);
    }

    @Override
    public void onLeftClick(LeftClickEvent event) {
        if (MC.crosshairTarget == null
                || MC.crosshairTarget.getType() != HitResult.Type.ENTITY
                || !(((EntityHitResult) MC.crosshairTarget)
                .getEntity() instanceof LivingEntity))
            return;

        DoKnockBackPlus();
    }

    public void DoKnockBackPlus() {

        ClientPlayerEntity player = MC.player;
        ClientPlayNetworkHandler netHandler = player.networkHandler;
        netHandler.sendPacket(
                new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING));

        double xchan = player.getX();
        double ychan = player.getY();
        double zchan = player.getZ();
                netHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(xchan,
                        ychan + 1e-10, zchan, false));

    }
}
