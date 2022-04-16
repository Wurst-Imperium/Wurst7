/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class MCKeys
{
	private final MinecraftClient MC;

	public MCKeys(MinecraftClient mc) {
		this.MC = mc;
	}
	public KeyBinding forwardKey(){
		return this.MC.options.keyForward;
	}
	public KeyBinding leftKey(){
		return this.MC.options.keyLeft;
	}
	public KeyBinding backKey(){
		return this.MC.options.keyBack;
	}
	public KeyBinding rightKey(){
		return this.MC.options.keyRight;
	}
	public KeyBinding jumpKey(){
		return this.MC.options.keyJump;
	}
	public KeyBinding sneakKey(){
		return this.MC.options.keySneak;
	}
	public KeyBinding sprintKey(){
		return this.MC.options.keySprint;
	}
	public KeyBinding inventoryKey(){
		return this.MC.options.keyInventory;
	}
	public KeyBinding swapHandsKey(){
		return this.MC.options.keySwapHands;
	}
	public KeyBinding dropKey(){
		return this.MC.options.keyDrop;
	}
	public KeyBinding useKey(){
		return this.MC.options.keyUse;
	}
	public KeyBinding attackKey(){
		return this.MC.options.keyAttack;
	}
	public KeyBinding pickItemKey(){
		return this.MC.options.keyPickItem;
	}
	public KeyBinding chatKey(){
		return this.MC.options.keyChat;
	}
	public KeyBinding playerListKey(){
		return this.MC.options.keyPlayerList;
	}
	public KeyBinding commandKey(){
		return this.MC.options.keyCommand;
	}
	public KeyBinding socialInteractionsKey(){
		return this.MC.options.keySocialInteractions;
	}
	public KeyBinding screenshotKey(){
		return this.MC.options.keyScreenshot;
	}
	public KeyBinding togglePerspectiveKey(){
		return this.MC.options.keyTogglePerspective;
	}
	public KeyBinding smoothCameraKey(){
		return this.MC.options.keySmoothCamera;
	}
	public KeyBinding fullscreenKey(){
		return this.MC.options.keyFullscreen;
	}
	public KeyBinding spectatorOutlinesKey(){
		return this.MC.options.keySpectatorOutlines;
	}
	public KeyBinding advancementsKey(){
		return this.MC.options.keyAdvancements;
	}
	public KeyBinding[] hotbarKeys(){
		return this.MC.options.keysHotbar;
	}
	public KeyBinding saveToolbarActivatorKey(){
		return this.MC.options.keySaveToolbarActivator;
	}
	public KeyBinding loadToolbarActivatorKey(){
		return this.MC.options.keyLoadToolbarActivator;
	}
	public KeyBinding[] allKeys(){
		return this.MC.options.keysAll;
	}
}
