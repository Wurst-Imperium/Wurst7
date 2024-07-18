/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.util.UUID;

public final class MinecraftProfile
{
	private final UUID uuid;
	private final String name;
	private final String mcAccessToken;
	
	public MinecraftProfile(UUID uuid, String name, String mcAccessToken)
	{
		this.uuid = uuid;
		this.name = name;
		this.mcAccessToken = mcAccessToken;
	}
	
	public UUID getUUID()
	{
		return uuid;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getAccessToken()
	{
		return mcAccessToken;
	}
}
