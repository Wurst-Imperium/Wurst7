/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"camera noclip", "camera no clip"})
public final class CameraNoClipHack extends Hack
{
	public CameraNoClipHack()
	{
		super("视角穿墙", "允许第三人称的视角穿墙");
		
		setCategory(Category.RENDER);
	}
}
