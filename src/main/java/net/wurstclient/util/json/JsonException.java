/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.json;

public final class JsonException extends Exception
{
	public JsonException()
	{
		super();
	}
	
	public JsonException(String message)
	{
		super(message);
	}
	
	public JsonException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
	public JsonException(Throwable cause)
	{
		super(cause);
	}
}
