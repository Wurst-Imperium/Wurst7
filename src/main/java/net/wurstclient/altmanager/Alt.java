/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import com.google.gson.JsonObject;

public abstract class Alt
{
	private boolean favorite;
	
	public Alt(boolean favorite)
	{
		this.favorite = favorite;
	}
	
	/**
	 * Logs the user in with this Alt and updates the Alt's name, but doesn't
	 * save the changes. <b>You should probably call
	 * {@link AltManager#login(Alt)} instead.</b>
	 *
	 * @throws LoginException
	 *             if the login attempt failed for any reason. The reason will
	 *             be explained in the Exception's message, which should be
	 *             displayed to the user.
	 */
	public abstract void login() throws LoginException;
	
	/**
	 * Adds this Alt to the given {@link JsonObject}. Used for saving and
	 * exporting the alt list.
	 */
	public abstract void exportAsJson(JsonObject json);
	
	/**
	 * @return The alt's login details in text format. Used for exporting the
	 *         alt list. Cannot be null or empty.
	 */
	public abstract String exportAsTXT();
	
	/**
	 * @return the Alt's name, or an empty String if unknown. Cannot be null.
	 */
	public abstract String getName();
	
	/**
	 * @return the Alt's name, or email if the name is unknown. Cannot be null
	 *         or empty.
	 */
	public abstract String getDisplayName();
	
	public final boolean isCracked()
	{
		return this instanceof CrackedAlt;
	}
	
	/**
	 * @return true if the Alt is premium (a real paid Minecraft account) and
	 *         checked (has logged in successfully at some point).
	 */
	public final boolean isCheckedPremium()
	{
		return !isCracked() && !getName().isEmpty();
	}
	
	/**
	 * @return true if the Alt is premium (a real paid Minecraft account) and
	 *         unchecked (has never logged in successfully).
	 */
	public final boolean isUncheckedPremium()
	{
		return !isCracked() && getName().isEmpty();
	}
	
	public final boolean isFavorite()
	{
		return favorite;
	}
	
	/**
	 * Changes whether or not the Alt is marked as a favorite, but doesn't save
	 * the changes. <b>You should probably call
	 * {@link AltManager#toggleFavorite(Alt)} instead.</b>
	 */
	public final void setFavorite(boolean favorite)
	{
		this.favorite = favorite;
	}
	
	/**
	 * @apiNote This method intentionally does not include the Alt's password,
	 *          to prevent accidental leaks if an Alt ever gets written directly
	 *          to the log files. Use {@link #exportAsTXT()} to get the full
	 *          login credentials.
	 */
	@Override
	public final String toString()
	{
		return getDisplayName();
	}
}
