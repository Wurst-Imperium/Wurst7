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
        return this.MC.options.keyForward;
    }

    public KeyBinding getLeftKey() {
        return this.MC.options.keyLeft;
    }

    public KeyBinding getBackKey() {
        return this.MC.options.keyBack;
    }

    public KeyBinding getRightKey() {
        return this.MC.options.keyRight;
    }

    public KeyBinding getJumpKey() {
        return this.MC.options.keyJump;
    }

    public KeyBinding getSneakKey() {
        return this.MC.options.keySneak;
    }

    public KeyBinding getSprintKey() {
        return this.MC.options.keySprint;
    }

    public KeyBinding getInventoryKey() {
        return this.MC.options.keyInventory;
    }

    public KeyBinding getSwapHandsKey() {
        return this.MC.options.keySwapHands;
    }

    public KeyBinding getDropKey() {
        return this.MC.options.keyDrop;
    }

    public KeyBinding getUseKey() {
        return this.MC.options.keyUse;
    }

    public KeyBinding getAttackKey() {
        return this.MC.options.keyAttack;
    }

    public KeyBinding getPickItemKey() {
        return this.MC.options.keyPickItem;
    }

    public KeyBinding getChatKey() {
        return this.MC.options.keyChat;
    }

    public KeyBinding getPlayerListKey() {
        return this.MC.options.keyPlayerList;
    }

    public KeyBinding getCommandKey() {
        return this.MC.options.keyCommand;
    }

    public KeyBinding getSocialInteractionsKey() {
        return this.MC.options.keySocialInteractions;
    }

    public KeyBinding getScreenshotKey() {
        return this.MC.options.keyScreenshot;
    }

    public KeyBinding getTogglePerspectiveKey() {
        return this.MC.options.keyTogglePerspective;
    }

    public KeyBinding getSmoothCameraKey() {
        return this.MC.options.keySmoothCamera;
    }

    public KeyBinding getFullscreenKey() {
        return this.MC.options.keyFullscreen;
    }

    public KeyBinding getSpectatorOutlinesKey() {
        return this.MC.options.keySpectatorOutlines;
    }

    public KeyBinding getAdvancementsKey() {
        return this.MC.options.keyAdvancements;
    }

    public KeyBinding[] getHotbarKeys() {
        return this.MC.options.keysHotbar;
    }

    public KeyBinding getSaveToolbarActivatorKey() {
        return this.MC.options.keySaveToolbarActivator;
    }

    public KeyBinding getLoadToolbarActivatorKey() {
        return this.MC.options.keyLoadToolbarActivator;
    }

    public KeyBinding[] getAllKeys() {
        return this.MC.options.keysAll;
    }

    // SETTERS

    public void setGamma(double value) {
        this.MC.options.gamma = value;
    }

    public void setMouseSensitivity(double value) {
        this.MC.options.mouseSensitivity = value;
    }


}
