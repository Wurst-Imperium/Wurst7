/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.*;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerEntity;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@DontSaveState
@SearchTags({"free camera", "spectator"})
public final class FreecamHack extends Hack
	implements UpdateListener, PacketOutputListener, PlayerMoveListener,
	IsPlayerInWaterListener, CameraTransformViewBobbingListener,
	IsNormalCubeListener, SetOpaqueCubeListener, RenderListener
{
	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	private final CheckboxSetting tracer = new CheckboxSetting("Tracer",
		"Draws a line to your character's actual position.", false);
	
	private FakePlayerEntity fakePlayer;
	private int playerBox;
	
	public FreecamHack()
	{
		super("Freecam",
			"Allows you to move the camera without moving your character.");
		setCategory(Category.RENDER);
		addSetting(speed);
		addSetting(tracer);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
		EVENTS.add(PlayerMoveListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(IsNormalCubeListener.class, this);
		EVENTS.add(SetOpaqueCubeListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		fakePlayer = new FakePlayerEntity();
		
		GameOptions gs = MC.options;
		KeyBinding[] bindings = {gs.keyForward, gs.keyBack, gs.keyLeft,
			gs.keyRight, gs.keyJump, gs.keySneak};
		
		for(KeyBinding binding : bindings)
			binding.setPressed(((IKeyBinding)binding).isActallyPressed());
		
		playerBox = GL11.glGenLists(1);
		GL11.glNewList(playerBox, GL11.GL_COMPILE);
		Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
		RenderUtils.drawOutlinedBox(bb);
		GL11.glEndList();
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
		EVENTS.remove(PlayerMoveListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(IsNormalCubeListener.class, this);
		EVENTS.remove(SetOpaqueCubeListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		fakePlayer.resetPlayerPosition();
		fakePlayer.despawn();
		
		ClientPlayerEntity player = MC.player;
		player.setVelocity(Vec3d.ZERO);
		
		MC.worldRenderer.reload();
		
		GL11.glDeleteLists(playerBox, 1);
		playerBox = 0;
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		player.setVelocity(Vec3d.ZERO);
		
		player.onGround = false;
		player.flyingSpeed = speed.getValueF();
		Vec3d velcity = player.getVelocity();
		
		if(MC.options.keyJump.isPressed())
			player.setVelocity(velcity.add(0, speed.getValue(), 0));
		
		if(MC.options.keySneak.isPressed())
			player.setVelocity(velcity.subtract(0, speed.getValue(), 0));
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof PlayerMoveC2SPacket)
			event.cancel();
	}
	
	@Override
	public void onPlayerMove(IClientPlayerEntity player)
	{
		player.setNoClip(true);
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		event.setInWater(false);
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
	public void onRender(float partialTicks)
	{
		if(fakePlayer == null || !tracer.isChecked())
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		GL11.glColor4f(1, 1, 1, 0.5F);
		
		// box
		GL11.glPushMatrix();
		GL11.glTranslated(fakePlayer.getX(), fakePlayer.getY(),
			fakePlayer.getZ());
		GL11.glScaled(fakePlayer.getWidth() + 0.1, fakePlayer.getHeight() + 0.1,
			fakePlayer.getWidth() + 0.1);
		GL11.glCallList(playerBox);
		GL11.glPopMatrix();
		
		// line
		Vec3d start =
			RotationUtils.getClientLookVec().add(RenderUtils.getCameraPos());
		Vec3d end = fakePlayer.getBoundingBox().getCenter();
		
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(start.x, start.y, start.z);
		GL11.glVertex3d(end.x, end.y, end.z);
		GL11.glEnd();
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
}
