package net.wurstclient.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class MCGameOptions {

    private final MinecraftClient MC;

    public MCGameOptions(MinecraftClient MC) {
        this.MC = MC;
    }

    // GETTERS

    public double getGamma() {
        return this.MC.options.gamma;
    }

    public boolean getIsFullscreen() {
        return this.MC.options.fullscreen;
    }

    public double getMouseSensitivity() {
        return this.MC.options.mouseSensitivity;
    }

    public KeyBinding getForwardKey() {
        return this.MC.options.forwardKey;
    }

    public KeyBinding getLeftKey() {
        return this.MC.options.leftKey;
    }

    public KeyBinding getBackKey() {
        return this.MC.options.backKey;
    }

    public KeyBinding getRightKey() {
        return this.MC.options.rightKey;
    }

    public KeyBinding getJumpKey() {
        return this.MC.options.jumpKey;
    }

    public KeyBinding getSneakKey() {
        return this.MC.options.sneakKey;
    }

    public KeyBinding getSprintKey() {
        return this.MC.options.sprintKey;
    }

    public KeyBinding getInventoryKey() {
        return this.MC.options.inventoryKey;
    }

    public KeyBinding getSwapHandsKey() {
        return this.MC.options.swapHandsKey;
    }

    public KeyBinding getDropKey() {
        return this.MC.options.dropKey;
    }

    public KeyBinding getUseKey() {
        return this.MC.options.useKey;
    }

    public KeyBinding getAttackKey() {
        return this.MC.options.attackKey;
    }

    public KeyBinding getPickItemKey() {
        return this.MC.options.pickItemKey;
    }

    public KeyBinding getChatKey() {
        return this.MC.options.chatKey;
    }

    public KeyBinding getPlayerListKey() {
        return this.MC.options.playerListKey;
    }

    public KeyBinding getCommandKey() {
        return this.MC.options.commandKey;
    }

    public KeyBinding getSocialInteractionsKey() {
        return this.MC.options.socialInteractionsKey;
    }

    public KeyBinding getScreenshotKey() {
        return this.MC.options.screenshotKey;
    }

    public KeyBinding getTogglePerspectiveKey() {
        return this.MC.options.togglePerspectiveKey;
    }

    public KeyBinding getSmoothCameraKey() {
        return this.MC.options.smoothCameraKey;
    }

    public KeyBinding getFullscreenKey() {
        return this.MC.options.fullscreenKey;
    }

    public KeyBinding getSpectatorOutlinesKey() {
        return this.MC.options.spectatorOutlinesKey;
    }

    public KeyBinding getAdvancementsKey() {
        return this.MC.options.advancementsKey;
    }

    public KeyBinding[] getHotbarKeys() {
        return this.MC.options.hotbarKeys;
    }

    public KeyBinding getSaveToolbarActivatorKey() {
        return this.MC.options.saveToolbarActivatorKey;
    }

    public KeyBinding getLoadToolbarActivatorKey() {
        return this.MC.options.loadToolbarActivatorKey;
    }

    public KeyBinding[] getAllKeys() {
        return this.MC.options.allKeys;
    }

    // SETTERS

    public void setGamma(double value) {
        this.MC.options.gamma = value;
    }


    public void setMouseSensitivity(double value) {
        this.MC.options.mouseSensitivity = value;
    }

}
