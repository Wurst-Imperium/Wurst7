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
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
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
@SearchTags({"free camera", "spectator", "ai", "baritone"})
public final class FreecamHack extends Hack implements UpdateListener,
	PacketOutputListener, IsPlayerInWaterListener, AirStrafingSpeedListener,
	IsPlayerInLavaListener, CameraTransformViewBobbingListener,
	IsNormalCubeListener, SetOpaqueCubeListener, RenderListener,
	MouseScrollListener, VelocityFromFluidListener, MouseUpdateListener,
	LeftClickListener, RightClickListener
{
	private final SliderSetting horizontalSpeed =
		new SliderSetting("Horizontal speed",
			"description.wurst.setting.freecam.horizontal_speed", 1, 0.05, 10,
			0.05, ValueDisplay.DECIMAL);
	
	private final SliderSetting verticalSpeed = new SliderSetting(
		"Vertical speed", "description.wurst.setting.freecam.vertical_speed", 1,
		0.05, 5, 0.05,
		v -> ValueDisplay.DECIMAL.getValueString(getActualVerticalSpeed()));
	
	private final CheckboxSetting scrollToChangeSpeed =
		new CheckboxSetting("Scroll to change speed",
			"description.wurst.setting.freecam.scroll_to_change_speed", true);
	
	private final CheckboxSetting renderSpeed =
		new CheckboxSetting("Show speed in HackList",
			"description.wurst.setting.freecam.show_speed_in_hacklist", false);
	
	private final CheckboxSetting aiCompatibility =
		new CheckboxSetting("Baritone Compatibility", "AI Compatibility",
			"description.wurst.setting.freecam.ai_compatibility", false);
	
	private final CheckboxSetting tracer = new CheckboxSetting("Tracer",
		"description.wurst.setting.freecam.tracer", false);
	
	private final ColorSetting color =
		new ColorSetting("Tracer color", Color.WHITE);
	
	private final CheckboxSetting disableOnDamage =
		new CheckboxSetting("Disable on damage",
			"description.wurst.setting.freecam.disable_on_damage", true);
	
	private static final Field MOVE_VECTOR_FIELD = initMoveVectorField();
	
	private FakePlayerEntity fakePlayer;
	private float lastHealth = Float.MIN_VALUE;
	
	private double camX, camY, camZ;
	private float camYaw, camPitch;
	private double prevCamX, prevCamY, prevCamZ;
	
	public FreecamHack()
	{
		super("Freecam");
		setCategory(Category.RENDER);
		addSetting(horizontalSpeed);
		addSetting(verticalSpeed);
		addSetting(scrollToChangeSpeed);
		addSetting(renderSpeed);
		addSetting(aiCompatibility);
		addSetting(tracer);
		addSetting(color);
		addSetting(disableOnDamage);
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
	public String getRenderName()
	{
		if(!renderSpeed.isChecked())
			return getName();
		
		return getName() + " [" + horizontalSpeed.getValueString() + ", "
			+ verticalSpeed.getValueString() + "]";
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
		EVENTS.add(MouseScrollListener.class, this);
		EVENTS.add(VelocityFromFluidListener.class, this);
		EVENTS.add(MouseUpdateListener.class, this);
		EVENTS.add(LeftClickListener.class, this);
		EVENTS.add(RightClickListener.class, this);
		
		LocalPlayer player = MC.player;
		
		if(aiCompatibility.isChecked())
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
		EVENTS.remove(MouseScrollListener.class, this);
		EVENTS.remove(VelocityFromFluidListener.class, this);
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
		
		lastHealth = Float.MIN_VALUE;
		
		MC.levelRenderer.allChanged();
	}
	
	@Override
	public void onUpdate()
	{
		LocalPlayer player = MC.player;
		
		float currentHealth = player.getHealth();
		if(disableOnDamage.isChecked() && currentHealth < lastHealth)
		{
			setEnabled(false);
			return;
		}
		lastHealth = currentHealth;
		
		if(isAiCompatibilityMode())
		{
			suppressPhysicallyPressedKeys();
			if(MC.screen == null)
				updateCameraMovement();
			return;
		}
		
		player.setDeltaMovement(Vec3.ZERO);
		player.getAbilities().flying = false;
		
		player.setOnGround(false);
		double vSpeed = getActualVerticalSpeed();
		
		if(MC.options.keyJump.isDown())
			player.addDeltaMovement(new Vec3(0, vSpeed, 0));
		
		if(IKeyBinding.get(MC.options.keyShift).isActuallyDown())
		{
			MC.options.keyShift.setDown(false);
			player.addDeltaMovement(new Vec3(0, -vSpeed, 0));
		}
	}
	
	private double getActualVerticalSpeed()
	{
		return Mth.clamp(horizontalSpeed.getValue() * verticalSpeed.getValue(),
			0.05, 10);
	}
	
	private void suppressPhysicallyPressedKeys()
	{
		Options opt = MC.options;
		KeyMapping[] keys =
			{opt.keyUp, opt.keyDown, opt.keyLeft, opt.keyRight, opt.keyJump,
				opt.keyShift, opt.keyAttack, opt.keyUse, opt.keyPickItem};
		
		for(KeyMapping key : keys)
			if(isActuallyDown(key))
				key.setDown(false);
			
		for(KeyMapping key : opt.keyHotbarSlots)
			if(isActuallyDown(key))
				key.setDown(false);
	}
	
	@Override
	public void onGetAirStrafingSpeed(AirStrafingSpeedEvent event)
	{
		if(isAiCompatibilityMode())
			return;
		
		event.setSpeed(horizontalSpeed.getValueF());
	}
	
	@Override
	public void onMouseScroll(double amount)
	{
		if(!scrollToChangeSpeed.isChecked() || MC.screen != null)
			return;
		
		if(WURST.getOtfs().zoomOtf.shouldPreventHotbarScrolling())
			return;
		
		if(amount > 0)
			horizontalSpeed.increaseValue();
		else if(amount < 0)
			horizontalSpeed.decreaseValue();
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(isAiCompatibilityMode())
			return;
		
		if(event.getPacket() instanceof ServerboundMovePlayerPacket)
			event.cancel();
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		if(isAiCompatibilityMode())
			return;
		
		event.setInWater(false);
	}
	
	@Override
	public void onIsPlayerInLava(IsPlayerInLavaEvent event)
	{
		if(isAiCompatibilityMode())
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
	public void onVelocityFromFluid(VelocityFromFluidEvent event)
	{
		if(isAiCompatibilityMode())
			return;
		
		if(event.getEntity() == MC.player)
			event.cancel();
	}
	
	@Override
	public void onMouseUpdate(MouseUpdateEvent event)
	{
		if(!isAiCompatibilityMode())
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
	public void onLeftClick(LeftClickEvent event)
	{
		if(!isAiCompatibilityMode() || !isExternalAiPathing())
			return;
		
		event.cancel();
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		if(!isAiCompatibilityMode() || !isExternalAiPathing())
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
		
		if(isAiCompatibilityMode())
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
		
		if(isAiCompatibilityMode())
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
	
	private void updateCameraMovement()
	{
		prevCamX = camX;
		prevCamY = camY;
		prevCamZ = camZ;
		
		double forward = 0;
		double strafe = 0;
		double vertical = 0;
		
		if(isActuallyDown(MC.options.keyUp))
			forward += 1;
		if(isActuallyDown(MC.options.keyDown))
			forward -= 1;
		if(isActuallyDown(MC.options.keyLeft))
			strafe += 1;
		if(isActuallyDown(MC.options.keyRight))
			strafe -= 1;
		if(isActuallyDown(MC.options.keyJump))
			vertical += 1;
		if(isActuallyDown(MC.options.keyShift))
			vertical -= 1;
		
		double yawRad = Math.toRadians(camYaw);
		double sin = Math.sin(yawRad);
		double cos = Math.cos(yawRad);
		
		double hSpeed = horizontalSpeed.getValue();
		double vSpeed = getActualVerticalSpeed();
		
		camX += (strafe * cos - forward * sin) * hSpeed;
		camZ += (forward * cos + strafe * sin) * hSpeed;
		camY += vertical * vSpeed;
	}
	
	private boolean isActuallyDown(KeyMapping key)
	{
		return IKeyBinding.get(key).isActuallyDown();
	}
	
	public boolean isAiCompatibilityMode()
	{
		return isEnabled() && aiCompatibility.isChecked();
	}
	
	public boolean shouldPreventHotbarScrolling()
	{
		return isAiCompatibilityMode() && MC.screen == null;
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
	
	public boolean isExternalAiPathing()
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
	
	public float getCamPitch()
	{
		return camPitch;
	}
	
	public Vec3 getRealPlayerPos()
	{
		if(fakePlayer != null)
			return fakePlayer.position();
		return MC.player.position();
	}
}
