package net.wurstclient.data;

import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.NotNull;

public final class MerchantPacket {
    public static ClientboundMerchantOffersPacket packet;
    public static int lastEntityId = 0;

    public static void reset() {
        packet = null;
        lastEntityId = 0;
    }
}

