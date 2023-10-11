/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filterlists;

import java.util.ArrayList;
import java.util.List;

import net.wurstclient.settings.filters.*;

public final class MobEspFilterList extends EntityFilterList
{
	private MobEspFilterList(List<EntityFilter> filters)
	{
		super(filters);
	}
	
	public static MobEspFilterList create()
	{
		ArrayList<EntityFilter> builder = new ArrayList<>();
		builder.add(FilterMonstersSetting.genericVision(false));
		builder.add(FilterZombiePiglinsSetting
			.genericVision(AttackDetectingEntityFilter.Mode.OFF));
		builder.add(FilterEndermenSetting
			.genericVision(AttackDetectingEntityFilter.Mode.OFF));
		builder.add(FilterPassiveSetting.genericVision(false));
		builder.add(FilterBatsSetting.genericVision(false));
		builder.add(FilterPetsSetting.genericVision(false));
		builder.add(FilterTradersSetting.genericVision(false));
		builder.add(FilterGolemsSetting.genericVision(false));
		builder.add(FilterAllaysSetting.genericVision(false));
		builder.add(FilterInvisibleSetting.genericVision(false));
		builder.add(FilterArmorStandsSetting.genericVision(true));
		return new MobEspFilterList(builder);
	}
}
