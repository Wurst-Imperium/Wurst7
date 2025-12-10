/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import java.util.Set;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.keybinds.PossibleKeybind;

public class NavigatorNewKeybindScreen extends NavigatorScreen
{
	private Set<PossibleKeybind> possibleKeybinds;
	private NavigatorFeatureScreen parent;
	private PossibleKeybind hoveredCommand;
	private PossibleKeybind selectedCommand;
	private String selectedKey = "key.keyboard.unknown";
	private String text = "";
	private Button okButton;
	private boolean choosingKey;
	
	public NavigatorNewKeybindScreen(Set<PossibleKeybind> possibleKeybinds,
		NavigatorFeatureScreen parent)
	{
		this.possibleKeybinds = possibleKeybinds;
		this.parent = parent;
	}
	
	@Override
	protected void onResize()
	{
		// OK button
		okButton = new Button(width / 2 - 151, height - 65, 149, 18,
			Component.literal("OK"), b -> {
				if(choosingKey)
				{
					String newCommands = selectedCommand.getCommand();
					
					String oldCommands = WurstClient.INSTANCE.getKeybinds()
						.getCommands(selectedKey);
					if(oldCommands != null)
						newCommands = oldCommands + " ; " + newCommands;
					
					WurstClient.INSTANCE.getKeybinds().add(selectedKey,
						newCommands);
					
					WurstClient.INSTANCE.getNavigator()
						.addPreference(parent.getFeature().getName());
					minecraft.setScreen(parent);
				}else
				{
					choosingKey = true;
					okButton.active = false;
				}
			}, Supplier::get)
		{
			@Override
			public boolean keyPressed(KeyEvent context)
			{
				// empty method so that pressing Enter won't trigger this button
				return false;
			}
			
			@Override
			protected void renderContents(GuiGraphics drawContext, int i, int j,
				float f)
			{
				renderDefaultSprite(drawContext);
				renderDefaultLabel(drawContext.textRendererForWidget(this,
					GuiGraphics.HoveredTextEffects.NONE));
			}
		};
		okButton.active = selectedCommand != null;
		addRenderableWidget(okButton);
		
		// cancel button
		addRenderableWidget(Button
			.builder(Component.literal("Cancel"),
				b -> WurstClient.MC.setScreen(parent))
			.bounds(width / 2 + 2, height - 65, 149, 18).build());
	}
	
	@Override
	protected void onKeyPress(KeyEvent context)
	{
		if(choosingKey)
		{
			selectedKey = InputConstants.getKey(context).getName();
			okButton.active = !selectedKey.equals("key.keyboard.unknown");
			
		}else if(context.key() == GLFW.GLFW_KEY_ESCAPE
			|| context.key() == GLFW.GLFW_KEY_BACKSPACE)
			minecraft.setScreen(parent);
	}
	
	@Override
	protected void onMouseClick(MouseButtonEvent context)
	{
		int button = context.button();
		
		// back button
		if(button == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			minecraft.setScreen(parent);
			return;
		}
		
		// commands
		if(hoveredCommand != null)
		{
			selectedCommand = hoveredCommand;
			okButton.active = true;
		}
	}
	
	@Override
	protected void onUpdate()
	{
		// text
		if(choosingKey)
		{
			text = "Now press the key that should trigger this keybind.";
			if(!selectedKey.equals("key.keyboard.unknown"))
			{
				text += "\n\nKey: " + selectedKey.replace("key.keyboard.", "");
				String commands =
					WurstClient.INSTANCE.getKeybinds().getCommands(selectedKey);
				if(commands != null)
				{
					text +=
						"\n\nWARNING: This key is already bound to the following\ncommand(s):";
					commands = commands.replace(";", "\u00a7")
						.replace("\u00a7\u00a7", ";");
					
					for(String cmd : commands.split("\u00a7"))
						text += "\n- " + cmd;
				}
			}
		}else
			text = "Select what this keybind should do.";
		
		// content height
		if(choosingKey)
			setContentHeight(getStringHeight(text));
		else
			setContentHeight(possibleKeybinds.size() * 24 - 10);
	}
	
	@Override
	protected void onRender(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		Font tr = minecraft.font;
		int txtColor = gui.getTxtColor();
		
		// title bar
		context.drawCenteredString(tr, "New Keybind", middleX, 32, txtColor);
		
		// background
		int bgx1 = middleX - 154;
		int bgx2 = middleX + 154;
		int bgy1 = 60;
		int bgy2 = height - 43;
		boolean noButtons = Screens.getButtons(this).isEmpty();
		int bgy3 = bgy2 - (noButtons ? 0 : 24);
		
		context.enableScissor(bgx1, bgy1, bgx2, bgy3);
		
		// possible keybinds
		if(!choosingKey)
		{
			hoveredCommand = null;
			int yi = bgy1 - 12 + scroll;
			for(PossibleKeybind pkb : possibleKeybinds)
			{
				yi += 24;
				
				// positions
				int x1 = bgx1 + 2;
				int x2 = bgx2 - 2;
				int y1 = yi;
				int y2 = y1 + 20;
				
				// color
				int buttonColor;
				if(mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2
					&& mouseY <= bgy2 - 24)
				{
					hoveredCommand = pkb;
					if(pkb == selectedCommand)
						buttonColor = 0x6000FF00;
					else
						buttonColor = 0x60404040;
				}else if(pkb == selectedCommand)
					buttonColor = 0x4000FF00;
				else
					buttonColor = 0x40404040;
				
				// button
				drawBox(context, x1, y1, x2, y2, buttonColor);
				
				// text
				context.guiRenderState.up();
				context.drawString(tr, pkb.getDescription(), x1 + 1, y1 + 1,
					txtColor);
				context.drawString(tr, pkb.getCommand(), x1 + 1,
					y1 + 1 + tr.lineHeight, txtColor);
			}
		}
		
		// text
		int textY = bgy1 + scroll + 2;
		context.guiRenderState.up();
		for(String line : text.split("\n"))
		{
			context.drawString(tr, line, bgx1 + 2, textY, txtColor);
			textY += tr.lineHeight;
		}
		
		context.disableScissor();
		
		// buttons below scissor box
		for(AbstractWidget button : Screens.getButtons(this))
		{
			// positions
			int x1 = button.getX();
			int x2 = x1 + button.getWidth();
			int y1 = button.getY();
			int y2 = y1 + 18;
			
			// color
			int buttonColor;
			if(!button.active)
				buttonColor = 0x40000000;
			else if(mouseX >= x1 && mouseX <= x2 && mouseY >= y1
				&& mouseY <= y2)
				buttonColor = 0x40606060;
			else
				buttonColor = 0x40404040;
			
			// button
			drawBox(context, x1, y1, x2, y2, buttonColor);
			
			// text
			context.guiRenderState.up();
			context.drawCenteredString(tr, button.getMessage().getString(),
				(x1 + x2) / 2, y1 + 5, txtColor);
		}
	}
	
	@Override
	protected void onMouseDrag(double mouseX, double mouseY, int button,
		double double_3, double double_4)
	{
		
	}
	
	@Override
	protected void onMouseRelease(double x, double y, int button)
	{
		
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
}
