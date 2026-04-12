/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RenderUtils;

import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.mojang.blaze3d.vertex.PoseStack;

@SearchTags({"logout", "player out", "logout spot"})
public final class LogoutSpotHack extends Hack
	implements UpdateListener, RenderListener
{
	private final ColorSetting color =
		new ColorSetting("Box Color", Color.WHITE);
	
	private record Entry(UUID uuid, Vec3i position, Instant instant)
	{}
	
	private Map<UUID, String> onlinePlayers = new HashMap<>();
	private Map<UUID, Vec3i> renderPlayers = new HashMap<>();
	private Map<UUID, String> lastPlayers = new HashMap<>();
	private final Map<UUID, Entry> logOutPlayers = new HashMap<>();
	private String currentJoinServer;
	
	public LogoutSpotHack()
	{
		super("LogOutSpot");
		setCategory(Category.RENDER);
		
		addSetting(color);
		
		scheduler.scheduleWithFixedDelay(
			() -> logOutPlayers.entrySet()
				.removeIf(entry -> Instant.now().isAfter(
					entry.getValue().instant.plus(10, ChronoUnit.MINUTES))),
			0, 5, TimeUnit.MINUTES);
		
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			
			if(MC.getConnection() == null)
				return;
			
			String serverAddress = MC.getConnection().getServerData().ip;
			if(serverAddress.equals(currentJoinServer))
				return;
			
			currentJoinServer = serverAddress;
			logOutPlayers.clear();
		});
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}
	
	/**
	 * 1. Get the entire PlayerList from the server using {@link Minecraft#getInstance()}.
	 * 2. If lastPlayerList and the current PlayerList are the same, SKIP.
	 * 3. If lastPlayerList and the new PlayerList are different, and the player is rendered in view,
	 * add to logoutEntries.
	 * 4. Remove players from logoutEntries if they are not being rendered.
	 * 5. Update lastPlayerList to the new PlayerList.
	 */
	@Override
	public void onUpdate()
	{
		// Online player list (Network tab list)
		onlinePlayers =
			Minecraft.getInstance().getConnection().getOnlinePlayers().stream()
				.collect(Collectors.toMap(entry -> entry.getProfile().id(),
					entry -> entry.getProfile().name()));
		
		// If reconnected to online players, remove from logOutPlayers
		logOutPlayers.entrySet()
			.removeIf(entry -> onlinePlayers.containsKey(entry.getKey()));
		
		// Skip update if the player count hasn't changed
		if(onlinePlayers.size() == lastPlayers.size())
		{

			renderPlayers = Minecraft.getInstance().level.players().stream()
				.filter(e -> !(e instanceof FakePlayerEntity))
				.collect(Collectors.toMap(Entity::getUUID,
					Entity::blockPosition // BlockPos extends Vec3i, so it is compatible
				));
			return;
		}
		
		for(UUID uuid : lastPlayers.keySet())
		{
			if(!onlinePlayers.containsKey(uuid))
			{ // If the player is not on the server
				System.out.println(renderPlayers.get(uuid));
				Optional.ofNullable(renderPlayers.get(uuid))
					.ifPresent(pos -> logOutPlayers.put(uuid,
						new Entry(uuid, pos, Instant.now())));
			}
		}
		
		// Finally, update lastPlayers with all UUIDs from onlinePlayers
		lastPlayers.clear();
		lastPlayers = onlinePlayers;
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(logOutPlayers.isEmpty())
			return;
		
		List<AABB> logoutPlayerPositionBox = new ArrayList<>();
		for(Entry entry : logOutPlayers.values())
		{
			Vec3i targetExitPosition = entry.position();
			logoutPlayerPositionBox.add(new AABB(
				targetExitPosition.getX() - 0.5, targetExitPosition.getY(),
				targetExitPosition.getZ() - 0.5,
				targetExitPosition.getX() + 0.5, targetExitPosition.getY() + 2,
				targetExitPosition.getZ() + 0.5));
		}
		
		RenderUtils.drawSolidBoxes(matrixStack, logoutPlayerPositionBox,
			color.getColorI(0x80), false);
	}
}
