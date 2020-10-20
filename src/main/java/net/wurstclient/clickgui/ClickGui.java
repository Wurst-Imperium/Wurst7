/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.wurstclient.Category;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.components.FeatureButton;
import net.wurstclient.hacks.ClickGuiHack;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.json.JsonUtils;

public final class ClickGui
{
	private static final WurstClient WURST = WurstClient.INSTANCE;
	private static final MinecraftClient MC = WurstClient.MC;
	
	private final ArrayList<Window> windows = new ArrayList<>();
	private final ArrayList<Popup> popups = new ArrayList<>();
	private final Path windowsFile;
	
	private float[] bgColor = new float[3];
	private float[] acColor = new float[3];
	private float opacity;
	
	private String tooltip = "";
	
	private boolean leftMouseButtonPressed;
	
	public ClickGui(Path windowsFile)
	{
		this.windowsFile = windowsFile;
	}
	
	public void init()
	{
		updateColors();
		
		LinkedHashMap<Category, Window> windowMap = new LinkedHashMap<>();
		for(Category category : Category.values())
			windowMap.put(category, new Window(category.getName()));
		
		ArrayList<Feature> features = new ArrayList<>();
		features.addAll(WURST.getHax().getAllHax());
		features.addAll(WURST.getCmds().getAllCmds());
		features.addAll(WURST.getOtfs().getAllOtfs());
		
		for(Feature f : features)
			if(f.getCategory() != null)
				windowMap.get(f.getCategory()).add(new FeatureButton(f));
			
		windows.addAll(windowMap.values());
		
		Window uiSettings = new Window("UI Settings");
		uiSettings.add(new FeatureButton(WURST.getOtfs().wurstLogoOtf));
		uiSettings.add(new FeatureButton(WURST.getOtfs().hackListOtf));
		ClickGuiHack clickGuiHack = WURST.getHax().clickGuiHack;
		Stream<Setting> settings = clickGuiHack.getSettings().values().stream();
		settings.map(Setting::getComponent).forEach(c -> uiSettings.add(c));
		windows.add(uiSettings);
		
		for(Window window : windows)
			window.setMinimized(true);
		
		windows.add(WurstClient.INSTANCE.getHax().radarHack.getWindow());
		
		int x = 5;
		int y = 5;
		net.minecraft.client.util.Window sr = MC.getWindow();
		for(Window window : windows)
		{
			window.pack();
			
			if(x + window.getWidth() + 5 > sr.getScaledWidth())
			{
				x = 5;
				y += 18;
			}
			
			window.setX(x);
			window.setY(y);
			x += window.getWidth() + 5;
		}
		
		JsonObject json;
		try(BufferedReader reader = Files.newBufferedReader(windowsFile))
		{
			json = JsonUtils.JSON_PARSER.parse(reader).getAsJsonObject();
			
		}catch(NoSuchFileException e)
		{
			saveWindows();
			return;
			
		}catch(Exception e)
		{
			System.out.println("Failed to load " + windowsFile.getFileName());
			e.printStackTrace();
			
			saveWindows();
			return;
		}
		
		for(Window window : windows)
		{
			JsonElement jsonWindow = json.get(window.getTitle());
			if(jsonWindow == null || !jsonWindow.isJsonObject())
				continue;
			
			JsonElement jsonX = jsonWindow.getAsJsonObject().get("x");
			if(jsonX.isJsonPrimitive() && jsonX.getAsJsonPrimitive().isNumber())
				window.setX(jsonX.getAsInt());
			
			JsonElement jsonY = jsonWindow.getAsJsonObject().get("y");
			if(jsonY.isJsonPrimitive() && jsonY.getAsJsonPrimitive().isNumber())
				window.setY(jsonY.getAsInt());
			
			JsonElement jsonMinimized =
				jsonWindow.getAsJsonObject().get("minimized");
			if(jsonMinimized.isJsonPrimitive()
				&& jsonMinimized.getAsJsonPrimitive().isBoolean())
				window.setMinimized(jsonMinimized.getAsBoolean());
			
			JsonElement jsonPinned = jsonWindow.getAsJsonObject().get("pinned");
			if(jsonPinned.isJsonPrimitive()
				&& jsonPinned.getAsJsonPrimitive().isBoolean())
				window.setPinned(jsonPinned.getAsBoolean());
		}
		
		saveWindows();
	}
	
	private void saveWindows()
	{
		JsonObject json = new JsonObject();
		
		for(Window window : windows)
		{
			if(window.isClosable())
				continue;
			
			JsonObject jsonWindow = new JsonObject();
			jsonWindow.addProperty("x", window.getX());
			jsonWindow.addProperty("y", window.getY());
			jsonWindow.addProperty("minimized", window.isMinimized());
			jsonWindow.addProperty("pinned", window.isPinned());
			json.add(window.getTitle(), jsonWindow);
		}
		
		try(BufferedWriter writer = Files.newBufferedWriter(windowsFile))
		{
			JsonUtils.PRETTY_GSON.toJson(json, writer);
			
		}catch(IOException e)
		{
			System.out.println("Failed to save " + windowsFile.getFileName());
			e.printStackTrace();
		}
	}
	
	public void handleMouseClick(int mouseX, int mouseY, int mouseButton)
	{
		if(mouseButton == 0)
			leftMouseButtonPressed = true;
		
		boolean popupClicked =
			handlePopupMouseClick(mouseX, mouseY, mouseButton);
		
		if(!popupClicked)
			handleWindowMouseClick(mouseX, mouseY, mouseButton);
		
		for(Popup popup : popups)
			if(popup.getOwner().getParent().isClosing())
				popup.close();
			
		windows.removeIf(w -> w.isClosing());
		popups.removeIf(p -> p.isClosing());
	}
	
	public void handleMouseRelease(double mouseX, double mouseY,
		int mouseButton)
	{
		if(mouseButton == 0)
			leftMouseButtonPressed = false;
	}
	
	public void handleMouseScroll(double mouseX, double mouseY, double delta)
	{
		int dWheel = (int)delta * 4;
		if(dWheel == 0)
			return;
		
		for(int i = windows.size() - 1; i >= 0; i--)
		{
			Window window = windows.get(i);
			
			if(!window.isScrollingEnabled() || window.isMinimized()
				|| window.isInvisible())
				continue;
			
			if(mouseX < window.getX() || mouseY < window.getY() + 13)
				continue;
			if(mouseX >= window.getX() + window.getWidth()
				|| mouseY >= window.getY() + window.getHeight())
				continue;
			
			int scroll = window.getScrollOffset() + dWheel;
			scroll = Math.min(scroll, 0);
			scroll = Math.max(scroll,
				-window.getInnerHeight() + window.getHeight() - 13);
			window.setScrollOffset(scroll);
			break;
		}
	}
	
	public boolean handleNavigatorPopupClick(double mouseX, double mouseY,
		int mouseButton)
	{
		boolean popupClicked =
			handlePopupMouseClick(mouseX, mouseY, mouseButton);
		
		if(popupClicked)
		{
			for(Popup popup : popups)
				if(popup.getOwner().getParent().isClosing())
					popup.close();
				
			popups.removeIf(p -> p.isClosing());
		}
		
		return popupClicked;
	}
	
	public void handleNavigatorMouseClick(double cMouseX, double cMouseY,
		int mouseButton, Window window)
	{
		if(mouseButton == 0)
			leftMouseButtonPressed = true;
		
		handleComponentMouseClick(window, cMouseX, cMouseY, mouseButton);
		
		for(Popup popup : popups)
			if(popup.getOwner().getParent().isClosing())
				popup.close();
			
		popups.removeIf(p -> p.isClosing());
	}
	
	private boolean handlePopupMouseClick(double mouseX, double mouseY,
		int mouseButton)
	{
		for(int i = popups.size() - 1; i >= 0; i--)
		{
			Popup popup = popups.get(i);
			Component owner = popup.getOwner();
			Window parent = owner.getParent();
			
			int x0 = parent.getX() + owner.getX();
			int y0 =
				parent.getY() + 13 + parent.getScrollOffset() + owner.getY();
			
			int x1 = x0 + popup.getX();
			int y1 = y0 + popup.getY();
			int x2 = x1 + popup.getWidth();
			int y2 = y1 + popup.getHeight();
			
			if(mouseX < x1 || mouseY < y1)
				continue;
			if(mouseX >= x2 || mouseY >= y2)
				continue;
			
			int cMouseX = (int)(mouseX - x0);
			int cMouseY = (int)(mouseY - y0);
			popup.handleMouseClick(cMouseX, cMouseY, mouseButton);
			
			popups.remove(i);
			popups.add(popup);
			return true;
		}
		
		return false;
	}
	
	private void handleWindowMouseClick(int mouseX, int mouseY, int mouseButton)
	{
		for(int i = windows.size() - 1; i >= 0; i--)
		{
			Window window = windows.get(i);
			if(window.isInvisible())
				continue;
			
			int x1 = window.getX();
			int y1 = window.getY();
			int x2 = x1 + window.getWidth();
			int y2 = y1 + window.getHeight();
			int y3 = y1 + 13;
			
			if(mouseX < x1 || mouseY < y1)
				continue;
			if(mouseX >= x2 || mouseY >= y2)
				continue;
			
			if(mouseY < y3)
				handleTitleBarMouseClick(window, mouseX, mouseY, mouseButton);
			else if(!window.isMinimized())
			{
				window.validate();
				
				int cMouseX = mouseX - x1;
				int cMouseY = mouseY - y3;
				
				if(window.isScrollingEnabled() && mouseX >= x2 - 3)
					handleScrollbarMouseClick(window, cMouseX, cMouseY,
						mouseButton);
				else
				{
					if(window.isScrollingEnabled())
						cMouseY -= window.getScrollOffset();
					
					handleComponentMouseClick(window, cMouseX, cMouseY,
						mouseButton);
				}
				
			}else
				continue;
			
			windows.remove(i);
			windows.add(window);
			break;
		}
	}
	
	private void handleTitleBarMouseClick(Window window, int mouseX, int mouseY,
		int mouseButton)
	{
		if(mouseButton != 0)
			return;
		
		if(mouseY < window.getY() + 2 || mouseY >= window.getY() + 11)
		{
			window.startDragging(mouseX, mouseY);
			return;
		}
		
		int x3 = window.getX() + window.getWidth();
		
		if(window.isClosable())
		{
			x3 -= 11;
			if(mouseX >= x3 && mouseX < x3 + 9)
			{
				window.close();
				return;
			}
		}
		
		if(window.isPinnable())
		{
			x3 -= 11;
			if(mouseX >= x3 && mouseX < x3 + 9)
			{
				window.setPinned(!window.isPinned());
				saveWindows();
				return;
			}
		}
		
		if(window.isMinimizable())
		{
			x3 -= 11;
			if(mouseX >= x3 && mouseX < x3 + 9)
			{
				window.setMinimized(!window.isMinimized());
				saveWindows();
				return;
			}
		}
		
		window.startDragging(mouseX, mouseY);
	}
	
	private void handleScrollbarMouseClick(Window window, int mouseX,
		int mouseY, int mouseButton)
	{
		if(mouseButton != 0)
			return;
		
		if(mouseX >= window.getWidth() - 1)
			return;
		
		double outerHeight = window.getHeight() - 13;
		double innerHeight = window.getInnerHeight();
		double maxScrollbarHeight = outerHeight - 2;
		int scrollbarY =
			(int)(outerHeight * (-window.getScrollOffset() / innerHeight) + 1);
		int scrollbarHeight =
			(int)(maxScrollbarHeight * outerHeight / innerHeight);
		
		if(mouseY < scrollbarY || mouseY >= scrollbarY + scrollbarHeight)
			return;
		
		window.startDraggingScrollbar(window.getY() + 13 + mouseY);
	}
	
	private void handleComponentMouseClick(Window window, double mouseX,
		double mouseY, int mouseButton)
	{
		for(int i2 = window.countChildren() - 1; i2 >= 0; i2--)
		{
			Component c = window.getChild(i2);
			
			if(mouseX < c.getX() || mouseY < c.getY())
				continue;
			if(mouseX >= c.getX() + c.getWidth()
				|| mouseY >= c.getY() + c.getHeight())
				continue;
			
			c.handleMouseClick(mouseX, mouseY, mouseButton);
			break;
		}
	}
	
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		updateColors();
		
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glLineWidth(1);
		
		tooltip = "";
		for(Window window : windows)
		{
			if(window.isInvisible())
				continue;
			
			// dragging
			if(window.isDragging())
				if(leftMouseButtonPressed)
					window.dragTo(mouseX, mouseY);
				else
				{
					window.stopDragging();
					saveWindows();
				}
			
			// scrollbar dragging
			if(window.isDraggingScrollbar())
				if(leftMouseButtonPressed)
					window.dragScrollbarTo(mouseY);
				else
					window.stopDraggingScrollbar();
				
			renderWindow(matrixStack, window, mouseX, mouseY, partialTicks);
		}
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		renderPopupsAndTooltip(matrixStack, mouseX, mouseY);
		
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	public void renderPopupsAndTooltip(MatrixStack matrixStack, int mouseX,
		int mouseY)
	{
		// popups
		for(Popup popup : popups)
		{
			Component owner = popup.getOwner();
			Window parent = owner.getParent();
			
			int x1 = parent.getX() + owner.getX();
			int y1 =
				parent.getY() + 13 + parent.getScrollOffset() + owner.getY();
			
			GL11.glPushMatrix();
			GL11.glTranslated(x1, y1, 300);
			
			int cMouseX = mouseX - x1;
			int cMouseY = mouseY - y1;
			popup.render(matrixStack, cMouseX, cMouseY);
			
			GL11.glPopMatrix();
		}
		
		// tooltip
		if(!tooltip.isEmpty())
		{
			String[] lines = tooltip.split("\n");
			TextRenderer fr = MC.textRenderer;
			
			int tw = 0;
			int th = lines.length * fr.fontHeight;
			for(String line : lines)
			{
				int lw = fr.getWidth(line);
				if(lw > tw)
					tw = lw;
			}
			int sw = MC.currentScreen.width;
			int sh = MC.currentScreen.height;
			
			int xt1 = mouseX + tw + 11 <= sw ? mouseX + 8 : mouseX - tw - 8;
			int xt2 = xt1 + tw + 3;
			int yt1 = mouseY + th - 2 <= sh ? mouseY - 4 : mouseY - th - 4;
			int yt2 = yt1 + th + 2;
			
			GL11.glPushMatrix();
			GL11.glTranslated(0, 0, 300);
			
			// background
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], 0.75F);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2i(xt1, yt1);
			GL11.glVertex2i(xt1, yt2);
			GL11.glVertex2i(xt2, yt2);
			GL11.glVertex2i(xt2, yt1);
			GL11.glEnd();
			
			// outline
			GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2i(xt1, yt1);
			GL11.glVertex2i(xt1, yt2);
			GL11.glVertex2i(xt2, yt2);
			GL11.glVertex2i(xt2, yt1);
			GL11.glEnd();
			
			// text
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			for(int i = 0; i < lines.length; i++)
				fr.draw(matrixStack, lines[i], xt1 + 2,
					yt1 + 2 + i * fr.fontHeight, 0xffffff);
			GL11.glEnable(GL11.GL_BLEND);
			
			GL11.glPopMatrix();
		}
	}
	
	public void renderPinnedWindows(MatrixStack matrixStack, float partialTicks)
	{
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glLineWidth(1);
		
		for(Window window : windows)
			if(window.isPinned() && !window.isInvisible())
				renderWindow(matrixStack, window, Integer.MIN_VALUE,
					Integer.MIN_VALUE, partialTicks);
			
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	public void updateColors()
	{
		ClickGuiHack clickGui = WURST.getHax().clickGuiHack;
		
		opacity = clickGui.getOpacity();
		bgColor = clickGui.getBgColor();
		
		if(WurstClient.INSTANCE.getHax().rainbowUiHack.isEnabled())
		{
			float x = System.currentTimeMillis() % 2000 / 1000F;
			acColor[0] = 0.5F + 0.5F * (float)Math.sin(x * Math.PI);
			acColor[1] = 0.5F + 0.5F * (float)Math.sin((x + 4F / 3F) * Math.PI);
			acColor[2] = 0.5F + 0.5F * (float)Math.sin((x + 8F / 3F) * Math.PI);
			
		}else
			acColor = clickGui.getAcColor();
	}
	
	private void renderWindow(MatrixStack matrixStack, Window window,
		int mouseX, int mouseY, float partialTicks)
	{
		int x1 = window.getX();
		int y1 = window.getY();
		int x2 = x1 + window.getWidth();
		int y2 = y1 + window.getHeight();
		int y3 = y1 + 13;
		
		if(window.isMinimized())
			y2 = y3;
		
		if(mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2)
			tooltip = "";
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		
		if(!window.isMinimized())
		{
			window.setMaxHeight(187);
			window.validate();
			
			// scrollbar
			if(window.isScrollingEnabled())
			{
				int xs1 = x2 - 3;
				int xs2 = xs1 + 2;
				int xs3 = x2;
				
				double outerHeight = y2 - y3;
				double innerHeight = window.getInnerHeight();
				double maxScrollbarHeight = outerHeight - 2;
				double scrollbarY =
					outerHeight * (-window.getScrollOffset() / innerHeight) + 1;
				double scrollbarHeight =
					maxScrollbarHeight * outerHeight / innerHeight;
				
				int ys1 = y3;
				int ys2 = y2;
				int ys3 = ys1 + (int)scrollbarY;
				int ys4 = ys3 + (int)scrollbarHeight;
				
				// window background
				GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
				GL11.glBegin(GL11.GL_QUADS);
				GL11.glVertex2i(xs2, ys1);
				GL11.glVertex2i(xs2, ys2);
				GL11.glVertex2i(xs3, ys2);
				GL11.glVertex2i(xs3, ys1);
				GL11.glVertex2i(xs1, ys1);
				GL11.glVertex2i(xs1, ys3);
				GL11.glVertex2i(xs2, ys3);
				GL11.glVertex2i(xs2, ys1);
				GL11.glVertex2i(xs1, ys4);
				GL11.glVertex2i(xs1, ys2);
				GL11.glVertex2i(xs2, ys2);
				GL11.glVertex2i(xs2, ys4);
				GL11.glEnd();
				
				boolean hovering = mouseX >= xs1 && mouseY >= ys3
					&& mouseX < xs2 && mouseY < ys4;
				
				// scrollbar
				GL11.glColor4f(acColor[0], acColor[1], acColor[2],
					hovering ? opacity * 1.5F : opacity);
				GL11.glBegin(GL11.GL_QUADS);
				GL11.glVertex2i(xs1, ys3);
				GL11.glVertex2i(xs1, ys4);
				GL11.glVertex2i(xs2, ys4);
				GL11.glVertex2i(xs2, ys3);
				GL11.glEnd();
				
				// outline
				GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
				GL11.glBegin(GL11.GL_LINE_LOOP);
				GL11.glVertex2i(xs1, ys3);
				GL11.glVertex2i(xs1, ys4);
				GL11.glVertex2i(xs2, ys4);
				GL11.glVertex2i(xs2, ys3);
				GL11.glEnd();
			}
			
			int x3 = x1 + 2;
			int x4 = window.isScrollingEnabled() ? x2 - 3 : x2;
			int x5 = x4 - 2;
			int y4 = y3 + window.getScrollOffset();
			
			// window background
			// left & right
			GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2i(x1, y3);
			GL11.glVertex2i(x1, y2);
			GL11.glVertex2i(x3, y2);
			GL11.glVertex2i(x3, y3);
			GL11.glVertex2i(x5, y3);
			GL11.glVertex2i(x5, y2);
			GL11.glVertex2i(x4, y2);
			GL11.glVertex2i(x4, y3);
			GL11.glEnd();
			
			net.minecraft.client.util.Window sr = MC.getWindow();
			int sf = (int)sr.getScaleFactor();
			GL11.glScissor(x1 * sf, (sr.getScaledHeight() - y2) * sf,
				window.getWidth() * sf, (y2 - y3) * sf);
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
			GL11.glPushMatrix();
			GL11.glTranslated(x1, y4, 0);
			
			GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
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
			
			// render children
			int cMouseX = mouseX - x1;
			int cMouseY = mouseY - y4;
			for(int i = 0; i < window.countChildren(); i++)
				window.getChild(i).render(matrixStack, cMouseX, cMouseY,
					partialTicks);
			
			GL11.glPopMatrix();
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}
		
		// window outline
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
		
		if(!window.isMinimized())
		{
			// title bar outline
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex2i(x1, y3);
			GL11.glVertex2i(x2, y3);
			GL11.glEnd();
		}
		
		// title bar buttons
		int x3 = x2;
		int y4 = y1 + 2;
		int y5 = y3 - 2;
		boolean hoveringY = mouseY >= y4 && mouseY < y5;
		
		if(window.isClosable())
		{
			x3 -= 11;
			int x4 = x3 + 9;
			boolean hovering = hoveringY && mouseX >= x3 && mouseX < x4;
			renderCloseButton(x3, y4, x4, y5, hovering);
		}
		
		if(window.isPinnable())
		{
			x3 -= 11;
			int x4 = x3 + 9;
			boolean hovering = hoveringY && mouseX >= x3 && mouseX < x4;
			renderPinButton(x3, y4, x4, y5, hovering, window.isPinned());
		}
		
		if(window.isMinimizable())
		{
			x3 -= 11;
			int x4 = x3 + 9;
			boolean hovering = hoveringY && mouseX >= x3 && mouseX < x4;
			renderMinimizeButton(x3, y4, x4, y5, hovering,
				window.isMinimized());
		}
		
		// title bar background
		// above & below buttons
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x3, y1);
		GL11.glVertex2i(x3, y4);
		GL11.glVertex2i(x2, y4);
		GL11.glVertex2i(x2, y1);
		GL11.glVertex2i(x3, y5);
		GL11.glVertex2i(x3, y3);
		GL11.glVertex2i(x2, y3);
		GL11.glVertex2i(x2, y5);
		GL11.glEnd();
		
		// title bar background
		// behind title
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y3);
		GL11.glVertex2i(x3, y3);
		GL11.glVertex2i(x3, y1);
		GL11.glEnd();
		
		// window title
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor4f(1, 1, 1, 1);
		TextRenderer fr = MC.textRenderer;
		String title =
			fr.trimToWidth(new LiteralText(window.getTitle()), x3 - x1)
				.getString();
		fr.draw(matrixStack, title, x1 + 2, y1 + 3, 0xf0f0f0);
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	private void renderTitleBarButton(int x1, int y1, int x2, int y2,
		boolean hovering)
	{
		int x3 = x2 + 2;
		
		// button background
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
			hovering ? opacity * 1.5F : opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
		
		// background between buttons
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x2, y1);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x3, y1);
		GL11.glEnd();
		
		// button outline
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
	}
	
	private void renderMinimizeButton(int x1, int y1, int x2, int y2,
		boolean hovering, boolean minimized)
	{
		renderTitleBarButton(x1, y1, x2, y2, hovering);
		
		double xa1 = x1 + 1;
		double xa2 = (x1 + x2) / 2.0;
		double xa3 = x2 - 1;
		double ya1;
		double ya2;
		
		if(minimized)
		{
			ya1 = y1 + 3;
			ya2 = y2 - 2.5;
			GL11.glColor4f(0, hovering ? 1 : 0.85F, 0, 1);
			
		}else
		{
			ya1 = y2 - 3;
			ya2 = y1 + 2.5;
			GL11.glColor4f(hovering ? 1 : 0.85F, 0, 0, 1);
		}
		
		// arrow
		GL11.glBegin(GL11.GL_TRIANGLES);
		GL11.glVertex2d(xa1, ya1);
		GL11.glVertex2d(xa3, ya1);
		GL11.glVertex2d(xa2, ya2);
		GL11.glEnd();
		
		// outline
		GL11.glColor4f(0.0625F, 0.0625F, 0.0625F, 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2d(xa1, ya1);
		GL11.glVertex2d(xa3, ya1);
		GL11.glVertex2d(xa2, ya2);
		GL11.glEnd();
	}
	
	private void renderPinButton(int x1, int y1, int x2, int y2,
		boolean hovering, boolean pinned)
	{
		renderTitleBarButton(x1, y1, x2, y2, hovering);
		float h = hovering ? 1 : 0.85F;
		
		if(pinned)
		{
			double xk1 = x1 + 2;
			double xk2 = x2 - 2;
			double xk3 = x1 + 1;
			double xk4 = x2 - 1;
			double yk1 = y1 + 2;
			double yk2 = y2 - 2;
			double yk3 = y2 - 0.5;
			
			// knob
			GL11.glColor4f(h, 0, 0, 0.5F);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2d(xk1, yk1);
			GL11.glVertex2d(xk2, yk1);
			GL11.glVertex2d(xk2, yk2);
			GL11.glVertex2d(xk1, yk2);
			GL11.glVertex2d(xk3, yk2);
			GL11.glVertex2d(xk4, yk2);
			GL11.glVertex2d(xk4, yk3);
			GL11.glVertex2d(xk3, yk3);
			GL11.glEnd();
			
			double xn1 = x1 + 3.5;
			double xn2 = x2 - 3.5;
			double yn1 = y2 - 0.5;
			double yn2 = y2;
			
			// needle
			GL11.glColor4f(h, h, h, 1);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2d(xn1, yn1);
			GL11.glVertex2d(xn2, yn1);
			GL11.glVertex2d(xn2, yn2);
			GL11.glVertex2d(xn1, yn2);
			GL11.glEnd();
			
			// outlines
			GL11.glColor4f(0.0625F, 0.0625F, 0.0625F, 0.5F);
			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2d(xk1, yk1);
			GL11.glVertex2d(xk2, yk1);
			GL11.glVertex2d(xk2, yk2);
			GL11.glVertex2d(xk1, yk2);
			GL11.glEnd();
			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2d(xk3, yk2);
			GL11.glVertex2d(xk4, yk2);
			GL11.glVertex2d(xk4, yk3);
			GL11.glVertex2d(xk3, yk3);
			GL11.glEnd();
			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2d(xn1, yn1);
			GL11.glVertex2d(xn2, yn1);
			GL11.glVertex2d(xn2, yn2);
			GL11.glVertex2d(xn1, yn2);
			GL11.glEnd();
			
		}else
		{
			double xk1 = x2 - 3.5;
			double xk2 = x2 - 0.5;
			double xk3 = x2 - 3;
			double xk4 = x1 + 3;
			double xk5 = x1 + 2;
			double xk6 = x2 - 2;
			double xk7 = x1 + 1;
			double yk1 = y1 + 0.5;
			double yk2 = y1 + 3.5;
			double yk3 = y2 - 3;
			double yk4 = y1 + 3;
			double yk5 = y1 + 2;
			double yk6 = y2 - 2;
			double yk7 = y2 - 1;
			
			// knob
			GL11.glColor4f(0, h, 0, 1);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2d(xk1, yk1);
			GL11.glVertex2d(xk2, yk2);
			GL11.glVertex2d(xk3, yk3);
			GL11.glVertex2d(xk4, yk4);
			GL11.glVertex2d(xk5, yk5);
			GL11.glVertex2d(xk6, yk6);
			GL11.glVertex2d(xk3, yk7);
			GL11.glVertex2d(xk7, yk4);
			GL11.glEnd();
			
			double xn1 = x1 + 3;
			double xn2 = x1 + 4;
			double xn3 = x1 + 1;
			double yn1 = y2 - 4;
			double yn2 = y2 - 3;
			double yn3 = y2 - 1;
			
			// needle
			GL11.glColor4f(h, h, h, 1);
			GL11.glBegin(GL11.GL_TRIANGLES);
			GL11.glVertex2d(xn1, yn1);
			GL11.glVertex2d(xn2, yn2);
			GL11.glVertex2d(xn3, yn3);
			GL11.glEnd();
			
			// outlines
			GL11.glColor4f(0.0625F, 0.0625F, 0.0625F, 0.5F);
			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2d(xk1, yk1);
			GL11.glVertex2d(xk2, yk2);
			GL11.glVertex2d(xk3, yk3);
			GL11.glVertex2d(xk4, yk4);
			GL11.glEnd();
			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2d(xk5, yk5);
			GL11.glVertex2d(xk6, yk6);
			GL11.glVertex2d(xk3, yk7);
			GL11.glVertex2d(xk7, yk4);
			GL11.glEnd();
			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2d(xn1, yn1);
			GL11.glVertex2d(xn2, yn2);
			GL11.glVertex2d(xn3, yn3);
			GL11.glEnd();
		}
	}
	
	private void renderCloseButton(int x1, int y1, int x2, int y2,
		boolean hovering)
	{
		renderTitleBarButton(x1, y1, x2, y2, hovering);
		
		double xc1 = x1 + 2;
		double xc2 = x1 + 3;
		double xc3 = x2 - 2;
		double xc4 = x2 - 3;
		double xc5 = x1 + 3.5;
		double xc6 = (x1 + x2) / 2.0;
		double xc7 = x2 - 3.5;
		double yc1 = y1 + 3;
		double yc2 = y1 + 2;
		double yc3 = y2 - 3;
		double yc4 = y2 - 2;
		double yc5 = y1 + 3.5;
		double yc6 = (y1 + y2) / 2.0;
		double yc7 = y2 - 3.5;
		
		// cross
		GL11.glColor4f(hovering ? 1 : 0.85F, 0, 0, 1);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2d(xc1, yc1);
		GL11.glVertex2d(xc2, yc2);
		GL11.glVertex2d(xc3, yc3);
		GL11.glVertex2d(xc4, yc4);
		GL11.glVertex2d(xc3, yc1);
		GL11.glVertex2d(xc4, yc2);
		GL11.glVertex2d(xc6, yc5);
		GL11.glVertex2d(xc7, yc6);
		GL11.glVertex2d(xc6, yc7);
		GL11.glVertex2d(xc5, yc6);
		GL11.glVertex2d(xc1, yc3);
		GL11.glVertex2d(xc2, yc4);
		GL11.glEnd();
		
		// outline
		GL11.glColor4f(0.0625F, 0.0625F, 0.0625F, 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2d(xc1, yc1);
		GL11.glVertex2d(xc2, yc2);
		GL11.glVertex2d(xc6, yc5);
		GL11.glVertex2d(xc4, yc2);
		GL11.glVertex2d(xc3, yc1);
		GL11.glVertex2d(xc7, yc6);
		GL11.glVertex2d(xc3, yc3);
		GL11.glVertex2d(xc4, yc4);
		GL11.glVertex2d(xc6, yc7);
		GL11.glVertex2d(xc2, yc4);
		GL11.glVertex2d(xc1, yc3);
		GL11.glVertex2d(xc5, yc6);
		GL11.glEnd();
	}
	
	public float[] getBgColor()
	{
		return bgColor;
	}
	
	public float[] getAcColor()
	{
		return acColor;
	}
	
	public float getOpacity()
	{
		return opacity;
	}
	
	public void setTooltip(String tooltip)
	{
		this.tooltip = Objects.requireNonNull(tooltip);
	}
	
	public void addWindow(Window window)
	{
		windows.add(window);
	}
	
	public void addPopup(Popup popup)
	{
		popups.add(popup);
	}
	
	public boolean isLeftMouseButtonPressed()
	{
		return leftMouseButtonPressed;
	}
}
