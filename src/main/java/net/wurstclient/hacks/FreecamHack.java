/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
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
import net.wurstclient.util.RenderUtils;

@DontSaveState
@SearchTags({"free camera", "spectator", "baritone"})
public final class FreecamHack extends Hack implements UpdateListener,
	PacketOutputListener, IsPlayerInWaterListener, AirStrafingSpeedListener,
	IsPlayerInLavaListener, CameraTransformViewBobbingListener,
	IsNormalCubeListener, SetOpaqueCubeListener, RenderListener,
	MouseUpdateListener, LeftClickListener, RightClickListener
{
	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting baritoneCompat = new CheckboxSetting(
		"Baritone Compatibility",
		"Allows Baritone to control the player while you fly around with the camera.",
		false);
	
	private final CheckboxSetting tracer = new CheckboxSetting("Tracer",
		"Draws a line to your character's actual position.", false);
	
	private final ColorSetting color =
		new ColorSetting("Tracer color", Color.WHITE);
	
	private static final Field MOVE_VECTOR_FIELD = initMoveVectorField();
	
	private FakePlayerEntity fakePlayer;
	
	private double camX, camY, camZ;
	private float camYaw, camPitch;
	private double prevCamX, prevCamY, prevCamZ;
	
	public FreecamHack()
	{
		super("Freecam");
		setCategory(Category.RENDER);
		addSetting(speed);
		addSetting(baritoneCompat);
		addSetting(tracer);
		addSetting(color);
	}
	
	private static Field initMoveVectorField()
	{
		try
		{
			Field field = ClientInput.class.getDeclaredField("moveVector");
			field.setAccessible(true);
			return field;
		}catch(ReflectiveOperationException e)
		{
			return null;
		}
	}
	
	@Override
	protected void onEnable()
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
		EVENTS.add(MouseUpdateListener.class, this);
		EVENTS.add(LeftClickListener.class, this);
		EVENTS.add(RightClickListener.class, this);
		
		LocalPlayer player = MC.player;
		
		if(baritoneCompat.isChecked())
		{
			camX = prevCamX = player.getX();
			camY = prevCamY = player.getEyeY();
			camZ = prevCamZ = player.getZ();
			camYaw = player.getYRot();
			camPitch = player.getXRot();
			fakePlayer = null;
		}else
		{
			fakePlayer = new FakePlayerEntity();
		}
		
		Options opt = MC.options;
		KeyMapping[] bindings = {opt.keyUp, opt.keyDown, opt.keyLeft,
			opt.keyRight, opt.keyJump, opt.keyShift};
		
		for(KeyMapping binding : bindings)
			IKeyBinding.get(binding).resetPressedState();
	}
	
	@Override
	protected void onDisable()
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
		EVENTS.remove(MouseUpdateListener.class, this);
		EVENTS.remove(LeftClickListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
		
		if(fakePlayer != null)
		{
			fakePlayer.resetPlayerPosition();
			fakePlayer.despawn();
			fakePlayer = null;
			MC.player.setDeltaMovement(Vec3.ZERO);
		}
		
		MC.levelRenderer.allChanged();
	}
	
	@Override
	public void onUpdate()
	{
		LocalPlayer player = MC.player;
		
		if(baritoneCompat.isChecked())
		{
			updateCameraMovement();
			suppressPlayerInput(player);
		}else
		{
			player.setDeltaMovement(Vec3.ZERO);
			player.getAbilities().flying = false;
			
			player.setOnGround(false);
			Vec3 velocity = player.getDeltaMovement();
			
			if(MC.options.keyJump.isDown())
				player.setDeltaMovement(velocity.add(0, speed.getValue(), 0));
			
			if(MC.options.keyShift.isDown())
				player.setDeltaMovement(
					velocity.subtract(0, speed.getValue(), 0));
		}
	}
	
	private void suppressPlayerInput(LocalPlayer player)
	{
		if(player.input == null || !isBaritoneMode() || isBaritonePathing())
			return;
		
		if(player.input instanceof ClientInput clientInput)
			clearClientInput(clientInput);
		else
			player.input.keyPresses = Input.EMPTY;
	}
	
	public void clearClientInput(ClientInput clientInput)
	{
		if(clientInput == null)
			return;
		
		clientInput.keyPresses = Input.EMPTY;
		
		if(MOVE_VECTOR_FIELD != null)
			try
			{
				MOVE_VECTOR_FIELD.set(clientInput, Vec2.ZERO);
			}catch(IllegalAccessException ignored)
			{}
	}
	
	private void updateCameraMovement()
	{
		prevCamX = camX;
		prevCamY = camY;
		prevCamZ = camZ;
		
		double forward = 0;
		double strafe = 0;
		double vertical = 0;
		
		if(MC.options.keyUp.isDown())
			forward += 1;
		if(MC.options.keyDown.isDown())
			forward -= 1;
		if(MC.options.keyLeft.isDown())
			strafe += 1;
		if(MC.options.keyRight.isDown())
			strafe -= 1;
		if(MC.options.keyJump.isDown())
			vertical += 1;
		if(MC.options.keyShift.isDown())
			vertical -= 1;
		
		double yawRad = Math.toRadians(camYaw);
		double sin = Math.sin(yawRad);
		double cos = Math.cos(yawRad);
		
		double speedVal = speed.getValue();
		camX += (strafe * cos - forward * sin) * speedVal;
		camZ += (forward * cos + strafe * sin) * speedVal;
		camY += vertical * speedVal;
	}
	
	@Override
	public void onGetAirStrafingSpeed(AirStrafingSpeedEvent event)
	{
		if(baritoneCompat.isChecked())
			return;
		
		event.setSpeed(speed.getValueF());
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(baritoneCompat.isChecked())
			return;
		
		if(event.getPacket() instanceof ServerboundMovePlayerPacket)
			event.cancel();
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		if(baritoneCompat.isChecked())
			return;
		
		event.setInWater(false);
	}
	
	@Override
	public void onIsPlayerInLava(IsPlayerInLavaEvent event)
	{
		if(baritoneCompat.isChecked())
			return;
		
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
	public void onMouseUpdate(MouseUpdateEvent event)
	{
		if(!baritoneCompat.isChecked())
			return;
		
		if(MC.screen != null)
			return;
		
		double sensitivity = MC.options.sensitivity().get() * 0.6 + 0.2;
		double sensitivityCubed = sensitivity * sensitivity * sensitivity * 8.0;
		
		double deltaX = event.getDeltaX() * sensitivityCubed;
		double deltaY = event.getDeltaY() * sensitivityCubed;
		
		camYaw += (float)deltaX * 0.15f;
		camPitch = Mth.clamp(camPitch + (float)deltaY * 0.15f, -90.0f, 90.0f);
		
		event.setDeltaX(0);
		event.setDeltaY(0);
	}
	
	@Override
	public void onLeftClick(LeftClickListener.LeftClickEvent event)
	{
		if(!isBaritoneMode() || isBaritonePathing())
			return;
		
		event.cancel();
	}
	
	@Override
	public void onRightClick(RightClickListener.RightClickEvent event)
	{
		if(!isBaritoneMode() || isBaritonePathing())
			return;
		
		event.cancel();
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(!tracer.isChecked())
			return;
		
		int colorI = color.getColorI(0x80);
		AABB targetBox;
		
		if(baritoneCompat.isChecked())
			targetBox = MC.player.getBoundingBox();
		else
		{
			if(fakePlayer == null)
				return;
			targetBox = fakePlayer.getBoundingBox();
		}
		
		double extraSize = 0.05;
		AABB box = targetBox.move(0, extraSize, 0).inflate(extraSize);
		RenderUtils.drawOutlinedBox(matrixStack, box, colorI, false);
		
		if(baritoneCompat.isChecked())
		{
			Vec3 camPos = RenderUtils.getCameraPos();
			Vec3 lookVec = getLookVecFromRotation(camYaw, camPitch).scale(10);
			RenderUtils.drawLine(matrixStack, camPos.add(lookVec),
				targetBox.getCenter(), colorI, false);
		}else
		{
			RenderUtils.drawTracer(matrixStack, partialTicks,
				targetBox.getCenter(), colorI, false);
		}
	}
	
	private Vec3 getLookVecFromRotation(float yaw, float pitch)
	{
		float pitchRad = pitch * ((float)Math.PI / 180F);
		float yawRad = -yaw * ((float)Math.PI / 180F);
		float cosPitch = Mth.cos(pitchRad);
		float sinPitch = Mth.sin(pitchRad);
		float cosYaw = Mth.cos(yawRad);
		float sinYaw = Mth.sin(yawRad);
		return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
	}
	
	public boolean isBaritoneMode()
	{
		return isEnabled() && baritoneCompat.isChecked();
	}
	
	public boolean shouldPreventHotbarScrolling()
	{
		return isBaritoneMode();
	}
	
	public boolean isBaritonePathing()
	{
		try
		{
			Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
			Method getProvider = apiClass.getMethod("getProvider");
			Object provider = getProvider.invoke(null);
			if(provider == null)
				return false;
			
			Method getPrimaryBaritone =
				provider.getClass().getMethod("getPrimaryBaritone");
			Object baritone = getPrimaryBaritone.invoke(provider);
			if(baritone == null)
				return false;
			
			Object pathingBehavior = baritone.getClass()
				.getMethod("getPathingBehavior").invoke(baritone);
			if(pathingBehavior != null)
			{
				Method isPathing =
					pathingBehavior.getClass().getMethod("isPathing");
				if(Boolean.TRUE.equals(isPathing.invoke(pathingBehavior)))
					return true;
			}
			
			try
			{
				Object builderProcess = baritone.getClass()
					.getMethod("getBuilderProcess").invoke(baritone);
				if(builderProcess != null)
				{
					Method isActive =
						builderProcess.getClass().getMethod("isActive");
					if(Boolean.TRUE.equals(isActive.invoke(builderProcess)))
						return true;
				}
			}catch(NoSuchMethodException ignored)
			{}
			
			return false;
		}catch(ClassNotFoundException | NoSuchMethodException
			| IllegalAccessException | InvocationTargetException e)
		{
			return false;
		}
	}
	
	public double getCamX(float partialTicks)
	{
		return prevCamX + (camX - prevCamX) * partialTicks;
	}
	
	public double getCamY(float partialTicks)
	{
		return prevCamY + (camY - prevCamY) * partialTicks;
	}
	
	public double getCamZ(float partialTicks)
	{
		return prevCamZ + (camZ - prevCamZ) * partialTicks;
	}
	
	public float getCamYaw()
	{
		return camYaw;
	}
	
	public void setCamYaw(float yaw)
	{
		this.camYaw = yaw;
	}
	
	public float getCamPitch()
	{
		return camPitch;
	}
	
	public void setCamPitch(float pitch)
	{
		this.camPitch = pitch;
	}
	
	public Vec3 getRealPlayerPos()
	{
		if(fakePlayer != null)
			return fakePlayer.position();
		return MC.player.position();
	}
}
