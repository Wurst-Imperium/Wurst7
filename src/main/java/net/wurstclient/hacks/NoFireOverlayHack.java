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

@SearchTags({"no fire overlay"})
public final class NoFireOverlayHack extends Hack
{
	public NoFireOverlayHack()
	{
		super("防火挡脸", "当你身上着火时,\n不显示屏幕上的火焰贴图");
		
		setCategory(Category.RENDER);
	}
}
