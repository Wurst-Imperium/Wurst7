/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.hacks.autofarm.plants.*;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.ToggleAllPlantTypesSetting;
import net.wurstclient.util.BlockUtils;

public final class AutoFarmPlantTypeManager
{
	public final AmethystPlantType amethystType = new AmethystPlantType();
	public final BambooPlantType bambooType = new BambooPlantType();
	public final BeetrootsPlantType beetrootsType = new BeetrootsPlantType();
	public final CactusPlantType cactusType = new CactusPlantType();
	public final CarrotsPlantType carrotsType = new CarrotsPlantType();
	public final ChorusPlantPlantType chorusFruitType =
		new ChorusPlantPlantType();
	public final CocoaBeanPlantType cocoaBeanType = new CocoaBeanPlantType();
	public final GlowBerryPlantType glowBerryType = new GlowBerryPlantType();
	public final KelpPlantType kelpType = new KelpPlantType();
	public final MelonPlantType melonType = new MelonPlantType();
	public final NetherWartPlantType netherWartType = new NetherWartPlantType();
	public final PitcherPlantPlantType pitcherPlantType =
		new PitcherPlantPlantType();
	public final PotatoesPlantType potatoesType = new PotatoesPlantType();
	public final PumpkinPlantType pumpkinType = new PumpkinPlantType();
	public final SugarCanePlantType sugarCaneType = new SugarCanePlantType();
	public final SweetBerryPlantType sweetBerryPlantType =
		new SweetBerryPlantType();
	public final TorchflowerPlantType torchflowerType =
		new TorchflowerPlantType();
	public final TwistingVinesPlantType twistingVinesType =
		new TwistingVinesPlantType();
	public final WeepingVinesPlantType weepingVinesType =
		new WeepingVinesPlantType();
	public final WheatPlantType wheatType = new WheatPlantType();
	
	public final List<AutoFarmPlantType> plantTypes =
		List.of(amethystType, bambooType, beetrootsType, cactusType,
			carrotsType, chorusFruitType, cocoaBeanType, glowBerryType,
			kelpType, melonType, netherWartType, pitcherPlantType, potatoesType,
			pumpkinType, sugarCaneType, sweetBerryPlantType, torchflowerType,
			twistingVinesType, weepingVinesType, wheatType);
	
	public final ToggleAllPlantTypesSetting toggleAllSetting =
		new ToggleAllPlantTypesSetting("All plant types",
			plantTypes.stream().flatMap(AutoFarmPlantType::getSettings));
	
	public AutoFarmPlantType getReplantingSpotType(BlockPos pos)
	{
		BlockState state = BlockUtils.getState(pos);
		return plantTypes.stream()
			.filter(type -> type.isReplantingSpot(pos, state)).findFirst()
			.orElse(null);
	}
	
	public boolean shouldHarvestByMining(BlockPos pos)
	{
		BlockState state = BlockUtils.getState(pos);
		return plantTypes.stream()
			.filter(AutoFarmPlantType::isHarvestingEnabled)
			.anyMatch(type -> type.shouldHarvestByMining(pos, state));
	}
	
	public boolean shouldHarvestByInteracting(BlockPos pos)
	{
		BlockState state = BlockUtils.getState(pos);
		return plantTypes.stream()
			.filter(AutoFarmPlantType::isHarvestingEnabled)
			.anyMatch(type -> type.shouldHarvestByInteracting(pos, state));
	}
	
	public Stream<Setting> getSettings()
	{
		return Stream.concat(Stream.of(toggleAllSetting),
			plantTypes.stream().flatMap(AutoFarmPlantType::getSettings));
	}
}
