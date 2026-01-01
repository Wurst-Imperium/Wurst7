/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import net.minecraft.client.OptionInstance;

public interface ISimpleOption<T>
{
	/**
	 * Forces the value of the option to the specified value, even if it's
	 * outside of the normal range.
	 */
	public void forceSetValue(T newValue);
	
	/**
	 * Returns the given SimpleOption object as an ISimpleOption, allowing you
	 * to access the forceSetValue() method.
	 */
	@SuppressWarnings("unchecked")
	public static <T> ISimpleOption<T> get(OptionInstance<T> option)
	{
		return (ISimpleOption<T>)(Object)option;
	}
}
