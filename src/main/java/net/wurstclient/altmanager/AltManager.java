/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.nio.file.Path;

public final class AltManager
{
	private final AltsFile altsFile;
	private AltList alts = new AltList();
	
	public AltManager(Path altsFile, Path encFolder)
	{
		this.altsFile = new AltsFile(altsFile, encFolder);
		this.altsFile.load(alts);
	}
	
	public void addAlt(String email, String password, boolean starred)
	{
		Alt alt = new Alt(email, password, null, starred);
		alts.add(alt);
		altsFile.save(alts);
	}
	
	public void editAlt(Alt alt, String newEmail, String newPassword)
	{
		Alt newAlt = new Alt(newEmail, newPassword, null, alt.isStarred());
		alts.remove(alt);
		alts.add(newAlt);
		altsFile.save(alts);
	}
}
