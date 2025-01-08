/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.text;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import net.minecraft.text.Text;

/**
 * Allows you to build more complex text out of multiple Wurst translations and
 * have everything update automatically when the user's language changes.
 *
 * <p>
 * This is necessary because Minecraft's {@link Text} does not have access to
 * Wurst's translations for security reasons.
 */
public final class WText
{
	private final ArrayList<WTextContent> contents;
	
	private WText(WTextContent... contents)
	{
		this.contents = Lists.newArrayList(contents);
	}
	
	public static WText literal(String text)
	{
		return new WText(new WLiteralTextContent(text));
	}
	
	public static WText translated(String key, Object... args)
	{
		return new WText(new WTranslatedTextContent(key, args));
	}
	
	public static WText empty()
	{
		return new WText(WLiteralTextContent.EMPTY);
	}
	
	public WText append(WText text)
	{
		contents.addAll(text.contents);
		return this;
	}
	
	public WText append(String text)
	{
		return append(literal(text));
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		for(WTextContent content : contents)
			builder.append(content);
		return builder.toString();
	}
}
