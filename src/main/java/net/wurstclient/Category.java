/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

public enum Category
{
	BLOCKS("Blocks"),
	MOVEMENT("Movement"),
	COMBAT("Combat"),
	RENDER("Render"),
	CHAT("Chat"),
	FUN("Fun"),
	ITEMS("Items"),
	OTHER("Other");
	
	private final String name;
	
	private Category(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
}
