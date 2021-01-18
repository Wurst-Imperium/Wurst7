/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Prevents the TooManyHax hack from blocking this feature.
 * <p>
 * Use if blocking this feature...
 * <ul>
 * <li>wouldn't actually do anything (e.g. ServerFinder button wouldn't be
 * removed by blocking its feature)
 * <li>would break other features in potentially unexpected ways (e.g. blocking
 * Panic would break Disable Wurst, blocking .setslider would break keybinds,
 * etc.)
 * <li>would potentially brick the whole client (e.g. ClickGUI)
 * <li>would get the feature stuck in its current state rather than turning it
 * off (e.g. HackList, WurstLogo)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DontBlock
{
	
}
