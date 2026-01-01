/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;

/**
 * A {@link FormattedCharSink} to completely bypass Mojang's visitor
 * system and just get the damn {@link String} out of a
 * {@link GuiMessage.Line}.
 *
 * <p>
 * Is this seriously the replacement for <code>getString()</code>?
 * What were they thinking?!
 */
public class JustGiveMeTheStringVisitor implements FormattedCharSink
{
	private final StringBuilder sb = new StringBuilder();
	
	@Override
	public boolean accept(int index, Style style, int codePoint)
	{
		sb.appendCodePoint(codePoint);
		return true;
	}
	
	@Override
	public String toString()
	{
		return sb.toString();
	}
}
