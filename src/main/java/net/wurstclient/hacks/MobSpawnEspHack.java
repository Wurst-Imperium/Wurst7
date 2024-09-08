/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.settings.ChunkAreaSetting.ChunkArea;
import net.wurstclient.util.ChunkSearcherCoordinator;

@SearchTags({"mob spawn esp", "LightLevelESP", "light level esp",
	"LightLevelOverlay", "light level overlay"})
public final class MobSpawnEspHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final ChunkAreaSetting drawDistance =
		new ChunkAreaSetting("Draw distance", "", ChunkArea.A9);
	
	private final ChunkSearcherCoordinator coordinator =
		new ChunkSearcherCoordinator(drawDistance);
	
	public MobSpawnEspHack()
	{
		super("MobSpawnESP");
		setCategory(Category.RENDER);
		addSetting(drawDistance);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		coordinator.setQuery(this::isSpawnable);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		
	}
	
	private boolean isSpawnable(BlockPos pos, BlockState state)
	{
		// Check for solid blocks, fluids, redstone, prevent_spawning tags, etc.
		// See SpawnLocationTypes.ON_GROUND
		if(!SpawnRestriction.isSpawnPosAllowed(EntityType.CREEPER, MC.world,
			pos))
			return false;
			
		// Check for hitbox collisions
		// (using a creeper because it's shorter than a zombie)
		if(!MC.world.isSpaceEmpty(EntityType.CREEPER
			.getSpawnBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)))
			return false;
		
		// Check block light level
		return MC.world.getLightLevel(LightType.BLOCK, pos) < 1;
	}
}
