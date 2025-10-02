/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.Window;
import net.wurstclient.command.Command;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.TooManyHaxHack;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.WurstColors;

public final class NavigatorFeatureScreen extends NavigatorScreen
{
	private Feature feature;
	private NavigatorMainScreen parent;
	private ButtonData activeButton;
	private ButtonWidget primaryButton;
	private String text;
	private ArrayList<ButtonData> buttonDatas = new ArrayList<>();
	
	private Window window = new Window("");
	private int windowComponentY;
	
	public NavigatorFeatureScreen(Feature feature, NavigatorMainScreen parent)
	{
		this.feature = feature;
		this.parent = parent;
		hasBackground = false;
		
		for(Setting setting : feature.getSettings().values())
		{
			Component c = setting.getComponent();
			
			if(c != null)
				window.add(c);
		}
		
		window.setWidth(308);
		window.setFixedWidth(true);
		window.pack();
	}
	
	@Override
	protected void onResize()
	{
		buttonDatas.clear();
		
		// primary button
		String primaryAction = feature.getPrimaryAction();
		boolean hasPrimaryAction = !primaryAction.isEmpty();
		if(hasPrimaryAction)
		{
			primaryButton =
				ButtonWidget.builder(Text.literal(primaryAction), b -> {
					TooManyHaxHack tooManyHax =
						WurstClient.INSTANCE.getHax().tooManyHaxHack;
					if(tooManyHax.isEnabled() && tooManyHax.isBlocked(feature))
					{
						ChatUtils.error(
							feature.getName() + " is blocked by TooManyHax.");
						return;
					}
					
					feature.doPrimaryAction();
					
					primaryButton
						.setMessage(Text.literal(feature.getPrimaryAction()));
					WurstClient.INSTANCE.getNavigator()
						.addPreference(feature.getName());
				}).dimensions(width / 2 - 151, height - 65, 302, 18).build();
			addDrawableChild(primaryButton);
		}
		
		// type
		text = "Type: ";
		if(feature instanceof Hack)
			text += "Hack";
		else if(feature instanceof Command)
			text += "Command";
		else
			text += "Other Feature";
		
		// category
		if(feature.getCategory() != null)
			text += ", Category: " + feature.getCategory().getName();
		
		// description
		String description = feature.getWrappedDescription(300);
		if(!description.isEmpty())
			text += "\n\nDescription:\n" + description;
		
		// area
		Rectangle area = new Rectangle(middleX - 154, 60, 308, height - 103);
		
		// settings
		Collection<Setting> settings = feature.getSettings().values();
		if(!settings.isEmpty())
		{
			text += "\n\nSettings:";
			windowComponentY = getStringHeight(text) + 2;
			
			for(int i = 0; i < Math.ceil(window.getInnerHeight() / 9.0); i++)
				text += "\n";
		}
		
		// keybinds
		Set<PossibleKeybind> possibleKeybinds = feature.getPossibleKeybinds();
		if(!possibleKeybinds.isEmpty())
		{
			// heading
			text += "\n\nKeybinds:";
			
			// add keybind button
			ButtonData addKeybindButton =
				new ButtonData(area.x + area.width - 16,
					area.y + getStringHeight(text) - 7, 12, 8, "+", 0x00ff00)
				{
					@Override
					public void press()
					{
						// add keybind
						WurstClient.MC.setScreen(new NavigatorNewKeybindScreen(
							possibleKeybinds, NavigatorFeatureScreen.this));
					}
				};
			buttonDatas.add(addKeybindButton);
			
			// keybind list
			HashMap<String, String> possibleKeybindsMap = new HashMap<>();
			for(PossibleKeybind possibleKeybind : possibleKeybinds)
				possibleKeybindsMap.put(possibleKeybind.getCommand(),
					possibleKeybind.getDescription());
			TreeMap<String, PossibleKeybind> existingKeybinds = new TreeMap<>();
			boolean noKeybindsSet = true;
			for(Keybind keybind : WurstClient.INSTANCE.getKeybinds()
				.getAllKeybinds())
			{
				String commands = keybind.getCommands();
				commands = commands.replace(";", "\u00a7")
					.replace("\u00a7\u00a7", ";");
				for(String command : commands.split("\u00a7"))
				{
					command = command.trim();
					String keybindDescription =
						possibleKeybindsMap.get(command);
					
					if(keybindDescription != null)
					{
						if(noKeybindsSet)
							noKeybindsSet = false;
						text +=
							"\n" + keybind.getKey().replace("key.keyboard.", "")
								+ ": " + keybindDescription;
						existingKeybinds.put(keybind.getKey(),
							new PossibleKeybind(command, keybindDescription));
						
					}else if(feature instanceof Hack
						&& command.equalsIgnoreCase(feature.getName()))
					{
						if(noKeybindsSet)
							noKeybindsSet = false;
						text +=
							"\n" + keybind.getKey().replace("key.keyboard.", "")
								+ ": " + "Toggle " + feature.getName();
						existingKeybinds.put(keybind.getKey(),
							new PossibleKeybind(command,
								"Toggle " + feature.getName()));
					}
				}
			}
			if(noKeybindsSet)
				text += "\nNone";
			else
			{
				// remove keybind button
				buttonDatas.add(new ButtonData(addKeybindButton.x,
					addKeybindButton.y, addKeybindButton.width,
					addKeybindButton.height, "-", 0xff0000)
				{
					@Override
					public void press()
					{
						// remove keybind
						client.setScreen(new NavigatorRemoveKeybindScreen(
							existingKeybinds, NavigatorFeatureScreen.this));
					}
				});
				addKeybindButton.x -= 16;
			}
		}
		
		// text height
		setContentHeight(getStringHeight(text));
	}
	
	@Override
	protected void onKeyPress(KeyInput context)
	{
		int keyCode = context.key();
		
		if(keyCode == GLFW.GLFW_KEY_ESCAPE
			|| keyCode == GLFW.GLFW_KEY_BACKSPACE)
			goBack();
	}
	
	@Override
	protected void onMouseClick(Click context)
	{
		double x = context.x();
		double y = context.y();
		int button = context.button();
		
		// popups
		if(WurstClient.INSTANCE.getGui().handleNavigatorPopupClick(x, y,
			button))
			return;
		
		// back button
		if(button == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			goBack();
			return;
		}
		
		boolean noButtons = Screens.getButtons(this).isEmpty();
		Rectangle area = new Rectangle(width / 2 - 154, 60, 308,
			height - 60 - (noButtons ? 43 : 67));
		if(!area.contains(x, y))
			return;
		
		// buttons
		if(activeButton != null)
		{
			client.getSoundManager().play(
				PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1));
			activeButton.press();
			WurstClient.INSTANCE.getNavigator()
				.addPreference(feature.getName());
			return;
		}
		
		// component settings
		WurstClient.INSTANCE.getGui().handleNavigatorMouseClick(
			x - middleX + 154, y - 60 - scroll - windowComponentY, button,
			window, context);
	}
	
	private void goBack()
	{
		parent.setExpanding(false);
		client.setScreen(parent);
	}
	
	@Override
	protected void onMouseDrag(double mouseX, double mouseY, int button,
		double double_3, double double_4)
	{
		
	}
	
	@Override
	protected void onMouseRelease(double x, double y, int button)
	{
		WurstClient.INSTANCE.getGui().handleMouseRelease(x, y, button);
	}
	
	@Override
	protected void onUpdate()
	{
		if(primaryButton != null)
			primaryButton.setMessage(Text.literal(feature.getPrimaryAction()));
	}
	
	@Override
	protected void onRender(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		Matrix3x2fStack matrixStack = context.getMatrices();
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		// title bar
		context.drawCenteredTextWithShadow(client.textRenderer,
			feature.getName(), middleX, 32, txtColor);
		
		// background
		int bgx1 = middleX - 154;
		window.setX(bgx1);
		int bgx2 = middleX + 154;
		int bgy1 = 60;
		int bgy2 = height - 43;
		boolean noButtons = Screens.getButtons(this).isEmpty();
		int bgy3 = bgy2 - (noButtons ? 0 : 24);
		int windowY1 = bgy1 + scroll + windowComponentY;
		int windowY2 = windowY1 + window.getInnerHeight();
		
		context.fill(bgx1, bgy1, bgx2, MathHelper.clamp(windowY1, bgy1, bgy3),
			getBackgroundColor());
		context.fill(bgx1, MathHelper.clamp(windowY2, bgy1, bgy3), bgx2, bgy2,
			getBackgroundColor());
		RenderUtils.drawBoxShadow2D(context, bgx1, bgy1, bgx2, bgy2);
		
		context.enableScissor(bgx1, bgy1, bgx2, bgy3);
		
		// settings
		gui.setTooltip("");
		window.validate();
		
		window.setY(windowY1 - 13);
		matrixStack.pushMatrix();
		matrixStack.translate(bgx1, windowY1);
		
		{
			int x1 = 0;
			int y1 = -13;
			int x2 = x1 + window.getWidth();
			int y2 = y1 + window.getHeight();
			int y3 = y1 + 13;
			int x3 = x1 + 2;
			int x5 = x2 - 2;
			
			// window background
			// left & right
			int bgColor = getBackgroundColor();
			context.fill(x1, y3, x3, y2, bgColor);
			context.fill(x5, y3, x2, y2, bgColor);
			
			// window background
			// between children
			int xc1 = 2;
			int xc2 = x5 - x1;
			for(int i = 0; i < window.countChildren(); i++)
			{
				int yc1 = window.getChild(i).getY();
				int yc2 = yc1 - 2;
				if(yc1 < bgy1 - windowY1)
					continue;
				if(yc2 > bgy3 - windowY1)
					break;
				
				context.fill(xc1, yc1, xc2, yc2, bgColor);
			}
			
			// window background
			// bottom
			int yc1;
			if(window.countChildren() == 0)
				yc1 = 0;
			else
			{
				Component lastChild =
					window.getChild(window.countChildren() - 1);
				yc1 = lastChild.getY() + lastChild.getHeight();
			}
			int yc2 = yc1 + 2;
			context.fill(xc1, yc1, xc2, yc2, bgColor);
		}
		
		for(int i = 0; i < window.countChildren(); i++)
		{
			Component child = window.getChild(i);
			if(child.getY() + child.getHeight() < bgy1 - windowY1)
				continue;
			if(child.getY() > bgy3 - windowY1)
				break;
			
			child.render(context, mouseX - bgx1, mouseY - windowY1,
				partialTicks);
		}
		matrixStack.popMatrix();
		
		// buttons
		activeButton = null;
		for(ButtonData buttonData : buttonDatas)
		{
			// positions
			int x1 = buttonData.x;
			int x2 = x1 + buttonData.width;
			int y1 = buttonData.y + scroll;
			int y2 = y1 + buttonData.height;
			
			// color
			float alpha;
			if(buttonData.isLocked())
				alpha = 0.25F;
			else if(mouseX >= x1 && mouseX <= x2 && mouseY >= y1
				&& mouseY <= y2)
			{
				alpha = 0.75F;
				activeButton = buttonData;
			}else
				alpha = 0.375F;
			float[] rgb = buttonData.color.getColorComponents(null);
			
			// button
			drawBox(context, x1, y1, x2, y2,
				RenderUtils.toIntColor(rgb, alpha));
			
			// text
			context.state.goUpLayer();
			context.drawCenteredTextWithShadow(client.textRenderer,
				buttonData.buttonText, (x1 + x2) / 2,
				y1 + (buttonData.height - 10) / 2 + 1, buttonData.isLocked()
					? WurstColors.VERY_LIGHT_GRAY : buttonData.textColor);
		}
		
		// text
		int textY = bgy1 + scroll + 2;
		context.state.goUpLayer();
		for(String line : text.split("\n"))
		{
			context.drawText(client.textRenderer, line, bgx1 + 2, textY,
				txtColor, false);
			textY += client.textRenderer.fontHeight;
		}
		
		context.disableScissor();
		
		// buttons below scissor box
		for(ClickableWidget button : Screens.getButtons(this))
		{
			// positions
			int x1 = button.getX();
			int x2 = x1 + button.getWidth();
			int y1 = button.getY();
			int y2 = y1 + 18;
			
			// color
			boolean hovering =
				mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
			int buttonColor;
			if(feature.isEnabled() && button == primaryButton)
				buttonColor = hovering ? 0x4000FF00 : 0x4000E000;
			else
				buttonColor = hovering ? 0x40606060 : 0x40404040;
			
			// button
			drawBox(context, x1, y1, x2, y2, buttonColor);
			
			// text
			String buttonText = button.getMessage().getString();
			context.state.goUpLayer();
			context.drawText(client.textRenderer, buttonText,
				(x1 + x2 - client.textRenderer.getWidth(buttonText)) / 2,
				y1 + 5, txtColor, false);
		}
		
		// popups & tooltip
		gui.renderPopups(context, mouseX, mouseY);
		gui.renderTooltip(context, mouseX, mouseY);
	}
	
	@Override
	public void close()
	{
		window.close();
		WurstClient.INSTANCE.getGui().handleMouseClick(new Click(
			Double.MIN_VALUE, Double.MIN_VALUE, new MouseInput(0, 0)));
	}
	
	public Feature getFeature()
	{
		return feature;
	}
	
	public int getMiddleX()
	{
		return middleX;
	}
	
	public void addText(String text)
	{
		this.text += text;
	}
	
	public int getTextHeight()
	{
		return getStringHeight(text);
	}
	
	public abstract static class ButtonData extends Rectangle
	{
		public String buttonText;
		public Color color;
		public int textColor = Colors.WHITE;
		
		public ButtonData(int x, int y, int width, int height,
			String buttonText, int color)
		{
			super(x, y, width, height);
			this.buttonText = buttonText;
			this.color = new Color(color);
		}
		
		public abstract void press();
		
		public boolean isLocked()
		{
			return false;
		}
	}
}
