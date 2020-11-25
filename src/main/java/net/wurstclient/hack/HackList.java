/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hack;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hacks.*;
import net.wurstclient.util.json.JsonException;

public final class HackList implements UpdateListener
{
	public final AntiAfkHack antiAfkHack = new AntiAfkHack();
	public final AntiBlindHack antiBlindHack = new AntiBlindHack();
	public final AntiCactusHack antiCactusHack = new AntiCactusHack();
	public final AntiKnockbackHack antiKnockbackHack = new AntiKnockbackHack();
	public final AntiSpamHack antiSpamHack = new AntiSpamHack();
	public final AntiWaterPushHack antiWaterPushHack = new AntiWaterPushHack();
	public final AntiWobbleHack antiWobbleHack = new AntiWobbleHack();
	public final AutoArmorHack autoArmorHack = new AutoArmorHack();
	public final AutoBuildHack autoBuildHack = new AutoBuildHack();
	public final AutoDropHack autoDropHack = new AutoDropHack();
	public final AutoLeaveHack autoLeaveHack = new AutoLeaveHack();
	public final AutoEatHack autoEatHack = new AutoEatHack();
	public final AutoFarmHack autoFarmHack = new AutoFarmHack();
	public final AutoFishHack autoFishHack = new AutoFishHack();
	public final AutoMineHack autoMineHack = new AutoMineHack();
	public final AutoPotionHack autoPotionHack = new AutoPotionHack();
	public final AutoReconnectHack autoReconnectHack = new AutoReconnectHack();
	public final AutoRespawnHack autoRespawnHack = new AutoRespawnHack();
	public final AutoSignHack autoSignHack = new AutoSignHack();
	public final AutoSoupHack autoSoupHack = new AutoSoupHack();
	public final AutoSprintHack autoSprintHack = new AutoSprintHack();
	public final AutoStealHack autoStealHack = new AutoStealHack();
	public final AutoSwimHack autoSwimHack = new AutoSwimHack();
	public final AutoSwitchHack autoSwitchHack = new AutoSwitchHack();
	public final AutoSwordHack autoSwordHack = new AutoSwordHack();
	public final AutoToolHack autoToolHack = new AutoToolHack();
	public final AutoTotemHack autoTotemHack = new AutoTotemHack();
	public final AutoWalkHack autoWalkHack = new AutoWalkHack();
	public final BaseFinderHack baseFinderHack = new BaseFinderHack();
	public final BlinkHack blinkHack = new BlinkHack();
	public final BoatFlyHack boatFlyHack = new BoatFlyHack();
	public final BonemealAuraHack bonemealAuraHack = new BonemealAuraHack();
	public final BowAimbotHack bowAimbotHack = new BowAimbotHack();
	public final BuildRandomHack buildRandomHack = new BuildRandomHack();
	public final BunnyHopHack bunnyHopHack = new BunnyHopHack();
	public final CameraNoClipHack cameraNoClipHack = new CameraNoClipHack();
	public final CaveFinderHack caveFinderHack = new CaveFinderHack();
	public final ChatTranslatorHack chatTranslatorHack =
		new ChatTranslatorHack();
	public final ChestEspHack chestEspHack = new ChestEspHack();
	public final ClickAuraHack clickAuraHack = new ClickAuraHack();
	public final ClickGuiHack clickGuiHack = new ClickGuiHack();
	public final CrashChestHack crashChestHack = new CrashChestHack();
	public final CriticalsHack criticalsHack = new CriticalsHack();
	public final DerpHack derpHack = new DerpHack();
	public final DolphinHack dolphinHack = new DolphinHack();
	public final ExcavatorHack excavatorHack = new ExcavatorHack();
	public final ExtraElytraHack extraElytraHack = new ExtraElytraHack();
	public final FancyChatHack fancyChatHack = new FancyChatHack();
	public final FastBreakHack fastBreakHack = new FastBreakHack();
	public final FastLadderHack fastLadderHack = new FastLadderHack();
	public final FastPlaceHack fastPlaceHack = new FastPlaceHack();
	public final FeedAuraHack feedAuraHack = new FeedAuraHack();
	public final FightBotHack fightBotHack = new FightBotHack();
	public final FishHack fishHack = new FishHack();
	public final FlightHack flightHack = new FlightHack();
	public final FollowHack followHack = new FollowHack();
	public final ForceOpHack forceOpHack = new ForceOpHack();
	public final FreecamHack freecamHack = new FreecamHack();
	public final FullbrightHack fullbrightHack = new FullbrightHack();
	public final GlideHack glideHack = new GlideHack();
	public final HandNoClipHack handNoClipHack = new HandNoClipHack();
	public final HeadRollHack headRollHack = new HeadRollHack();
	public final HealthTagsHack healthTagsHack = new HealthTagsHack();
	public final HighJumpHack highJumpHack = new HighJumpHack();
	public final InfiniChatHack infiniChatHack = new InfiniChatHack();
	public final InstantBunkerHack instantBunkerHack = new InstantBunkerHack();
	public final ItemEspHack itemEspHack = new ItemEspHack();
	public final ItemGeneratorHack itemGeneratorHack = new ItemGeneratorHack();
	public final JesusHack jesusHack = new JesusHack();
	public final JetpackHack jetpackHack = new JetpackHack();
	public final KaboomHack kaboomHack = new KaboomHack();
	public final KillauraLegitHack killauraLegitHack = new KillauraLegitHack();
	public final KillauraHack killauraHack = new KillauraHack();
	public final KillPotionHack killPotionHack = new KillPotionHack();
	public final LiquidsHack liquidsHack = new LiquidsHack();
	public final LsdHack lsdHack = new LsdHack();
	public final MassTpaHack massTpaHack = new MassTpaHack();
	public final MileyCyrusHack mileyCyrusHack = new MileyCyrusHack();
	public final MobEspHack mobEspHack = new MobEspHack();
	public final MobSpawnEspHack mobSpawnEspHack = new MobSpawnEspHack();
	public final MultiAuraHack multiAuraHack = new MultiAuraHack();
	public final NameProtectHack nameProtectHack = new NameProtectHack();
	public final NameTagsHack nameTagsHack = new NameTagsHack();
	public final NavigatorHack navigatorHack = new NavigatorHack();
	public final NoClipHack noClipHack = new NoClipHack();
	public final NoFallHack noFallHack = new NoFallHack();
	public final NoFireOverlayHack noFireOverlayHack = new NoFireOverlayHack();
	public final NoHurtcamHack noHurtcamHack = new NoHurtcamHack();
	public final NoOverlayHack noOverlayHack = new NoOverlayHack();
	public final NoPumpkinHack noPumpkinHack = new NoPumpkinHack();
	public final NoSlowdownHack noSlowdownHack = new NoSlowdownHack();
	public final NoWeatherHack noWeatherHack = new NoWeatherHack();
	public final NoWebHack noWebHack = new NoWebHack();
	public final NukerHack nukerHack = new NukerHack();
	public final NukerLegitHack nukerLegitHack = new NukerLegitHack();
	public final OverlayHack overlayHack = new OverlayHack();
	public final PanicHack panicHack = new PanicHack();
	public final ParkourHack parkourHack = new ParkourHack();
	public final PlayerEspHack playerEspHack = new PlayerEspHack();
	public final PlayerFinderHack playerFinderHack = new PlayerFinderHack();
	public final PotionSaverHack potionSaverHack = new PotionSaverHack();
	public final ProphuntEspHack prophuntEspHack = new ProphuntEspHack();
	public final ProtectHack protectHack = new ProtectHack();
	public final RadarHack radarHack = new RadarHack();
	public final RainbowUiHack rainbowUiHack = new RainbowUiHack();
	public final ReachHack reachHack = new ReachHack();
	public final RemoteViewHack remoteViewHack = new RemoteViewHack();
	public final SafeWalkHack safeWalkHack = new SafeWalkHack();
	public final ScaffoldWalkHack scaffoldWalkHack = new ScaffoldWalkHack();
	public final SearchHack searchHack = new SearchHack();
	public final ServerCrasherHack serverCrasherHack = new ServerCrasherHack();
	public final SkinDerpHack skinDerpHack = new SkinDerpHack();
	public final SneakHack sneakHack = new SneakHack();
	public final SpeedHackHack speedHackHack = new SpeedHackHack();
	public final SpeedNukerHack speedNukerHack = new SpeedNukerHack();
	public final SpiderHack spiderHack = new SpiderHack();
	public final StepHack stepHack = new StepHack();
	public final ThrowHack throwHack = new ThrowHack();
	public final TillauraHack tillauraHack = new TillauraHack();
	public final TimerHack timerHack = new TimerHack();
	public final TiredHack tiredHack = new TiredHack();
	public final TooManyHaxHack tooManyHaxHack = new TooManyHaxHack();
	public final TpAuraHack tpAuraHack = new TpAuraHack();
	public final TrajectoriesHack trajectoriesHack = new TrajectoriesHack();
	public final TriggerBotHack triggerBotHack = new TriggerBotHack();
	public final TrollPotionHack trollPotionHack = new TrollPotionHack();
	public final TrueSightHack trueSightHack = new TrueSightHack();
	public final TunnellerHack tunnellerHack = new TunnellerHack();
	public final XRayHack xRayHack = new XRayHack();
	
	private final TreeMap<String, Hack> hax =
		new TreeMap<>((o1, o2) -> o1.compareToIgnoreCase(o2));
	
	private final EnabledHacksFile enabledHacksFile;
	private final Path profilesFolder =
		WurstClient.INSTANCE.getWurstFolder().resolve("enabled hacks");
	
	private final EventManager eventManager =
		WurstClient.INSTANCE.getEventManager();
	
	public HackList(Path enabledHacksFile)
	{
		this.enabledHacksFile = new EnabledHacksFile(enabledHacksFile);
		
		try
		{
			for(Field field : HackList.class.getDeclaredFields())
			{
				if(!field.getName().endsWith("Hack"))
					continue;
				
				Hack hack = (Hack)field.get(this);
				hax.put(hack.getName(), hack);
			}
			
		}catch(Exception e)
		{
			String message = "Initializing Wurst hacks";
			CrashReport report = CrashReport.create(e, message);
			throw new CrashException(report);
		}
		
		eventManager.add(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		enabledHacksFile.load(this);
		eventManager.remove(UpdateListener.class, this);
	}
	
	public void saveEnabledHax()
	{
		enabledHacksFile.save(this);
	}
	
	public Hack getHackByName(String name)
	{
		return hax.get(name);
	}
	
	public Collection<Hack> getAllHax()
	{
		return Collections.unmodifiableCollection(hax.values());
	}
	
	public int countHax()
	{
		return hax.size();
	}
	
	public ArrayList<Path> listProfiles()
	{
		if(!Files.isDirectory(profilesFolder))
			return new ArrayList<>();
		
		try(Stream<Path> files = Files.list(profilesFolder))
		{
			return files.filter(Files::isRegularFile)
				.collect(Collectors.toCollection(() -> new ArrayList<>()));
			
		}catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void loadProfile(String fileName) throws IOException, JsonException
	{
		enabledHacksFile.loadProfile(this, profilesFolder.resolve(fileName));
	}
	
	public void saveProfile(String fileName) throws IOException, JsonException
	{
		enabledHacksFile.saveProfile(this, profilesFolder.resolve(fileName));
	}
}
