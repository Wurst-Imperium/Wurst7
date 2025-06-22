/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.toast.ToastManager;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.Hack;

@SearchTags({"no toasts", "no advancements", "no recipes"})
public final class NoToastsHack extends Hack
{
	
	public NoToastsHack()
	{
		super("NoToasts");
		setCategory(Category.OTHER);
	}
	
	@Override
	protected void onEnable()
	{
		ToastManager toastManager = WurstClient.MC.getToastManager();
		
		if(toastManager != null)
			toastManager.clear();
	}
	// See ToastManagerMixin.onAdd()
}
