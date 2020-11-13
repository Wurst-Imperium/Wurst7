/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

public interface IMiningToolItem
{
	/**
	 * Returns the attack damage. Calling it getAttackDamage() causes a false
	 * positive from McAfee GW Edition.
	 */
	public float fuckMcAfee1();
	
	/**
	 * Returns the attack speed. Calling it getAttackSpeed() causes a false
	 * positive from McAfee GW Edition.
	 */
	public float fuckMcAfee2();
}
