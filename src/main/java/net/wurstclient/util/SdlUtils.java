/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import org.lwjgl.sdl.SDLMouse;

public enum SdlUtils
{
	;
	
	public static boolean isMouseButtonPressed(int button)
	{
		int pressedButtons = SDLMouse.SDL_GetMouseState(null, null);
		int buttonMask = 1 << (button - 1);
		return (pressedButtons & buttonMask) != 0;
	}
}
