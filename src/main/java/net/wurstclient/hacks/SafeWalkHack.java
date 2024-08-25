/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"safe walk", "SneakSafety", "sneak safety", "SpeedBridgeHelper",
	"speed bridge helper"})
public final class SafeWalkHack extends Hack
{
	private final SliderSetting minDepth = new SliderSetting("Min depth",
		"Won't sneak if it isn't at least this deep.\n"
			+ "Increase to stop SafeWalk from stucking on stairs.\n"
			+ "Decrease to make SafeWalk sneak even at the edge of carpets.",
		2.0, 0.1, 10, 0.1, ValueDisplay.DECIMAL.withSuffix("m"));
	
	private final SliderSetting motionPrediction = new SliderSetting(
		"Motion prediction",
		"Predict your motion to sneak earlier.\n"
			+ "If not stopping at edges, increase; If not stopping near a wall, decrease.",
		2.0, 1, 5, 0.5, ValueDisplay.DECIMAL.withSuffix("x"));
	
	private final CheckboxSetting sneak =
		new CheckboxSetting("Sneak at edges", "Visibly sneak at edges.", false);
	
	private final SliderSetting edgeDistance = new SliderSetting(
		"Sneak edge distance",
		"How close SafeWalk will let you get to the edge before sneaking.\n\n"
			+ "This setting is only used when \"Sneak at edges\" is enabled.",
		0.05, 0.05, 0.25, 0.001, ValueDisplay.DECIMAL.withSuffix("m"));
	
	private boolean sneaking;
	
	public SafeWalkHack()
	{
		super("SafeWalk");
		setCategory(Category.MOVEMENT);
		addSetting(minDepth);
		addSetting(motionPrediction);
		addSetting(sneak);
		addSetting(edgeDistance);
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().parkourHack.setEnabled(false);
		sneaking = false;
	}
	
	@Override
	protected void onDisable()
	{
		if(sneaking)
			setSneaking(false);
	}
	
	public boolean shouldClip()
	{
		ClientPlayerEntity player = MC.player;
		
		Box box = player.getBoundingBox();
		Box adjustedBox = box
			.offset(player.getVelocity().multiply(motionPrediction.getValue()))
			.stretch(0, -minDepth.getValue(), 0)
			.expand(-edgeDistance.getValue(), 0, -edgeDistance.getValue());
		
		return this.isEnabled() && MC.world.isSpaceEmpty(player, adjustedBox);
	}
	
	public void onClipAtLedge()
	{
		boolean clipping = false;
		
		ClientPlayerEntity player = MC.player;
		
		if(!isEnabled() || !sneak.isChecked() || !player.isOnGround())
		{
			if(sneaking)
				setSneaking(false);
			
			return;
		}
		
		Box box = player.getBoundingBox();
		Box adjustedBox = box
			.offset(player.getVelocity().multiply(motionPrediction.getValue()))
			.stretch(0, -minDepth.getValue(), 0)
			.expand(-edgeDistance.getValue(), 0, -edgeDistance.getValue());
		
		if(MC.world.isSpaceEmpty(player, adjustedBox))
			clipping = true;
		
		setSneaking(clipping);
	}
	
	private void setSneaking(boolean sneaking)
	{
		IKeyBinding sneakKey = IKeyBinding.get(MC.options.sneakKey);
		
		if(sneaking)
			sneakKey.setPressed(true);
		else
			sneakKey.resetPressedState();
		
		this.sneaking = sneaking;
	}
	
	// See ClientPlayerEntityMixin
}
