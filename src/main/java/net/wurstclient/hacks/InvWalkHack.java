/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemGroup;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.clickgui.screens.ClickGuiScreen;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"inv walk", "inventory walk", "InvMove", "inv move",
	"inventory move", "MenuWalk", "menu walk"})
public final class InvWalkHack extends Hack implements UpdateListener
{
	private final CheckboxSetting allowClickGUI =
		new CheckboxSetting("Allow ClickGUI",
			"description.wurst.setting.invwalk.allow_clickgui", true);
	
	private final CheckboxSetting allowOther =
		new CheckboxSetting("Allow other screens",
			"description.wurst.setting.invwalk.allow_other", true);
	
	public InvWalkHack()
	{
		super("InvWalk");
		setCategory(Category.MOVEMENT);
		addSetting(allowClickGUI);
		addSetting(allowOther);
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
		Screen screen = MC.currentScreen;
		if(screen == null)
			return;
		
		if(!isAllowedScreen(screen))
			return;
		
		KeyBinding[] keys = {MC.options.keyForward, MC.options.keyBack,
			MC.options.keyLeft, MC.options.keyRight, MC.options.keyJump,
			MC.options.keySprint, MC.options.keySneak};
		
		for(KeyBinding key : keys)
			key.setPressed(((IKeyBinding)key).isActallyPressed());
	}
	
	private boolean isAllowedScreen(Screen screen)
	{
		if(screen instanceof AbstractInventoryScreen
			&& !isCreativeSearchBarOpen(screen))
			return true;
		
		if(allowClickGUI.isChecked() && screen instanceof ClickGuiScreen)
			return true;
		
		if(allowOther.isChecked() && screen instanceof HandledScreen
			&& !hasTextBox(screen))
			return true;
		
		return false;
	}
	
	private boolean isCreativeSearchBarOpen(Screen screen)
	{
		if(!(screen instanceof CreativeInventoryScreen))
			return false;
		
		CreativeInventoryScreen crInvScreen = (CreativeInventoryScreen)screen;
		return crInvScreen.getSelectedTab() == ItemGroup.SEARCH.getIndex();
	}
	
	private boolean hasTextBox(Screen screen)
	{
		return screen.children().stream()
			.anyMatch(e -> e instanceof TextFieldWidget);
	}
}
