/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.sentry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.blaze3d.platform.GlDebugInfo;

import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.util.crash.CrashReport;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.Setting;

public enum SentryConfig
{
	;
	
	private static final String DSN =
		"https://c01aef15a7cb466da7824ec5dac0d009@sentry.io/5464583";
	
	private static SentryConfigFile configFile;
	
	public static void setupSentry(Path path)
	{
		configFile = new SentryConfigFile(path);
		configFile.load();
		
		FabricLoader fabricLoader = FabricLoader.getInstance();
		
		Sentry.init(options -> {
			options.setDsn(DSN);
			options.setRelease(getRelease(fabricLoader));
		});
		
		addVersions(fabricLoader);
		addEnvironmentType(fabricLoader);
		addOsInfo();
		addJavaInfo();
		addMods(fabricLoader);
	}
	
	private static String getRelease(FabricLoader fabricLoader)
	{
		String modVersion = fabricLoader.getModContainer("wurst").get()
			.getMetadata().getVersion().getFriendlyString();
		
		if(modVersion != null && !modVersion.equals("${version}"))
			return modVersion;
		
		return "v" + WurstClient.VERSION + "-MC" + WurstClient.MC_VERSION;
	}
	
	private static void addVersions(FabricLoader fabricLoader)
	{
		Sentry.configureScope(scope -> {
			scope.setTag("wurst.version", WurstClient.VERSION);
		});
		
		Sentry.configureScope(scope -> {
			scope.setTag("mc.version",
				SharedConstants.getGameVersion().getName());
		});
		
		Sentry.configureScope(scope -> {
			scope.setTag("fabric.api_version",
				fabricLoader.getModContainer("fabric").get().getMetadata()
					.getVersion().getFriendlyString());
		});
		
		Sentry.configureScope(scope -> {
			scope.setTag("fabric.loader_version",
				fabricLoader.getModContainer("fabricloader").get().getMetadata()
					.getVersion().getFriendlyString());
		});
	}
	
	private static void addEnvironmentType(FabricLoader fabricLoader)
	{
		Sentry.configureScope(scope -> {
			boolean dev = fabricLoader.isDevelopmentEnvironment();
			scope.setTag("environment", dev ? "dev" : "prod");
		});
	}
	
	private static void addOsInfo()
	{
		Sentry.configureScope(scope -> {
			
			HashMap<String, String> map = new HashMap<>();
			map.put("name", System.getProperty("os.name"));
			scope.setContexts("os", map);
			
			scope.setTag("os.arch", System.getProperty("os.arch"));
		});
	}
	
	private static void addJavaInfo()
	{
		Sentry.configureScope(scope -> {
			
			HashMap<String, String> map = new HashMap<>();
			map.put("runtime", System.getProperty("java.runtime.name"));
			map.put("version", System.getProperty("java.runtime.version"));
			map.put("vendor", System.getProperty("java.vendor"));
			map.put("vm", System.getProperty("java.vm.name") + " ("
				+ System.getProperty("java.vm.info") + ")");
			scope.setContexts("java", map);
			
			scope.setTag("java.version", System.getProperty("java.version"));
			scope.setTag("java.vendor", System.getProperty("java.vendor"));
			scope.setTag("java.vm", System.getProperty("java.vm.name"));
		});
	}
	
	private static void addMods(FabricLoader fabricLoader)
	{
		Sentry.configureScope(scope -> {
			
			HashMap<String, String> modMap = getModMap(fabricLoader);
			scope.setContexts("mods", modMap);
			
			scope.setTag("other_mods", getOtherMods(modMap));
		});
	}
	
	private static HashMap<String, String> getModMap(FabricLoader fabricLoader)
	{
		Stream<ModMetadata> modStream =
			fabricLoader.getAllMods().stream().map(mod -> mod.getMetadata());
		
		// filter out those Fabric API sub-mod things
		modStream = modStream.filter(mod -> !mod.getId().startsWith("fabric-"));
		
		ArrayList<ModMetadata> modList =
			modStream.collect(Collectors.toCollection(() -> new ArrayList<>()));
		
		// create a map of each mod's ID and version
		HashMap<String, String> modMap = new HashMap<>();
		for(ModMetadata mod : modList)
			modMap.put(mod.getId(), mod.getVersion().getFriendlyString());
		
		return modMap;
	}
	
	/**
	 * Returns the number of installed mods that aren't part of Wurst, Fabric or
	 * Minecraft.
	 */
	private static String getOtherMods(HashMap<String, String> modMap)
	{
		HashSet<String> otherMods = new HashSet<>(modMap.keySet());
		otherMods.remove("minecraft");
		otherMods.remove("fabric");
		otherMods.remove("fabricloader");
		otherMods.remove("wurst");
		otherMods.remove("io_sentry_sentry");
		return "" + otherMods.size();
	}
	
	public static boolean isEnabled()
	{
		return configFile.isEnabled();
	}
	
	public static void setEnabled(boolean enabled)
	{
		configFile.setEnabled(enabled);
		configFile.save();
	}
	
	public static void addHackToggleBreadcrumb(Hack hack, boolean enabled)
	{
		Breadcrumb breadcrumb = new Breadcrumb(hack.getName());
		breadcrumb.setCategory("hack." + (enabled ? "enable" : "disable"));
		
		for(Entry<String, Setting> e : hack.getSettings().entrySet())
			breadcrumb.setData(e.getValue().getName(),
				e.getValue().toJson().toString());
		
		Sentry.addBreadcrumb(breadcrumb);
	}
	
	public static void addKeybindTriggerBreadcrumb(String keyName, String cmds)
	{
		Breadcrumb breadcrumb = new Breadcrumb(cmds);
		breadcrumb.setCategory("keybind.trigger");
		breadcrumb.setData("key", keyName);
		Sentry.addBreadcrumb(breadcrumb);
	}
	
	public static void addScreenChangeBreadcrumb(Screen screen)
	{
		Breadcrumb breadcrumb = new Breadcrumb();
		breadcrumb.setType("navigation");
		breadcrumb.setCategory("screen.change");
		
		Screen cs = WurstClient.MC.currentScreen;
		String from = cs == null ? "none" : cs.getClass().getCanonicalName();
		breadcrumb.setData("from",
			StacktraceDeobfuscator.deobfuscateClass(from));
		
		String to =
			screen == null ? "none" : screen.getClass().getCanonicalName();
		breadcrumb.setData("to", StacktraceDeobfuscator.deobfuscateClass(to));
		
		Sentry.addBreadcrumb(breadcrumb);
	}
	
	public static void addDetailsOnCrash()
	{
		addCpuInfo();
		addGpuInfo();
		addLanguage();
		addFontType();
		addCurrentScreen();
		addWurstInfo();
	}
	
	private static void addCpuInfo()
	{
		Sentry.configureScope(scope -> {
			HashMap<String, String> map = new HashMap<>();
			map.put("name", GlDebugInfo.getCpuInfo());
			scope.setContexts("cpu", map);
		});
	}
	
	private static void addGpuInfo()
	{
		Sentry.configureScope(scope -> {
			
			HashMap<String, String> map = new HashMap<>();
			
			map.put("name", GlDebugInfo.getRenderer());
			map.put("version", GlDebugInfo.getVersion());
			map.put("vendor_name", GlDebugInfo.getVendor());
			
			Window window = WurstClient.MC.getWindow();
			map.put("framebuffer", window.getFramebufferWidth() + "x"
				+ window.getFramebufferHeight());
			
			scope.setContexts("gpu", map);
		});
	}
	
	private static void addLanguage()
	{
		Sentry.configureScope(scope -> {
			scope.setTag("mc.lang",
				WurstClient.MC.getLanguageManager().getLanguage().getCode());
		});
	}
	
	private static void addFontType()
	{
		Sentry.configureScope(scope -> {
			scope.setTag("mc.font",
				WurstClient.MC.forcesUnicodeFont() ? "unicode" : "default");
		});
	}
	
	private static void addCurrentScreen()
	{
		Sentry.configureScope(scope -> {
			Screen cs = WurstClient.MC.currentScreen;
			String screen =
				cs == null ? "none" : cs.getClass().getCanonicalName();
			scope.setTag("mc.screen", screen);
		});
	}
	
	private static void addWurstInfo()
	{
		Sentry.configureScope(scope -> {
			HashMap<String, Object> map = new HashMap<>();
			map.put("enabled_hacks", getEnabledHax());
			map.put("settings", getSettingsMap());
			scope.setContexts("wurst", map);
		});
	}
	
	private static ArrayList<String> getEnabledHax()
	{
		return WurstClient.INSTANCE.getHax().getAllHax().stream()
			.filter(Hack::isEnabled).map(Hack::getName)
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
	}
	
	private static HashMap<String, HashMap<String, String>> getSettingsMap()
	{
		WurstClient wurst = WurstClient.INSTANCE;
		
		ArrayList<Feature> features = new ArrayList<>();
		features.addAll(wurst.getHax().getAllHax());
		features.addAll(wurst.getCmds().getAllCmds());
		features.addAll(wurst.getOtfs().getAllOtfs());
		
		HashMap<String, HashMap<String, String>> settingsMap = new HashMap<>();
		
		for(Feature feature : features)
		{
			Collection<Setting> ftSettings = feature.getSettings().values();
			if(ftSettings.isEmpty())
				continue;
			
			HashMap<String, String> ftSettingsMap = new HashMap<>();
			for(Setting setting : ftSettings)
				ftSettingsMap.put(setting.getName(),
					setting.toJson().toString());
			
			settingsMap.put(feature.getName(), ftSettingsMap);
		}
		
		return settingsMap;
	}
	
	public static void reportCrash(CrashReport report)
	{
		if(configFile != null && !configFile.isEnabled())
			return;
			
		// don't report crash if the version is known to be outdated, but still
		// report if the updater didn't get a chance to check before the crash
		WurstClient wurst = WurstClient.INSTANCE;
		if(wurst.getUpdater() != null && wurst.getUpdater().isOutdated())
			return;
		
		Throwable cause = report.getCause();
		StacktraceDeobfuscator.deobfuscateThrowable(cause);
		Sentry.captureException(cause);
	}
}
