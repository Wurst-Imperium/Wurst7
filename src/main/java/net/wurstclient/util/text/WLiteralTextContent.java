/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.text;

import java.util.Objects;

public final class WLiteralTextContent implements WTextContent
{
	public static final WLiteralTextContent EMPTY = new WLiteralTextContent("");
	
	private final String text;
	
	public WLiteralTextContent(String text)
	{
		this.text = Objects.requireNonNull(text);
	}
	
	@Override
	public String toString()
	{
		return text;
	}
}
