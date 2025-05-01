/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.wurstclient.Category;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.components.FeatureButton;
import net.wurstclient.hacks.ClickGuiHack;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.RenderUtils;
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
	private int txtColor;
	private float opacity;
	private float ttOpacity;
	private int maxHeight;
	private int maxSettingsHeight;
	
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
		uiSettings.add(new FeatureButton(WURST.getOtfs().keybindManagerOtf));
		ClickGuiHack clickGuiHack = WURST.getHax().clickGuiHack;
		Stream<Setting> settings = clickGuiHack.getSettings().values().stream();
		settings.map(Setting::getComponent).forEach(c -> uiSettings.add(c));
		windows.add(uiSettings);
		
		for(Window window : windows)
			window.setMinimized(true);
		
		windows.add(WurstClient.INSTANCE.getHax().radarHack.getWindow());
		
		int x = 5;
		int y = 5;
		int scaledWidth = MC.getWindow().getScaledWidth();
		for(Window window : windows)
		{
			window.pack();
			
			if(x + window.getWidth() + 5 > scaledWidth)
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
			json = JsonParser.parseReader(reader).getAsJsonObject();
			
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
			jsonWindow.addProperty("x", window.getActualX());
			jsonWindow.addProperty("y", window.getActualY());
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
		if(mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT)
			leftMouseButtonPressed = true;
		
		boolean popupClicked =
			handlePopupMouseClick(mouseX, mouseY, mouseButton);
		
		if(!popupClicked)
			handleWindowMouseClick(mouseX, mouseY, mouseButton);
		
		for(Popup popup : popups)
			if(popup.getOwner().getParent().isClosing())
				popup.close();
			
		windows.removeIf(Window::isClosing);
		popups.removeIf(Popup::isClosing);
	}
	
	public void handleMouseRelease(double mouseX, double mouseY,
		int mouseButton)
	{
		if(mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT)
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
				
			popups.removeIf(Popup::isClosing);
		}
		
		return popupClicked;
	}
	
	public void handleNavigatorMouseClick(double cMouseX, double cMouseY,
		int mouseButton, Window window)
	{
		if(mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT)
			leftMouseButtonPressed = true;
		
		handleComponentMouseClick(window, cMouseX, cMouseY, mouseButton);
		
		for(Popup popup : popups)
			if(popup.getOwner().getParent().isClosing())
				popup.close();
			
		popups.removeIf(Popup::isClosing);
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
		if(mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT)
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
	
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		updateColors();
		
		Matrix3x2fStack matrixStack = context.getMatrices();
		matrixStack.pushMatrix();
		
		tooltip = "";
		int windowLayers = 0;
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
				
			context.state.goUpLayer();
			windowLayers++;
			renderWindow(context, window, mouseX, mouseY, partialTicks);
		}
		
		renderPopups(context, mouseX, mouseY);
		renderTooltip(context, mouseX, mouseY);
		
		matrixStack.popMatrix();
		for(int i = 0; i < windowLayers; i++)
			context.state.goDownLayer();
	}
	
	public void renderPopups(DrawContext context, int mouseX, int mouseY)
	{
		Matrix3x2fStack matrixStack = context.getMatrices();
		for(Popup popup : popups)
		{
			Component owner = popup.getOwner();
			Window parent = owner.getParent();
			
			int x1 = parent.getX() + owner.getX();
			int y1 =
				parent.getY() + 13 + parent.getScrollOffset() + owner.getY();
			
			matrixStack.pushMatrix();
			matrixStack.translate(x1, y1);
			context.state.goUpLayer();
			
			int cMouseX = mouseX - x1;
			int cMouseY = mouseY - y1;
			popup.render(context, cMouseX, cMouseY);
			
			context.state.goDownLayer();
			matrixStack.popMatrix();
		}
	}
	
	public void renderTooltip(DrawContext context, int mouseX, int mouseY)
	{
		if(tooltip.isEmpty())
			return;
		
		String[] lines = tooltip.split("\n");
		TextRenderer tr = MC.textRenderer;
		
		int tw = 0;
		int th = lines.length * tr.fontHeight;
		for(String line : lines)
		{
			int lw = tr.getWidth(line);
			if(lw > tw)
				tw = lw;
		}
		int sw = MC.currentScreen.width;
		int sh = MC.currentScreen.height;
		
		int xt1 = mouseX + tw + 11 <= sw ? mouseX + 8 : mouseX - tw - 8;
		int xt2 = xt1 + tw + 3;
		int yt1 = mouseY + th - 2 <= sh ? mouseY - 4 : mouseY - th - 4;
		int yt2 = yt1 + th + 2;
		
		context.state.goUpLayer();
		
		// background
		context.fill(xt1, yt1, xt2, yt2,
			RenderUtils.toIntColor(bgColor, ttOpacity));
		
		// outline
		RenderUtils.drawBorder2D(context, xt1, yt1, xt2, yt2,
			RenderUtils.toIntColor(acColor, 0.5F));
		
		// text
		context.state.goUpLayer();
		for(int i = 0; i < lines.length; i++)
			context.drawText(tr, lines[i], xt1 + 2, yt1 + 2 + i * tr.fontHeight,
				txtColor, false);
		context.state.goDownLayer();
		
		context.state.goDownLayer();
	}
	
	public void renderPinnedWindows(DrawContext context, float partialTicks)
	{
		int windowLayers = 0;
		for(Window window : windows)
		{
			if(!window.isPinned() || window.isInvisible())
				continue;
			
			context.state.goUpLayer();
			windowLayers++;
			renderWindow(context, window, Integer.MIN_VALUE, Integer.MIN_VALUE,
				partialTicks);
		}
		
		for(int i = 0; i < windowLayers; i++)
			context.state.goDownLayer();
	}
	
	public void updateColors()
	{
		ClickGuiHack clickGui = WURST.getHax().clickGuiHack;
		
		opacity = clickGui.getOpacity();
		ttOpacity = clickGui.getTooltipOpacity();
		bgColor = clickGui.getBackgroundColor();
		txtColor = clickGui.getTextColor();
		maxHeight = clickGui.getMaxHeight();
		maxSettingsHeight = clickGui.getMaxSettingsHeight();
		
		if(WurstClient.INSTANCE.getHax().rainbowUiHack.isEnabled())
			acColor = RenderUtils.getRainbowColor();
		else
			acColor = clickGui.getAccentColor();
	}
	
	private void renderWindow(DrawContext context, Window window, int mouseX,
		int mouseY, float partialTicks)
	{
		int x1 = window.getX();
		int y1 = window.getY();
		int x2 = x1 + window.getWidth();
		int y2 = y1 + window.getHeight();
		int y3 = y1 + 13;
		
		int windowBgColor = RenderUtils.toIntColor(bgColor, opacity);
		int outlineColor = RenderUtils.toIntColor(acColor, 0.5F);
		
		Matrix3x2fStack matrixStack = context.getMatrices();
		
		if(window.isMinimized())
			y2 = y3;
		
		if(mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2)
			tooltip = "";
		
		if(!window.isMinimized())
		{
			window.setMaxHeight(window instanceof SettingsWindow
				? maxSettingsHeight : maxHeight);
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
				context.fill(xs2, ys1, xs3, ys2, windowBgColor);
				context.fill(xs1, ys1, xs2, ys3, windowBgColor);
				context.fill(xs1, ys4, xs2, ys2, windowBgColor);
				
				boolean hovering = mouseX >= xs1 && mouseY >= ys3
					&& mouseX < xs2 && mouseY < ys4;
				
				// scrollbar
				int scrollbarColor = RenderUtils.toIntColor(acColor,
					hovering ? opacity * 1.5F : opacity);
				context.fill(xs1, ys3, xs2, ys4, scrollbarColor);
				
				// outline
				RenderUtils.drawBorder2D(context, xs1, ys3, xs2, ys4,
					outlineColor);
			}
			
			int x3 = x1 + 2;
			int x4 = window.isScrollingEnabled() ? x2 - 3 : x2;
			int x5 = x4 - 2;
			int y4 = y3 + window.getScrollOffset();
			
			// window background
			// left & right
			context.fill(x1, y3, x3, y2, windowBgColor);
			context.fill(x5, y3, x4, y2, windowBgColor);
			
			context.enableScissor(x1, y3, x2, y2);
			
			matrixStack.pushMatrix();
			matrixStack.translate(x1, y4);
			
			// window background
			// between children
			int xc1 = 2;
			int xc2 = x5 - x1;
			for(int i = 0; i < window.countChildren(); i++)
			{
				int yc1 = window.getChild(i).getY();
				int yc2 = yc1 - 2;
				context.fill(xc1, yc2, xc2, yc1, windowBgColor);
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
			context.fill(xc1, yc2, xc2, yc1, windowBgColor);
			
			// render children
			int cMouseX = mouseX - x1;
			int cMouseY = mouseY - y4;
			for(int i = 0; i < window.countChildren(); i++)
				window.getChild(i).render(context, cMouseX, cMouseY,
					partialTicks);
			
			matrixStack.popMatrix();
			context.disableScissor();
		}
		
		// window outline
		RenderUtils.drawBorder2D(context, x1, y1, x2, y2, outlineColor);
		
		// title bar separator line
		if(!window.isMinimized())
			RenderUtils.drawLine2D(context, x1, y3, x2, y3, outlineColor);
		
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
			renderTitleBarButton(context, x3, y4, x4, y5, hovering);
			ClickGuiIcons.drawCross(context, x3, y4, x4, y5, hovering);
		}
		
		if(window.isPinnable())
		{
			x3 -= 11;
			int x4 = x3 + 9;
			boolean hovering = hoveringY && mouseX >= x3 && mouseX < x4;
			renderTitleBarButton(context, x3, y4, x4, y5, hovering);
			ClickGuiIcons.drawPin(context, x3, y4, x4, y5, hovering,
				window.isPinned());
		}
		
		if(window.isMinimizable())
		{
			x3 -= 11;
			int x4 = x3 + 9;
			boolean hovering = hoveringY && mouseX >= x3 && mouseX < x4;
			renderTitleBarButton(context, x3, y4, x4, y5, hovering);
			ClickGuiIcons.drawMinimizeArrow(context, x3, y4, x4, y5, hovering,
				window.isMinimized());
		}
		
		// title bar background
		// above & below buttons
		int titleBgColor = RenderUtils.toIntColor(acColor, opacity);
		context.fill(x3, y1, x2, y4, titleBgColor);
		context.fill(x3, y5, x2, y3, titleBgColor);
		
		// title bar background
		// behind title
		context.fill(x1, y1, x3, y3, titleBgColor);
		
		// window title
		TextRenderer tr = MC.textRenderer;
		String title = tr.trimToWidth(Text.literal(window.getTitle()), x3 - x1)
			.getString();
		context.state.goUpLayer();
		context.drawText(tr, title, x1 + 2, y1 + 3, txtColor, false);
		context.state.goDownLayer();
	}
	
	private void renderTitleBarButton(DrawContext context, int x1, int y1,
		int x2, int y2, boolean hovering)
	{
		int x3 = x2 + 2;
		
		// button background
		int buttonBgColor = RenderUtils.toIntColor(bgColor,
			hovering ? opacity * 1.5F : opacity);
		context.fill(x1, y1, x2, y2, buttonBgColor);
		
		// background between buttons
		int windowBgColor = RenderUtils.toIntColor(acColor, opacity);
		context.fill(x2, y1, x3, y2, windowBgColor);
		
		// button outline
		int outlineColor = RenderUtils.toIntColor(acColor, 0.5F);
		RenderUtils.drawBorder2D(context, x1, y1, x2, y2, outlineColor);
	}
	
	public float[] getBgColor()
	{
		return bgColor;
	}
	
	public float[] getAcColor()
	{
		return acColor;
	}
	
	public int getTxtColor()
	{
		return txtColor;
	}
	
	public float getOpacity()
	{
		return opacity;
	}
	
	public float getTooltipOpacity()
	{
		return ttOpacity;
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
