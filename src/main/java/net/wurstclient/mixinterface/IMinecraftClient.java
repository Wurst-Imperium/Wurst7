/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import net.minecraft.client.util.Session;

public interface IMinecraftClient
{
	public void rightClick();
	
	public void setItemUseCooldown(int itemUseCooldown);
	
	public IClientPlayerInteractionManager getInteractionManager();
	
	public int getItemUseCooldown();
	
	public IClientPlayerEntity getPlayer();
	
	public void setSession(Session session);
}
