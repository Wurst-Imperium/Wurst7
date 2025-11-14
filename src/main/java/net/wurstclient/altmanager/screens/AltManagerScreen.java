/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager.screens;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.wurstclient.WurstClient;
import net.wurstclient.altmanager.*;
import net.wurstclient.mixinterface.IMinecraftClient;
import net.wurstclient.util.MultiProcessingUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

public final class AltManagerScreen extends Screen
{
	private static final HashSet<Alt> failedLogins = new HashSet<>();
	
	private final Screen prevScreen;
	private final AltManager altManager;
	
	private ListGui listGui;
	private boolean shouldAsk = true;
	private int errorTimer;
	
	private Button useButton;
	private Button starButton;
	private Button editButton;
	private Button deleteButton;
	
	private Button importButton;
	private Button exportButton;
	private Button logoutButton;
	
	public AltManagerScreen(Screen prevScreen, AltManager altManager)
	{
		super(Component.literal("Alt Manager"));
		this.prevScreen = prevScreen;
		this.altManager = altManager;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(minecraft, this, altManager.getList());
		addWidget(listGui);
		
		WurstClient wurst = WurstClient.INSTANCE;
		
		Exception folderException = altManager.getFolderException();
		if(folderException != null && shouldAsk)
		{
			Component title = Component.literal(
				wurst.translate("gui.wurst.altmanager.folder_error.title"));
			Component message = Component.literal(wurst.translate(
				"gui.wurst.altmanager.folder_error.message", folderException));
			Component buttonText = Component.translatable("gui.done");
			
			// This just sets shouldAsk to false and closes the message.
			Runnable action = () -> confirmGenerate(false);
			
			AlertScreen screen =
				new AlertScreen(action, title, message, buttonText, false);
			minecraft.setScreen(screen);
			
		}else if(altManager.getList().isEmpty() && shouldAsk)
		{
			Component title = Component
				.literal(wurst.translate("gui.wurst.altmanager.empty.title"));
			Component message = Component
				.literal(wurst.translate("gui.wurst.altmanager.empty.message"));
			BooleanConsumer callback = this::confirmGenerate;
			
			ConfirmScreen screen = new ConfirmScreen(callback, title, message);
			minecraft.setScreen(screen);
		}
		
		addRenderableWidget(useButton =
			Button.builder(Component.literal("Login"), b -> pressLogin())
				.bounds(width / 2 - 154, height - 52, 100, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Direct Login"),
				b -> minecraft.setScreen(new DirectLoginScreen(this)))
			.bounds(width / 2 - 50, height - 52, 100, 20).build());
		
		addRenderableWidget(
			Button
				.builder(Component.literal("Add"),
					b -> minecraft
						.setScreen(new AddAltScreen(this, altManager)))
				.bounds(width / 2 + 54, height - 52, 100, 20).build());
		
		addRenderableWidget(starButton =
			Button.builder(Component.literal("Favorite"), b -> pressFavorite())
				.bounds(width / 2 - 154, height - 28, 75, 20).build());
		
		addRenderableWidget(editButton =
			Button.builder(Component.literal("Edit"), b -> pressEdit())
				.bounds(width / 2 - 76, height - 28, 74, 20).build());
		
		addRenderableWidget(deleteButton =
			Button.builder(Component.literal("Delete"), b -> pressDelete())
				.bounds(width / 2 + 2, height - 28, 74, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Cancel"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 + 80, height - 28, 75, 20).build());
		
		addRenderableWidget(importButton =
			Button.builder(Component.literal("Import"), b -> pressImportAlts())
				.bounds(8, 8, 50, 20).build());
		
		addRenderableWidget(exportButton =
			Button.builder(Component.literal("Export"), b -> pressExportAlts())
				.bounds(58, 8, 50, 20).build());
		
		addRenderableWidget(logoutButton =
			Button.builder(Component.literal("Logout"), b -> pressLogout())
				.bounds(width - 50 - 8, 8, 50, 20).build());
		
		updateAltButtons();
		boolean windowMode = !minecraft.options.fullscreen().get();
		importButton.active = windowMode;
		exportButton.active = windowMode;
	}
	
	private void updateAltButtons()
	{
		boolean altSelected = listGui.getSelected() != null;
		useButton.active = altSelected;
		starButton.active = altSelected;
		editButton.active = altSelected;
		deleteButton.active = altSelected;
		
		logoutButton.active =
			((IMinecraftClient)minecraft).getWurstSession() != null;
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			useButton.onPress();
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			onClose();
			return true;
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	private void pressLogin()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		try
		{
			altManager.login(alt);
			failedLogins.remove(alt);
			minecraft.setScreen(prevScreen);
			
		}catch(LoginException e)
		{
			errorTimer = 8;
			failedLogins.add(alt);
		}
	}
	
	private void pressLogout()
	{
		((IMinecraftClient)minecraft).setWurstSession(null);
		updateAltButtons();
	}
	
	private void pressFavorite()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		altManager.toggleFavorite(alt);
		listGui.setSelected(null);
	}
	
	private void pressEdit()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		minecraft.setScreen(new EditAltScreen(this, altManager, alt));
	}
	
	private void pressDelete()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		Component text =
			Component.literal("Are you sure you want to remove this alt?");
		
		String altName = alt.getDisplayName();
		Component message = Component.literal(
			"\"" + altName + "\" will be lost forever! (A long time!)");
		
		ConfirmScreen screen = new ConfirmScreen(this::confirmRemove, text,
			message, Component.literal("Delete"), Component.literal("Cancel"));
		minecraft.setScreen(screen);
	}
	
	private void pressImportAlts()
	{
		try
		{
			Process process = MultiProcessingUtils.startProcessWithIO(
				ImportAltsFileChooser.class,
				WurstClient.INSTANCE.getWurstFolder().toString());
			
			Path path = getFileChooserPath(process);
			process.waitFor();
			
			if(path.getFileName().toString().endsWith(".json"))
				importAsJSON(path);
			else
				importAsTXT(path);
			
		}catch(IOException | InterruptedException | JsonException e)
		{
			e.printStackTrace();
		}
	}
	
	private void importAsJSON(Path path) throws IOException, JsonException
	{
		WsonObject wson = JsonUtils.parseFileToObject(path);
		ArrayList<Alt> alts = AltsFile.parseJson(wson);
		altManager.addAll(alts);
	}
	
	private void importAsTXT(Path path) throws IOException
	{
		List<String> lines = Files.readAllLines(path);
		ArrayList<Alt> alts = new ArrayList<>();
		
		for(String line : lines)
		{
			String[] data = line.split(":");
			
			switch(data.length)
			{
				case 1:
				alts.add(new CrackedAlt(data[0]));
				break;
				
				case 2:
				alts.add(new MojangAlt(data[0], data[1]));
				break;
			}
		}
		
		altManager.addAll(alts);
	}
	
	private void pressExportAlts()
	{
		try
		{
			Process process = MultiProcessingUtils.startProcessWithIO(
				ExportAltsFileChooser.class,
				WurstClient.INSTANCE.getWurstFolder().toString());
			
			Path path = getFileChooserPath(process);
			
			process.waitFor();
			
			if(path.getFileName().toString().endsWith(".json"))
				exportAsJSON(path);
			else
				exportAsTXT(path);
			
		}catch(IOException | InterruptedException | JsonException e)
		{
			e.printStackTrace();
		}
	}
	
	private Path getFileChooserPath(Process process) throws IOException
	{
		try(BufferedReader bf =
			new BufferedReader(new InputStreamReader(process.getInputStream(),
				StandardCharsets.UTF_8)))
		{
			String response = bf.readLine();
			
			if(response == null)
				throw new IOException("No response from FileChooser");
			
			try
			{
				return Paths.get(response);
				
			}catch(InvalidPathException e)
			{
				throw new IOException(
					"Response from FileChooser is not a valid path");
			}
		}
	}
	
	private void exportAsJSON(Path path) throws IOException, JsonException
	{
		JsonObject json = AltsFile.createJson(altManager);
		JsonUtils.toJson(json, path);
	}
	
	private void exportAsTXT(Path path) throws IOException
	{
		List<String> lines = new ArrayList<>();
		
		for(Alt alt : altManager.getList())
			lines.add(alt.exportAsTXT());
		
		Files.write(path, lines);
	}
	
	private void confirmGenerate(boolean confirmed)
	{
		if(confirmed)
		{
			ArrayList<Alt> alts = new ArrayList<>();
			for(int i = 0; i < 8; i++)
				alts.add(new CrackedAlt(NameGenerator.generateName()));
			
			altManager.addAll(alts);
		}
		
		shouldAsk = false;
		minecraft.setScreen(this);
	}
	
	private void confirmRemove(boolean confirmed)
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		if(confirmed)
			altManager.remove(alt);
		
		minecraft.setScreen(this);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		// skin preview
		Alt alt = listGui.getSelectedAlt();
		if(alt != null)
		{
			AltRenderer.drawAltBack(context, alt.getName(),
				(width / 2 - 125) / 2 - 32, height / 2 - 64 - 9, 64, 128);
			AltRenderer.drawAltBody(context, alt.getName(),
				width - (width / 2 - 140) / 2 - 32, height / 2 - 64 - 9, 64,
				128);
		}
		
		// title text
		context.drawCenteredString(font, "Alt Manager", width / 2, 4,
			CommonColors.WHITE);
		context.drawCenteredString(font, "Alts: " + altManager.getList().size(),
			width / 2, 14, CommonColors.LIGHT_GRAY);
		context.drawCenteredString(font,
			"premium: " + altManager.getNumPremium() + ", cracked: "
				+ altManager.getNumCracked(),
			width / 2, 24, CommonColors.LIGHT_GRAY);
		
		// red flash for errors
		if(errorTimer > 0)
		{
			int alpha = (int)(Math.min(1, errorTimer / 16F) * 255);
			int color = 0xFF0000 | alpha << 24;
			context.fill(0, 0, width, height, color);
			errorTimer--;
		}
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		renderButtonTooltip(context, mouseX, mouseY);
		renderAltTooltip(context, mouseX, mouseY);
	}
	
	private void renderAltTooltip(GuiGraphics context, int mouseX, int mouseY)
	{
		if(!listGui.isMouseOver(mouseX, mouseY))
			return;
		
		Entry hoveredEntry = listGui.getHoveredEntry(mouseX, mouseY);
		if(hoveredEntry == null)
			return;
		
		int hoveredIndex = listGui.children().indexOf(hoveredEntry);
		int itemX = mouseX - listGui.getRowLeft();
		int itemY = mouseY - listGui.getRowTop(hoveredIndex);
		
		if(itemX < 31 || itemY < 15 || itemY >= 25)
			return;
		
		Alt alt = hoveredEntry.alt;
		ArrayList<Component> tooltip = new ArrayList<>();
		
		if(itemX >= 31 + font.width(hoveredEntry.getBottomText()))
			return;
		
		if(alt.isCracked())
			addTooltip(tooltip, "cracked");
		else
		{
			addTooltip(tooltip, "premium");
			
			if(failedLogins.contains(alt))
				addTooltip(tooltip, "failed");
			
			if(alt.isCheckedPremium())
				addTooltip(tooltip, "checked");
			else
				addTooltip(tooltip, "unchecked");
		}
		
		if(alt.isFavorite())
			addTooltip(tooltip, "favorite");
		
		context.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
	}
	
	private void renderButtonTooltip(GuiGraphics context, int mouseX,
		int mouseY)
	{
		for(AbstractWidget button : Screens.getButtons(this))
		{
			if(!button.isHoveredOrFocused())
				continue;
			
			if(button != importButton && button != exportButton)
				continue;
			
			ArrayList<Component> tooltip = new ArrayList<>();
			addTooltip(tooltip, "window");
			
			if(minecraft.options.fullscreen().get())
				addTooltip(tooltip, "fullscreen");
			else
				addTooltip(tooltip, "window_freeze");
			
			context.setComponentTooltipForNextFrame(font, tooltip, mouseX,
				mouseY);
			break;
		}
	}
	
	private void addTooltip(ArrayList<Component> tooltip, String trKey)
	{
		// translate
		String translated = WurstClient.INSTANCE
			.translate("description.wurst.altmanager." + trKey);
		
		// line-wrap
		StringJoiner joiner = new StringJoiner("\n");
		font.getSplitter().splitLines(translated, 200, Style.EMPTY).stream()
			.map(FormattedText::getString).forEach(s -> joiner.add(s));
		String wrapped = joiner.toString();
		
		// add to tooltip
		for(String line : wrapped.split("\n"))
			tooltip.add(Component.literal(line));
	}
	
	@Override
	public void onClose()
	{
		minecraft.setScreen(prevScreen);
	}
	
	private final class Entry
		extends ObjectSelectionList.Entry<AltManagerScreen.Entry>
	{
		private final Alt alt;
		private long lastClickTime;
		
		public Entry(Alt alt)
		{
			this.alt = Objects.requireNonNull(alt);
		}
		
		@Override
		public Component getNarration()
		{
			return Component.translatable("narrator.select",
				"Alt " + alt + ", " + StringUtil.stripColor(getBottomText()));
		}
		
		@Override
		public boolean mouseClicked(double mouseX, double mouseY,
			int mouseButton)
		{
			if(mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT)
				return false;
			
			long timeSinceLastClick = Util.getMillis() - lastClickTime;
			lastClickTime = Util.getMillis();
			
			if(timeSinceLastClick < 250)
				pressLogin();
			
			return true;
		}
		
		@Override
		public void render(GuiGraphics context, int index, int y, int x,
			int entryWidth, int entryHeight, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			// green glow when logged in
			if(minecraft.getUser().getName().equals(alt.getName()))
			{
				float opacity =
					0.3F - Math.abs(Mth.sin(System.currentTimeMillis() % 10000L
						/ 10000F * (float)Math.PI * 2.0F) * 0.15F);
				
				int color = 0x00FF00 | (int)(opacity * 255) << 24;
				context.fill(x - 2, y - 2, x + 218, y + 28, color);
			}
			
			// face
			AltRenderer.drawAltFace(context, alt.getName(), x + 1, y + 1, 24,
				24, listGui.getSelected() == this);
			
			Font tr = minecraft.font;
			
			// name / email
			context.drawString(tr, "Name: " + alt.getDisplayName(), x + 31,
				y + 3, CommonColors.LIGHT_GRAY, false);
			
			// status
			context.drawString(tr, getBottomText(), x + 31, y + 15,
				CommonColors.LIGHT_GRAY, false);
		}
		
		private String getBottomText()
		{
			String text = alt.isCracked() ? "\u00a78cracked" : "\u00a72premium";
			
			if(alt.isFavorite())
				text += "\u00a7r, \u00a7efavorite";
			
			if(failedLogins.contains(alt))
				text += "\u00a7r, \u00a7cwrong password?";
			else if(alt.isUncheckedPremium())
				text += "\u00a7r, \u00a7cunchecked";
			
			return text;
		}
	}
	
	private final class ListGui
		extends ObjectSelectionList<AltManagerScreen.Entry>
	{
		public ListGui(Minecraft minecraft, AltManagerScreen screen,
			List<Alt> list)
		{
			super(minecraft, screen.width, screen.height - 96, 36, 30, 0);
			
			list.stream().map(AltManagerScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		@Override
		public void setSelected(@Nullable AltManagerScreen.Entry entry)
		{
			super.setSelected(entry);
			updateAltButtons();
		}
		
		// This method sets selected to null without calling setSelected().
		@Override
		protected void clearEntries()
		{
			super.clearEntries();
			updateAltButtons();
		}
		
		/**
		 * @return The selected Alt, or null if no Alt is selected.
		 */
		public Alt getSelectedAlt()
		{
			AltManagerScreen.Entry selected = getSelected();
			return selected != null ? selected.alt : null;
		}
		
		/**
		 * @return The hovered Entry, or null if no Entry is hovered.
		 */
		public AltManagerScreen.Entry getHoveredEntry(double mouseX,
			double mouseY)
		{
			Optional<GuiEventListener> hovered = getChildAt(mouseX, mouseY);
			return hovered.map(e -> ((AltManagerScreen.Entry)e)).orElse(null);
		}
	}
}
