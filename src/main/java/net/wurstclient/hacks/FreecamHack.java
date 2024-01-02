/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.*;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@DontSaveState
@SearchTags({"free camera", "spectator"})
public final class FreecamHack extends Hack implements UpdateListener,
	PacketOutputListener, IsPlayerInWaterListener, AirStrafingSpeedListener,
	IsPlayerInLavaListener, CameraTransformViewBobbingListener,
	IsNormalCubeListener, SetOpaqueCubeListener, RenderListener
{
	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting tracer = new CheckboxSetting("Tracer",
		"Draws a line to your character's actual position.", false);
	
	private final ColorSetting color =
		new ColorSetting("Tracer color", Color.WHITE);
	
	private FakePlayerEntity fakePlayer;
	
	public FreecamHack()
	{
		super("Freecam");
		setCategory(Category.RENDER);
		addSetting(speed);
		addSetting(tracer);
		addSetting(color);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
		EVENTS.add(IsPlayerInLavaListener.class, this);
		EVENTS.add(AirStrafingSpeedListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(IsNormalCubeListener.class, this);
		EVENTS.add(SetOpaqueCubeListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		fakePlayer = new FakePlayerEntity();
		
		GameOptions gs = MC.options;
		KeyBinding[] bindings = {gs.forwardKey, gs.backKey, gs.leftKey,
			gs.rightKey, gs.jumpKey, gs.sneakKey};
		
		for(KeyBinding binding : bindings)
			((IKeyBinding)binding).resetPressedState();
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
		EVENTS.remove(IsPlayerInLavaListener.class, this);
		EVENTS.remove(AirStrafingSpeedListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(IsNormalCubeListener.class, this);
		EVENTS.remove(SetOpaqueCubeListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		fakePlayer.resetPlayerPosition();
		fakePlayer.despawn();
		
		ClientPlayerEntity player = MC.player;
		player.setVelocity(Vec3d.ZERO);
		
		MC.worldRenderer.reload();
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		player.setVelocity(Vec3d.ZERO);
		player.getAbilities().flying = false;
		
		player.setOnGround(false);
		Vec3d velocity = player.getVelocity();
		
		if(MC.options.jumpKey.isPressed())
			player.setVelocity(velocity.add(0, speed.getValue(), 0));
		
		if(MC.options.sneakKey.isPressed())
			player.setVelocity(velocity.subtract(0, speed.getValue(), 0));
	}
	
	@Override
	public void onGetAirStrafingSpeed(AirStrafingSpeedEvent event)
	{
		event.setSpeed(speed.getValueF());
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof PlayerMoveC2SPacket)
			event.cancel();
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		event.setInWater(false);
	}
	
	@Override
	public void onIsPlayerInLava(IsPlayerInLavaEvent event)
	{
		event.setInLava(false);
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(tracer.isChecked())
			event.cancel();
	}
	
	@Override
	public void onIsNormalCube(IsNormalCubeEvent event)
	{
		event.cancel();
	}
	
	@Override
	public void onSetOpaqueCube(SetOpaqueCubeEvent event)
	{
		event.cancel();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(fakePlayer == null || !tracer.isChecked())
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		float[] colorF = color.getColorF();
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
		
		// box
		matrixStack.push();
		matrixStack.translate(fakePlayer.getX() - region.x(), fakePlayer.getY(),
			fakePlayer.getZ() - region.z());
		matrixStack.scale(fakePlayer.getWidth() + 0.1F,
			fakePlayer.getHeight() + 0.1F, fakePlayer.getWidth() + 0.1F);
		Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
		RenderUtils.drawOutlinedBox(bb, matrixStack);
		matrixStack.pop();
		
		// line
		Vec3d regionVec = region.toVec3d();
		Vec3d start = RotationUtils.getClientLookVec(partialTicks)
			.add(RenderUtils.getCameraPos()).subtract(regionVec);
		Vec3d end = fakePlayer.getBoundingBox().getCenter().subtract(regionVec);
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		bufferBuilder
			.vertex(matrix, (float)start.x, (float)start.y, (float)start.z)
			.next();
		bufferBuilder.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
			.next();
		tessellator.draw();
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
