/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.*;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyMapping;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RenderUtils;

@DontSaveState
@SearchTags({"free camera", "spectator"})
public final class FreecamHack extends Hack
	implements UpdateListener, PacketOutputListener, IsPlayerInWaterListener,
	AirStrafingSpeedListener, IsPlayerInLavaListener,
	CameraTransformViewBobbingListener, IsNormalCubeListener, VisGraphListener,
	RenderListener, MouseScrollListener, VelocityFromFluidListener
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
			"description.wurst.setting.freecam.show_speed_in_hacklist", true);
	
	private final CheckboxSetting tracer = new CheckboxSetting("Tracer",
		"description.wurst.setting.freecam.tracer", false);
	
	private final ColorSetting color =
		new ColorSetting("Tracer color", Color.WHITE);
	
	private final CheckboxSetting hideHand = new CheckboxSetting("Hide hand",
		"description.wurst.setting.freecam.hide_hand", false);
	
	private final CheckboxSetting disableOnDamage =
		new CheckboxSetting("Disable on damage",
			"description.wurst.setting.freecam.disable_on_damage", true);
	
	private FakePlayerEntity fakePlayer;
	private float lastHealth = Float.MIN_VALUE;
	
	public FreecamHack()
	{
		super("Freecam");
		setCategory(Category.RENDER);
		addSetting(horizontalSpeed);
		addSetting(verticalSpeed);
		addSetting(scrollToChangeSpeed);
		addSetting(renderSpeed);
		addSetting(tracer);
		addSetting(color);
		addSetting(hideHand);
		addSetting(disableOnDamage);
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
		EVENTS.add(VisGraphListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(MouseScrollListener.class, this);
		EVENTS.add(VelocityFromFluidListener.class, this);
		
		fakePlayer = new FakePlayerEntity();
		
		Options opt = MC.options;
		KeyMapping[] bindings = {opt.keyUp, opt.keyDown, opt.keyLeft,
			opt.keyRight, opt.keyJump, opt.keyShift};
		
		for(KeyMapping binding : bindings)
			IKeyMapping.get(binding).resetPressedState();
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
		EVENTS.remove(VisGraphListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(MouseScrollListener.class, this);
		EVENTS.remove(VelocityFromFluidListener.class, this);
		
		fakePlayer.resetPlayerPosition();
		fakePlayer.despawn();
		lastHealth = Float.MIN_VALUE;
		
		LocalPlayer player = MC.player;
		player.setDeltaMovement(Vec3.ZERO);
		
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
		
		player.setDeltaMovement(Vec3.ZERO);
		player.getAbilities().flying = false;
		player.setOnGround(false);
		
		double vSpeed = getActualVerticalSpeed();
		
		if(MC.options.keyJump.isDown())
			player.addDeltaMovement(new Vec3(0, vSpeed, 0));
		
		if(IKeyMapping.get(MC.options.keyShift).isActuallyDown())
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
	
	@Override
	public void onGetAirStrafingSpeed(AirStrafingSpeedEvent event)
	{
		event.setSpeed(horizontalSpeed.getValueF());
	}
	
	@Override
	public void onMouseScroll(double amount)
	{
		if(!scrollToChangeSpeed.isChecked() || MC.screen != null)
			return;
		
		if(WURST.getOtfs().zoomOtf.isControllingScrollEvents())
			return;
		
		if(amount > 0)
			horizontalSpeed.increaseValue();
		else if(amount < 0)
			horizontalSpeed.decreaseValue();
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof ServerboundMovePlayerPacket)
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
	public void onVisGraph(VisGraphEvent event)
	{
		event.cancel();
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(fakePlayer == null || !tracer.isChecked())
			return;
		
		int colorI = color.getColorI(0x80);
		
		// box
		double extraSize = 0.05;
		AABB box = fakePlayer.getBoundingBox().move(0, extraSize, 0)
			.inflate(extraSize);
		RenderUtils.drawOutlinedBox(matrixStack, box, colorI, false);
		
		// line
		RenderUtils.drawTracer(matrixStack, partialTicks,
			fakePlayer.getBoundingBox().getCenter(), colorI, false);
	}
	
	@Override
	public void onVelocityFromFluid(VelocityFromFluidEvent event)
	{
		if(event.getEntity() == MC.player)
			event.cancel();
	}
	
	public boolean shouldHideHand()
	{
		return isEnabled() && hideHand.isChecked();
	}
}
