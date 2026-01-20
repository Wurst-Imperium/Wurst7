package net.wurstclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.wurstclient.WurstClient;
import net.wurstclient.data.MerchantPacket;
import net.wurstclient.hacks.AutoLibrarianHack;

import java.util.Objects;

/**
 * 交易 HUD 事件处理类
 */
public class TradingUtils {
    private static boolean isWindowOpen = false;

    public static boolean isEnableGuiLessTradeDetection() {
        AutoLibrarianHack autoLibrarianHack = WurstClient.INSTANCE.getHax().autoLibrarianHack;
        return autoLibrarianHack.isEnabled() && autoLibrarianHack.enableGuiLessTradeDetection.isChecked();
    }

    /**
     * 客户端世界后处理事件
     */
    public static void clientLevelPost() {
        if (!isEnableGuiLessTradeDetection()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        // 如果有界面打开则不处理
        if (minecraft.screen != null || player == null) {
            return;
        }

        Entity tradableEntity = TradingUtils.getCrosshairTradableEntity(minecraft, isWindowOpen);
        handleTradableEntity(tradableEntity, player);
    }

    /**
     * 处理可交易实体
     * @param entity 实体
     * @param player 玩家
     */
    private static void handleTradableEntity(Entity entity, LocalPlayer player) {
        if (entity != null) {
            // 如果是同一个实体，不重复处理
            if (MerchantPacket.lastEntityId == entity.getId()) {
                return;
            }

            // 重置商人信息并设置新的实体 ID
            MerchantPacket.reset();
            MerchantPacket.lastEntityId = entity.getId();

            // 发送交互包
            sendInteractionPacket(entity, player);
        } else {
            // 清除最后的实体 ID
            MerchantPacket.lastEntityId = 0;
        }
    }

    /**
     * 发送交互数据包
     * @param entity 实体
     * @param player 玩家
     */
    private static void sendInteractionPacket(Entity entity, LocalPlayer player) {
        ServerboundInteractPacket packet = ServerboundInteractPacket.createInteractionPacket(
                entity, player.isShiftKeyDown(), InteractionHand.MAIN_HAND);
        player.connection.send(packet);
    }

    public static boolean isWindowOpen() {
        return isWindowOpen;
    }

    public static void setWindowOpen(boolean windowOpen) {
        isWindowOpen = windowOpen;
    }

    /**
     * 检查实体是否为可交易的商人
     * @param entity 要检查的实体
     * @return 如果实体可以交易则返回 true
     */
    public static boolean isTradableMerchant(Entity entity) {
        if (Objects.isNull(entity) || !(entity instanceof Merchant)) {
            return false;
        }

        // 如果是村民，需要额外检查职业和玩家手持物品
        if (entity instanceof Villager villager) {
            return isValidVillagerForTrading(villager);
        }

        return true;
    }

    /**
     * 检查村民是否可以交易
     * @param villager 村民实体
     * @return 如果村民可以交易则返回 true
     */
    private static boolean isValidVillagerForTrading(Villager villager) {
        // 检查村民职业
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        if (profession.is(VillagerProfession.NONE) || profession.is(VillagerProfession.NITWIT)) {
            return false;
        }

        // 检查玩家手持物品
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack mainHandItem = player.getMainHandItem();
            return !mainHandItem.is(Items.VILLAGER_SPAWN_EGG) && !mainHandItem.is(Items.NAME_TAG);
        }

        return true;
    }

    /**
     * 获取当前准星指向的可交易实体
     * @param minecraft Minecraft 实例
     * @param isWindowOpen 交易窗口是否已打开
     * @return 可交易的实体，如果没有则返回 null
     */
    public static Entity getCrosshairTradableEntity(Minecraft minecraft, boolean isWindowOpen) {
        if (isWindowOpen) {
            return null;
        }

        Entity crosshairTarget = minecraft.crosshairPickEntity;
        return isTradableMerchant(crosshairTarget) ? crosshairTarget : null;
    }
}
