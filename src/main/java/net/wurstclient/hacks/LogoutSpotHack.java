/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EspBoxSizeSetting;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SearchTags({"logout", "player out", "logout spot"})
public final class LogoutSpotHack extends Hack
	implements UpdateListener, RenderListener
{
	record LogoutEntry(UUID uuid, Vec3d position)
	{}
	
	private final EspBoxSizeSetting boxSize = new EspBoxSizeSetting(
		"\u00a7lAccurate\u00a7r mode shows the exact hitbox of each player.\n"
			+ "\u00a7lFancy\u00a7r mode shows slightly larger boxes that look better.");
	
	private final ArrayList<PlayerEntity> lastPlayers = new ArrayList<>();
	private final ArrayList<LogoutEntry> logoutEntries = new ArrayList<>();
	
	public LogoutSpotHack()
	{
		super("LogOutSpot");
		setCategory(Category.RENDER);
		
		addSetting(boxSize);
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
	
	@Override
	public void onUpdate()
	{
		PlayerEntity localPlayer = MC.player;
		ClientWorld world = MC.world;
		
		ArrayList<PlayerEntity> currentPlayers = new ArrayList<>();
		Stream<AbstractClientPlayerEntity> stream = world.getPlayers()
			.parallelStream().filter(e -> !e.isRemoved() && e.getHealth() > 0)
			.filter(e -> e != localPlayer)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> Math.abs(e.getY() - localPlayer.getY()) <= 1e6);
		currentPlayers.addAll(stream.collect(Collectors.toList()));

//		Remove Spot if Comming back
		logoutEntries.removeIf(entry -> currentPlayers.stream()
			.anyMatch(p -> p.getUuid().equals(entry.uuid())));
		
		// lastPlayers에 있었으나 currentPlayers에는 없는 경우, 해당 플레이어가 로그아웃한 것으로 판단
		for(PlayerEntity p : lastPlayers)
		{
			if(!currentPlayers.contains(p))
			{
				// 로그아웃한 플레이어 데이터 생성 후 리스트에 추가
				logoutEntries.add(new LogoutEntry(p.getUuid(), p.getPos()));
			}
		}
		
		// 현재 플레이어 리스트를 lastPlayers로 업데이트
		lastPlayers.clear();
		lastPlayers.addAll(currentPlayers);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		float extraSize = boxSize.getExtraSize();
		
		for(LogoutEntry entry : logoutEntries)
		{
			matrixStack.push();
			
			// 로그아웃한 좌표를 카메라 기준 좌표로 변환
			Vec3d outPosition = entry.position().subtract(region.toVec3d());
			matrixStack.translate(outPosition.x, outPosition.y, outPosition.z);
			
			// 필요한 경우 크기를 조정 (예: 기본 박스 크기 적용)
			matrixStack.scale(1 + extraSize, 1 + extraSize, 1 + extraSize);
			
			// 로그아웃 박스는 예를 들어 빨간색으로 렌더링
			RenderSystem.setShaderColor(1, 0, 0, 0.5F);
			
			Box logoutBox = new Box(-0.5, 0, -0.5, 0.5, 2, 0.5);
			RenderUtils.drawOutlinedBox(logoutBox, matrixStack);
			
			matrixStack.pop();
		}
		
		matrixStack.pop();
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
