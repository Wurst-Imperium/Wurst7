/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.net.Proxy;

import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

import com.thealtening.auth.service.AlteningServiceType;
import net.minecraft.client.util.Session;
import net.wurstclient.WurstClient;

public final class LoginManager
{
	public static String login(String email, String password)
	{
		YggdrasilUserAuthentication auth =
			(YggdrasilUserAuthentication)new YggdrasilAuthenticationService(
				Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT);

		/*
		  Automatically changing the status from mojang/thealtening, depending on the account format
		  It won't update the service, if it's the same though.
		 */
		if(email.contains("@alt.com") && email.contains("-")) {
			WurstClient.INSTANCE.getTheAlteningAuthentication().updateService(AlteningServiceType.THEALTENING);
			password = "anything";
		} else {
			WurstClient.INSTANCE.getTheAlteningAuthentication().updateService(AlteningServiceType.MOJANG);
		}

		auth.setUsername(email);
		auth.setPassword(password);
		
		try
		{
			auth.logIn();
			WurstClient.IMC
				.setSession(new Session(auth.getSelectedProfile().getName(),
					auth.getSelectedProfile().getId().toString(),
					auth.getAuthenticatedToken(), "mojang"));
			return "";
			
		}catch(AuthenticationUnavailableException e)
		{
			return "\u00a74\u00a7lCannot contact authentication server!";
			
		}catch(AuthenticationException e)
		{
			e.printStackTrace();
			
			if(e.getMessage().contains("Invalid username or password.")
				|| e.getMessage().toLowerCase().contains("account migrated"))
				return "\u00a74\u00a7lWrong password! (or shadowbanned)";
			else
				return "\u00a74\u00a7lCannot contact authentication server!";
			
		}catch(NullPointerException e)
		{
			e.printStackTrace();
			return "\u00a74\u00a7lWrong password! (or shadowbanned)";
		}
	}
	
	public static void changeCrackedName(String newName)
	{
		WurstClient.IMC.setSession(new Session(newName, "", "", "mojang"));
	}
}
