/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

public enum MathUtils
{
	;
	
	public static boolean isInteger(String s)
	{
		try
		{
			Integer.parseInt(s);
			return true;
			
		}catch(NumberFormatException e)
		{
			return false;
		}
	}
	
	public static boolean isDouble(String s)
	{
		try
		{
			Double.parseDouble(s);
			return true;
			
		}catch(NumberFormatException e)
		{
			return false;
		}
	}
}
