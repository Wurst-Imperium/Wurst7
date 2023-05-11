/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;

@DontBlock
@SearchTags({
	"autogg", "good game", "hypixel"
})
public final class AutoGGOtf extends OtherFeature implements ChatInputListener, UpdateListener {
	// that long bar message sent when a game ends
	private final String END_LINE = "▬".repeat(64);
	private final String GAME_START_MSG = "The game starts in ";
	private final int DEBOUNCE_LENGTH = 40;
	private int debounce = 0;
	private boolean lastMessageWasEmpty = false;

	private final CheckboxSetting active = new CheckboxSetting("Active", false);

	// TODO custom gg message
	private final String ggMessage = "gg";

	public AutoGGOtf() {
		super("AutoGG", "Says GG after games end in Hypixel.");

		addSetting(active);

		EVENTS.add(ChatInputListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}

	@Override
	public boolean isEnabled() {
		return active.isChecked();
	}

	@Override
	public String getPrimaryAction() {
		return isEnabled() ? "Disable" : "Enable";
	}

	@Override
	public void doPrimaryAction() {
		active.setChecked(!active.isChecked());
	}

	@Override
	public void onReceivedMessage(ChatInputEvent event) {
		if (!isEnabled()) return;
		String message = event.getComponent().getString();
		if (message.startsWith(GAME_START_MSG)) {
			debounce = DEBOUNCE_LENGTH * 3;
			return;
		}
		
		boolean currentMessageEmpty = message.trim().equals("");
		
		// if the message isn't the game over line
		// or the debounce is not active, 
		if (lastMessageWasEmpty && message.contains(END_LINE) && !(debounce > 0)) {
			// send gg and reset debounce
			MC.getNetworkHandler().sendChatMessage("/ac " + ggMessage);
			debounce = DEBOUNCE_LENGTH;
		}

		lastMessageWasEmpty = currentMessageEmpty;
	}

	@Override
	public void onUpdate() {
		if (debounce > 0) {
			debounce--;
		}
	}
}
