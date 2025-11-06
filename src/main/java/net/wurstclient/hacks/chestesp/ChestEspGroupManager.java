/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chestesp;

import java.util.List;
import java.util.stream.Stream;

import net.wurstclient.hacks.chestesp.groups.*;

public final class ChestEspGroupManager
{
	public final NormalChestsGroup normalChests = new NormalChestsGroup();
	public final TrapChestsGroup trapChests = new TrapChestsGroup();
	public final EnderChestsGroup enderChests = new EnderChestsGroup();
	public final ChestCartsGroup chestCarts = new ChestCartsGroup();
	public final ChestBoatsGroup chestBoats = new ChestBoatsGroup();
	public final BarrelsGroup barrels = new BarrelsGroup();
	public final PotsGroup pots = new PotsGroup();
	public final ShulkerBoxesGroup shulkerBoxes = new ShulkerBoxesGroup();
	public final HoppersGroup hoppers = new HoppersGroup();
	public final HopperCartsGroup hopperCarts = new HopperCartsGroup();
	public final DroppersGroup droppers = new DroppersGroup();
	public final DispensersGroup dispensers = new DispensersGroup();
	public final CraftersGroup crafters = new CraftersGroup();
	public final FurnacesGroup furnaces = new FurnacesGroup();
	
	public final List<ChestEspBlockGroup> blockGroups =
		List.of(normalChests, trapChests, enderChests, barrels, pots,
			shulkerBoxes, hoppers, droppers, dispensers, crafters, furnaces);
	
	public final List<ChestEspEntityGroup> entityGroups =
		List.of(chestCarts, chestBoats, hopperCarts);
	
	public final List<ChestEspGroup> allGroups =
		Stream.concat(blockGroups.stream(), entityGroups.stream()).toList();
}
