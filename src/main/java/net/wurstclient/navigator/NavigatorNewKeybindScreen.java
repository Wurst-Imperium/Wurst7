/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import java.util.Set;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.RenderUtils;

public class NavigatorNewKeybindScreen extends NavigatorScreen
{
	private Set<PossibleKeybind> possibleKeybinds;
	private NavigatorFeatureScreen parent;
	private PossibleKeybind hoveredCommand;
	private PossibleKeybind selectedCommand;
	private String selectedKey = "key.keyboard.unknown";
	private String text = "";
	private ButtonWidget okButton;
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
		okButton = new ButtonWidget(width / 2 - 151, height - 65, 149, 18,
			Text.literal("OK"), b -> {
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
					WurstClient.MC.setScreen(parent);
				}else
				{
					choosingKey = true;
					okButton.active = false;
				}
			}, Supplier::get)
		{
			@Override
			public boolean keyPressed(int keyCode, int scanCode, int modifiers)
			{
				// empty method so that pressing Enter won't trigger this button
				return false;
			}
		};
		okButton.active = selectedCommand != null;
		addDrawableChild(okButton);
		
		// cancel button
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Cancel"),
				b -> WurstClient.MC.setScreen(parent))
			.dimensions(width / 2 + 2, height - 65, 149, 18).build());
	}
	
	@Override
	protected void onKeyPress(int keyCode, int scanCode, int int_3)
	{
		if(choosingKey)
		{
			selectedKey =
				InputUtil.fromKeyCode(keyCode, scanCode).getTranslationKey();
			okButton.active = !selectedKey.equals("key.keyboard.unknown");
			
		}else if(keyCode == GLFW.GLFW_KEY_ESCAPE
			|| keyCode == GLFW.GLFW_KEY_BACKSPACE)
			client.setScreen(parent);
	}
	
	@Override
	protected void onMouseClick(double x, double y, int button)
	{
		// back button
		if(button == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			WurstClient.MC.setScreen(parent);
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
	protected void onRender(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		// title bar
		context.drawCenteredTextWithShadow(client.textRenderer, "New Keybind",
			middleX, 32, txtColor);
		GL11.glEnable(GL11.GL_BLEND);
		
		// background
		int bgx1 = middleX - 154;
		int bgx2 = middleX + 154;
		int bgy1 = 60;
		int bgy2 = height - 43;
		boolean noButtons = Screens.getButtons(this).isEmpty();
		int bgy3 = bgy2 - (noButtons ? 0 : 24);
		
		// scissor box
		RenderUtils.scissorBox(bgx1, bgy1, bgx2, bgy3);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		
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
				if(mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2
					&& mouseY <= bgy2 - 24)
				{
					hoveredCommand = pkb;
					if(pkb == selectedCommand)
						RenderSystem.setShaderColor(0F, 1F, 0F, 0.375F);
					else
						RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F,
							0.375F);
				}else if(pkb == selectedCommand)
					RenderSystem.setShaderColor(0F, 1F, 0F, 0.25F);
				else
					RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 0.25F);
				
				// button
				drawBox(matrixStack, x1, y1, x2, y2);
				
				// text
				context.drawTextWithShadow(client.textRenderer,
					pkb.getDescription(), x1 + 1, y1 + 1, txtColor);
				context.drawTextWithShadow(client.textRenderer,
					pkb.getCommand(), x1 + 1,
					y1 + 1 + client.textRenderer.fontHeight, txtColor);
				GL11.glEnable(GL11.GL_BLEND);
			}
		}
		
		// text
		int textY = bgy1 + scroll + 2;
		for(String line : text.split("\n"))
		{
			context.drawTextWithShadow(client.textRenderer, line, bgx1 + 2,
				textY, txtColor);
			textY += client.textRenderer.fontHeight;
		}
		GL11.glEnable(GL11.GL_BLEND);
		
		// scissor box
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		
		// buttons below scissor box
		for(ClickableWidget button : Screens.getButtons(this))
		{
			// positions
			int x1 = button.getX();
			int x2 = x1 + button.getWidth();
			int y1 = button.getY();
			int y2 = y1 + 18;
			
			// color
			if(!button.active)
				RenderSystem.setShaderColor(0F, 0F, 0F, 0.25F);
			else if(mouseX >= x1 && mouseX <= x2 && mouseY >= y1
				&& mouseY <= y2)
				RenderSystem.setShaderColor(0.375F, 0.375F, 0.375F, 0.25F);
			else
				RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 0.25F);
			
			// button
			drawBox(matrixStack, x1, y1, x2, y2);
			
			// text
			context.drawCenteredTextWithShadow(client.textRenderer,
				button.getMessage().getString(), (x1 + x2) / 2, y1 + 5,
				txtColor);
			GL11.glEnable(GL11.GL_BLEND);
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
