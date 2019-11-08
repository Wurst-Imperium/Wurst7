/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.Window;
import net.wurstclient.command.Command;
import net.wurstclient.hack.Hack;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.RenderUtils;

public final class NavigatorFeatureScreen extends NavigatorScreen
{
	private Feature feature;
	private NavigatorMainScreen parent;
	private ButtonData activeButton;
	private ButtonWidget primaryButton;
	private String text;
	private ArrayList<ButtonData> buttonDatas = new ArrayList<>();
	
	private Window window = new Window("");
	
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
		
		window.pack();
		window.setWidth(308);
	}
	
	@Override
	protected void onResize()
	{
		buttonDatas.clear();
		
		// primary button
		String primaryAction = feature.getPrimaryAction();
		boolean hasPrimaryAction = !primaryAction.isEmpty();
		boolean hasHelp = false;// !feature.getHelpPage().isEmpty();
		if(hasPrimaryAction)
		{
			primaryButton = new ButtonWidget(width / 2 - 151, height - 65,
				hasHelp ? 149 : 302, 18, primaryAction, b -> {
					feature.doPrimaryAction();
					primaryButton.setMessage(feature.getPrimaryAction());
					WurstClient.INSTANCE.getNavigator()
						.addPreference(feature.getName());
				});
			addButton(primaryButton);
		}
		
		// help button
		// if(hasHelp)
		// addButton(new ButtonWidget(
		// width / 2 + (hasPrimaryAction ? 2 : -151), height - 65,
		// hasPrimaryAction ? 149 : 302, 20, "Help", b -> {
		// MiscUtils.openLink("https://www.wurstclient.net/wiki/"
		// + feature.getHelpPage() + "/");
		// wurst.navigator.analytics.trackEvent("help", "open",
		// feature.getName());
		// wurst.navigator.addPreference(feature.getName());
		// ConfigFiles.NAVIGATOR.save();
		// }));
		
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
		String description = feature.getDescription();
		if(!description.isEmpty())
			text += "\n\nDescription:\n" + description;
		
		// area
		Rectangle area = new Rectangle(middleX - 154, 60, 308, height - 103);
		
		// settings
		Collection<Setting> settings = feature.getSettings().values();
		if(!settings.isEmpty())
		{
			text += "\n\nSettings:";
			window.setY(getStringHeight(text) + 2);
			
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
						WurstClient.MC.openScreen(new NavigatorNewKeybindScreen(
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
						minecraft.openScreen(new NavigatorRemoveKeybindScreen(
							existingKeybinds, NavigatorFeatureScreen.this));
					}
				});
				addKeybindButton.x -= 16;
			}
		}
		
		// see also
		// Feature[] seeAlso = feature.getSeeAlso();
		// if(seeAlso.length != 0)
		// {
		// text += "\n\nSee also:";
		// for(Feature seeAlsoFeature : seeAlso)
		// {
		// int y = 60 + getTextHeight() + 2;
		// String name = seeAlsoFeature.getName();
		// text += "\n- " + name;
		// buttonDatas.add(new ButtonData(middleX - 148, y,
		// Fonts.segoe15.getStringWidth(name) + 1, 8, "", 0x404040)
		// {
		// @Override
		// public void press()
		// {
		// mc.displayGuiScreen(
		// new NavigatorFeatureScreen(seeAlsoFeature, parent));
		// }
		// });
		// }
		// }
		
		// text height
		setContentHeight(getStringHeight(text));
	}
	
	@Override
	protected void onKeyPress(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_ESCAPE)
		{
			parent.setExpanding(false);
			minecraft.openScreen(parent);
		}
	}
	
	@Override
	protected void onMouseClick(double x, double y, int button)
	{
		// popups
		if(WurstClient.INSTANCE.getGui().handleNavigatorPopupClick(
			x - middleX + 154, y - 60 - scroll + 13, button))
			return;
		
		Rectangle area = new Rectangle(width / 2 - 154, 60, 308, height - 103);
		if(!area.contains(x, y))
			return;
		
		// buttons
		if(activeButton != null)
		{
			minecraft.getSoundManager().play(
				PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1));
			activeButton.press();
			WurstClient.INSTANCE.getNavigator()
				.addPreference(feature.getName());
			return;
		}
		
		// component settings
		WurstClient.INSTANCE.getGui().handleNavigatorMouseClick(
			x - middleX + 154, y - 60 - scroll - window.getY(), button, window);
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
		
	}
	
	@Override
	protected void onRender(int mouseX, int mouseY, float partialTicks)
	{
		// title bar
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		drawCenteredString(minecraft.textRenderer, feature.getName(), middleX,
			32, 0xffffff);
		glDisable(GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		
		// background
		int bgx1 = middleX - 154;
		int bgx2 = middleX + 154;
		int bgy1 = 60;
		int bgy2 = height - 43;
		
		setColorToBackground();
		drawQuads(bgx1, bgy1, bgx2,
			Math.max(bgy1, Math.min(bgy2 - (buttons.isEmpty() ? 0 : 24),
				bgy1 + scroll + window.getY())));
		drawQuads(bgx1,
			Math.max(bgy1,
				Math.min(bgy2 - (buttons.isEmpty() ? 0 : 24),
					bgy1 + scroll + window.getY() + window.getInnerHeight())),
			bgx2, bgy2);
		drawBoxShadow(bgx1, bgy1, bgx2, bgy2);
		
		// scissor box
		RenderUtils.scissorBox(bgx1, bgy1, bgx2,
			bgy2 - (buttons.isEmpty() ? 0 : 24));
		glEnable(GL_SCISSOR_TEST);
		
		// settings
		WurstClient.INSTANCE.getGui().setTooltip("");
		window.validate();
		
		int windowY = bgy1 + scroll + window.getY();
		GL11.glPushMatrix();
		GL11.glTranslated(bgx1, windowY, 0);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		
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
			setColorToBackground();
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2i(x1, y3);
			GL11.glVertex2i(x1, y2);
			GL11.glVertex2i(x3, y2);
			GL11.glVertex2i(x3, y3);
			GL11.glVertex2i(x5, y3);
			GL11.glVertex2i(x5, y2);
			GL11.glVertex2i(x2, y2);
			GL11.glVertex2i(x2, y3);
			GL11.glEnd();
			
			setColorToBackground();
			GL11.glBegin(GL11.GL_QUADS);
			
			// window background
			// between children
			int xc1 = 2;
			int xc2 = x5 - x1;
			for(int i = 0; i < window.countChildren(); i++)
			{
				int yc1 = window.getChild(i).getY();
				int yc2 = yc1 - 2;
				GL11.glVertex2i(xc1, yc2);
				GL11.glVertex2i(xc1, yc1);
				GL11.glVertex2i(xc2, yc1);
				GL11.glVertex2i(xc2, yc2);
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
			GL11.glVertex2i(xc1, yc2);
			GL11.glVertex2i(xc1, yc1);
			GL11.glVertex2i(xc2, yc1);
			GL11.glVertex2i(xc2, yc2);
			
			GL11.glEnd();
		}
		
		for(int i = 0; i < window.countChildren(); i++)
			window.getChild(i).render(mouseX - bgx1, mouseY - windowY,
				partialTicks);
		GL11.glPopMatrix();
		
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
			glColor4f(rgb[0], rgb[1], rgb[2], alpha);
			
			// button
			drawBox(x1, y1, x2, y2);
			
			// text
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			drawCenteredString(minecraft.textRenderer, buttonData.buttonText,
				(x1 + x2) / 2, y1 + (buttonData.height - 10) / 2 + 1,
				buttonData.isLocked() ? 0xaaaaaa : buttonData.textColor);
			glDisable(GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
		}
		
		// text
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		int textY = bgy1 + scroll + 2;
		for(String line : text.split("\n"))
		{
			drawString(minecraft.textRenderer, line, bgx1 + 2, textY, 0xffffff);
			textY += minecraft.textRenderer.fontHeight;
		}
		GL11.glEnable(GL11.GL_BLEND);
		
		// scissor box
		glDisable(GL_SCISSOR_TEST);
		
		GL11.glPushMatrix();
		GL11.glTranslated(bgx1, bgy1 + scroll - 13, 0);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		WurstClient.INSTANCE.getGui().renderPopupsAndTooltip(mouseX - bgx1,
			mouseY - bgy1 - scroll + 13);
		GL11.glPopMatrix();
		
		// buttons below scissor box
		for(AbstractButtonWidget button : buttons)
		{
			// positions
			int x1 = button.x;
			int x2 = x1 + button.getWidth();
			int y1 = button.y;
			int y2 = y1 + 18;
			
			// color
			boolean hovering =
				mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
			if(feature.isEnabled() && button == primaryButton)
				// if(feature.isBlocked())
				// glColor4f(hovering ? 1F : 0.875F, 0F, 0F, 0.25F);
				// else
				glColor4f(0F, hovering ? 1F : 0.875F, 0F, 0.25F);
			else if(hovering)
				glColor4f(0.375F, 0.375F, 0.375F, 0.25F);
			else
				glColor4f(0.25F, 0.25F, 0.25F, 0.25F);
			
			// button
			glDisable(GL_TEXTURE_2D);
			drawBox(x1, y1, x2, y2);
			
			// text
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			drawCenteredString(minecraft.textRenderer, button.getMessage(),
				(x1 + x2) / 2, y1 + 4, 0xffffff);
			GL11.glEnable(GL11.GL_BLEND);
		}
		
		// GL resets
		glEnable(GL_CULL_FACE);
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
	}
	
	@Override
	public void onClose()
	{
		window.close();
		WurstClient.INSTANCE.getGui().handleMouseClick(Integer.MIN_VALUE,
			Integer.MIN_VALUE, 0);
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
	
	public abstract class ButtonData extends Rectangle
	{
		public String buttonText;
		public Color color;
		public int textColor = 0xffffff;
		
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
