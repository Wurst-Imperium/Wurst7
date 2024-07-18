/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

public final class XBoxLiveToken
{
	private final String token;
	private final String uhs;
	
	public XBoxLiveToken(String token, String uhs)
	{
		this.token = token;
		this.uhs = uhs;
	}
	
	public String getToken()
	{
		return token;
	}
	
	public String getUHS()
	{
		return uhs;
	}
}
