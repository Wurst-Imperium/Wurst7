/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.net.Proxy;
import java.util.Optional;

import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

import net.minecraft.client.util.Session;
import net.wurstclient.WurstClient;

public final class LoginManager
{
	public static void login(String email, String password)
		throws LoginException
	{
		YggdrasilUserAuthentication auth =
			(YggdrasilUserAuthentication)new YggdrasilAuthenticationService(
				Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT);
		
		auth.setUsername(email);
		auth.setPassword(password);
		
		try
		{
			auth.logIn();
			WurstClient.IMC
				.setSession(new Session(auth.getSelectedProfile().getName(),
					auth.getSelectedProfile().getId().toString(),
					auth.getAuthenticatedToken(), Optional.empty(),
					Optional.empty(), Session.AccountType.MOJANG));
			
		}catch(AuthenticationUnavailableException e)
		{
			throw new LoginException(
				"\u00a74\u00a7lCannot contact authentication server!", e);
			
		}catch(AuthenticationException e)
		{
			e.printStackTrace();
			
			if(e.getMessage().contains("Invalid username or password."))
				throw new LoginException(
					"\u00a74\u00a7lWrong password! (or shadowbanned)", e);
			
			if(e.getMessage().toLowerCase().contains("account migrated"))
				throw new LoginException(
					"\u00a74\u00a7lAccount migrated to Mojang account.", e);
			
			if(e.getMessage().toLowerCase().contains("migrated"))
				throw new LoginException(
					"\u00a74\u00a7lAccount migrated to Microsoft account.", e);
			
			throw new LoginException(
				"\u00a74\u00a7lCannot contact authentication server!", e);
			
		}catch(NullPointerException e)
		{
			e.printStackTrace();
			
			throw new LoginException(
				"\u00a74\u00a7lWrong password! (or shadowbanned)", e);
		}
	}
	
	public static void changeCrackedName(String newName)
	{
		WurstClient.IMC.setSession(new Session(newName, "", "",
			Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));
	}
}
