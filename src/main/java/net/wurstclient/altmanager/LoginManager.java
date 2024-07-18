/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.util.Optional;

import net.minecraft.client.session.Session;
import net.minecraft.util.Uuids;
import net.wurstclient.WurstClient;

public enum LoginManager
{
	;
	
	public static void changeCrackedName(String newName)
	{
		Session session =
			new Session(newName, Uuids.getOfflinePlayerUuid(newName), "",
				Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
		
		WurstClient.IMC.setSession(session);
	}
}
