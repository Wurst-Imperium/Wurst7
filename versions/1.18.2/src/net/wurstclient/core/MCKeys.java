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

	public MCKeys(MinecraftClient MC) {
		this.MC = MC;
	}

	public KeyBinding forwardKey(){
		return this.MC.options.forwardKey;
	}
	public KeyBinding leftKey(){
		return this.MC.options.leftKey;
	}
	public KeyBinding backKey(){
		return this.MC.options.backKey;
	}
	public KeyBinding rightKey(){
		return this.MC.options.rightKey;
	}
	public KeyBinding jumpKey(){
		return this.MC.options.jumpKey;
	}
	public KeyBinding sneakKey(){
		return this.MC.options.sneakKey;
	}
	public KeyBinding sprintKey(){
		return this.MC.options.sprintKey;
	}
	public KeyBinding inventoryKey(){
		return this.MC.options.inventoryKey;
	}
	public KeyBinding swapHandsKey(){
		return this.MC.options.swapHandsKey;
	}
	public KeyBinding dropKey(){
		return this.MC.options.dropKey;
	}
	public KeyBinding useKey(){
		return this.MC.options.useKey;
	}
	public KeyBinding attackKey(){
		return this.MC.options.attackKey;
	}
	public KeyBinding pickItemKey(){
		return this.MC.options.pickItemKey;
	}
	public KeyBinding chatKey(){
		return this.MC.options.chatKey;
	}
	public KeyBinding playerListKey(){
		return this.MC.options.playerListKey;
	}
	public KeyBinding commandKey(){
		return this.MC.options.commandKey;
	}
	public KeyBinding socialInteractionsKey(){
		return this.MC.options.socialInteractionsKey;
	}
	public KeyBinding screenshotKey(){
		return this.MC.options.screenshotKey;
	}
	public KeyBinding togglePerspectiveKey(){
		return this.MC.options.togglePerspectiveKey;
	}
	public KeyBinding smoothCameraKey(){
		return this.MC.options.smoothCameraKey;
	}
	public KeyBinding fullscreenKey(){
		return this.MC.options.fullscreenKey;
	}
	public KeyBinding spectatorOutlinesKey(){
		return this.MC.options.spectatorOutlinesKey;
	}
	public KeyBinding advancementsKey(){
		return this.MC.options.advancementsKey;
	}
	public KeyBinding[] hotbarKeys(){
		return this.MC.options.hotbarKeys;
	}
	public KeyBinding saveToolbarActivatorKey(){
		return this.MC.options.saveToolbarActivatorKey;
	}
	public KeyBinding loadToolbarActivatorKey(){
		return this.MC.options.loadToolbarActivatorKey;
	}
	public KeyBinding[] allKeys(){
		return this.MC.options.allKeys;
	}
}
