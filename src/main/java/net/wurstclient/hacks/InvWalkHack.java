/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.client.gui.screen.ingame.BlastFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.client.gui.screen.ingame.CartographyTableScreen;
import net.minecraft.client.gui.screen.ingame.CommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.SmithingScreen;
import net.minecraft.client.gui.screen.ingame.SmokerScreen;
import net.minecraft.client.gui.screen.ingame.StonecutterScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.clickgui.screens.ClickGuiScreen;
import net.wurstclient.clickgui.screens.EditBlockListScreen;
import net.wurstclient.clickgui.screens.EditBlockScreen;
import net.wurstclient.clickgui.screens.EditItemListScreen;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.navigator.NavigatorScreen;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"inventory walk", "InvMove"})
public final class InvWalkHack extends Hack implements UpdateListener
{
	private final CheckboxSetting exception =
			new CheckboxSetting("Only allow player inventory",
				"Blocks all other containers or instances of GUIs",
				false);
	
	public InvWalkHack()
	{
		super("InvWalk", "Enables movement in inventory and container screens");
		setCategory(Category.MOVEMENT);
		addSetting(exception);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if (avoid() || optional() && exception.isChecked())
			return;
		
		IKeyBinding forwardKey = (IKeyBinding)MC.options.keyForward;
		((KeyBinding)forwardKey).setPressed(forwardKey.isActallyPressed());
		
		IKeyBinding backKey = (IKeyBinding)MC.options.keyBack;
		((KeyBinding)backKey).setPressed(backKey.isActallyPressed());
		
		IKeyBinding leftKey = (IKeyBinding)MC.options.keyLeft;
		((KeyBinding)leftKey).setPressed(leftKey.isActallyPressed());
		
		IKeyBinding rightKey = (IKeyBinding)MC.options.keyRight;
		((KeyBinding)rightKey).setPressed(rightKey.isActallyPressed());
		
		IKeyBinding jumpKey = (IKeyBinding)MC.options.keyJump;
		((KeyBinding)jumpKey).setPressed(jumpKey.isActallyPressed());
		
		IKeyBinding sprintKey = (IKeyBinding)MC.options.keySprint;
		((KeyBinding)sprintKey).setPressed(sprintKey.isActallyPressed());
		
		IKeyBinding sneakKey = (IKeyBinding)MC.options.keySneak;
		((KeyBinding)sneakKey).setPressed(sneakKey.isActallyPressed());	
			
	}
	 private boolean avoid()
	    {
	        return MC.currentScreen == null || (MC.currentScreen instanceof CreativeInventoryScreen 
	        		|| MC.currentScreen instanceof ChatScreen || MC.currentScreen instanceof SignEditScreen
	        		|| MC.currentScreen instanceof CommandBlockScreen || MC.currentScreen instanceof EditBlockListScreen
	        		|| MC.currentScreen instanceof EditBlockScreen || MC.currentScreen instanceof EditItemListScreen
	        		|| MC.currentScreen instanceof AnvilScreen || MC.currentScreen instanceof AbstractCommandBlockScreen 
	        		|| MC.currentScreen instanceof StructureBlockScreen || MC.currentScreen instanceof ClickGuiScreen
	        		|| MC.currentScreen instanceof NavigatorScreen);
	    }
	 
	 private boolean optional()
		{
			return MC.currentScreen == null || (MC.currentScreen instanceof CraftingScreen
					|| MC.currentScreen instanceof GenericContainerScreen || MC.currentScreen instanceof HopperScreen
					|| MC.currentScreen instanceof FurnaceScreen || MC.currentScreen instanceof BeaconScreen
					|| MC.currentScreen instanceof BlastFurnaceScreen || MC.currentScreen instanceof BrewingStandScreen
					|| MC.currentScreen instanceof GrindstoneScreen || MC.currentScreen instanceof ShulkerBoxScreen
					|| MC.currentScreen instanceof SmithingScreen || MC.currentScreen instanceof StonecutterScreen
					|| MC.currentScreen instanceof SmokerScreen || MC.currentScreen instanceof EnchantmentScreen
					|| MC.currentScreen instanceof MerchantScreen || MC.currentScreen instanceof CartographyTableScreen
					|| MC.currentScreen instanceof Generic3x3ContainerScreen
					|| MC.currentScreen instanceof HorseScreen);
		} 
}
