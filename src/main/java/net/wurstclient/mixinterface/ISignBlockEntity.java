/*
 * Copyright (C) 2014 - 2020 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import net.minecraft.text.Text;

public interface ISignBlockEntity
{
	public Text getTextOnRow(int row);
	
	public default Text[] getTextOnAllRows()
	{
		return new Text[]{getTextOnRow(0), getTextOnRow(1), getTextOnRow(2),
			getTextOnRow(3)};
	}
}
