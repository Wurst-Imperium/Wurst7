/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.options.ServerList;
import net.wurstclient.mixinterface.IServerList;

@Mixin(ServerList.class)
public class ServerListMixin implements IServerList
{
	@Shadow
	private List<ServerInfo> servers;
	
	@Override
	public void clear()
	{
		servers.clear();
	}
}
