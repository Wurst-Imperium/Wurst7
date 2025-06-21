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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
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
	
	private ButtonWidget useButton;
	private ButtonWidget starButton;
	private ButtonWidget editButton;
	private ButtonWidget deleteButton;
	
	private ButtonWidget importButton;
	private ButtonWidget exportButton;
	private ButtonWidget logoutButton;
	
	public AltManagerScreen(Screen prevScreen, AltManager altManager)
	{
		super(Text.literal("Alt Manager"));
		this.prevScreen = prevScreen;
		this.altManager = altManager;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this, altManager.getList());
		addSelectableChild(listGui);
		
		WurstClient wurst = WurstClient.INSTANCE;
		
		Exception folderException = altManager.getFolderException();
		if(folderException != null && shouldAsk)
		{
			Text title = Text.literal(
				wurst.translate("gui.wurst.altmanager.folder_error.title"));
			Text message = Text.literal(wurst.translate(
				"gui.wurst.altmanager.folder_error.message", folderException));
			Text buttonText = Text.translatable("gui.done");
			
			// This just sets shouldAsk to false and closes the message.
			Runnable action = () -> confirmGenerate(false);
			
			NoticeScreen screen =
				new NoticeScreen(action, title, message, buttonText, false);
			client.setScreen(screen);
			
		}else if(altManager.getList().isEmpty() && shouldAsk)
		{
			Text title = Text
				.literal(wurst.translate("gui.wurst.altmanager.empty.title"));
			Text message = Text
				.literal(wurst.translate("gui.wurst.altmanager.empty.message"));
			BooleanConsumer callback = this::confirmGenerate;
			
			ConfirmScreen screen = new ConfirmScreen(callback, title, message);
			client.setScreen(screen);
		}
		
		addDrawableChild(useButton =
			ButtonWidget.builder(Text.literal("Login"), b -> pressLogin())
				.dimensions(width / 2 - 154, height - 52, 100, 20).build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Direct Login"),
				b -> client.setScreen(new DirectLoginScreen(this)))
			.dimensions(width / 2 - 50, height - 52, 100, 20).build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Add"),
				b -> client.setScreen(new AddAltScreen(this, altManager)))
			.dimensions(width / 2 + 54, height - 52, 100, 20).build());
		
		addDrawableChild(starButton =
			ButtonWidget.builder(Text.literal("Favorite"), b -> pressFavorite())
				.dimensions(width / 2 - 154, height - 28, 75, 20).build());
		
		addDrawableChild(editButton =
			ButtonWidget.builder(Text.literal("Edit"), b -> pressEdit())
				.dimensions(width / 2 - 76, height - 28, 74, 20).build());
		
		addDrawableChild(deleteButton =
			ButtonWidget.builder(Text.literal("Delete"), b -> pressDelete())
				.dimensions(width / 2 + 2, height - 28, 74, 20).build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Cancel"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 + 80, height - 28, 75, 20).build());
		
		addDrawableChild(importButton =
			ButtonWidget.builder(Text.literal("Import"), b -> pressImportAlts())
				.dimensions(8, 8, 50, 20).build());
		
		addDrawableChild(exportButton =
			ButtonWidget.builder(Text.literal("Export"), b -> pressExportAlts())
				.dimensions(58, 8, 50, 20).build());
		
		addDrawableChild(logoutButton =
			ButtonWidget.builder(Text.literal("Logout"), b -> pressLogout())
				.dimensions(width - 50 - 8, 8, 50, 20).build());
		
		updateAltButtons();
		boolean windowMode = !client.options.getFullscreen().getValue();
		importButton.active = windowMode;
		exportButton.active = windowMode;
	}
	
	private void updateAltButtons()
	{
		boolean altSelected = listGui.getSelectedOrNull() != null;
		useButton.active = altSelected;
		starButton.active = altSelected;
		editButton.active = altSelected;
		deleteButton.active = altSelected;
		
		logoutButton.active =
			((IMinecraftClient)client).getWurstSession() != null;
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
			close();
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
			client.setScreen(prevScreen);
			
		}catch(LoginException e)
		{
			errorTimer = 8;
			failedLogins.add(alt);
		}
	}
	
	private void pressLogout()
	{
		((IMinecraftClient)client).setWurstSession(null);
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
		
		client.setScreen(new EditAltScreen(this, altManager, alt));
	}
	
	private void pressDelete()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		Text text = Text.literal("Are you sure you want to remove this alt?");
		
		String altName = alt.getDisplayName();
		Text message = Text.literal(
			"\"" + altName + "\" will be lost forever! (A long time!)");
		
		ConfirmScreen screen = new ConfirmScreen(this::confirmRemove, text,
			message, Text.literal("Delete"), Text.literal("Cancel"));
		client.setScreen(screen);
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
		client.setScreen(this);
	}
	
	private void confirmRemove(boolean confirmed)
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		if(confirmed)
			altManager.remove(alt);
		
		client.setScreen(this);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
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
		context.drawCenteredTextWithShadow(textRenderer, "Alt Manager",
			width / 2, 4, Colors.WHITE);
		context.drawCenteredTextWithShadow(textRenderer,
			"Alts: " + altManager.getList().size(), width / 2, 14,
			Colors.LIGHT_GRAY);
		context.drawCenteredTextWithShadow(
			textRenderer, "premium: " + altManager.getNumPremium()
				+ ", cracked: " + altManager.getNumCracked(),
			width / 2, 24, Colors.LIGHT_GRAY);
		
		// red flash for errors
		if(errorTimer > 0)
		{
			int alpha = (int)(Math.min(1, errorTimer / 16F) * 255);
			int color = 0xFF0000 | alpha << 24;
			context.fill(0, 0, width, height, color);
			errorTimer--;
		}
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		renderButtonTooltip(context, mouseX, mouseY);
		renderAltTooltip(context, mouseX, mouseY);
	}
	
	private void renderAltTooltip(DrawContext context, int mouseX, int mouseY)
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
		ArrayList<Text> tooltip = new ArrayList<>();
		
		if(itemX >= 31 + textRenderer.getWidth(hoveredEntry.getBottomText()))
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
		
		context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
	}
	
	private void renderButtonTooltip(DrawContext context, int mouseX,
		int mouseY)
	{
		for(ClickableWidget button : Screens.getButtons(this))
		{
			if(!button.isSelected())
				continue;
			
			if(button != importButton && button != exportButton)
				continue;
			
			ArrayList<Text> tooltip = new ArrayList<>();
			addTooltip(tooltip, "window");
			
			if(client.options.getFullscreen().getValue())
				addTooltip(tooltip, "fullscreen");
			else
				addTooltip(tooltip, "window_freeze");
			
			context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
			break;
		}
	}
	
	private void addTooltip(ArrayList<Text> tooltip, String trKey)
	{
		// translate
		String translated = WurstClient.INSTANCE
			.translate("description.wurst.altmanager." + trKey);
		
		// line-wrap
		StringJoiner joiner = new StringJoiner("\n");
		textRenderer.getTextHandler().wrapLines(translated, 200, Style.EMPTY)
			.stream().map(StringVisitable::getString)
			.forEach(s -> joiner.add(s));
		String wrapped = joiner.toString();
		
		// add to tooltip
		for(String line : wrapped.split("\n"))
			tooltip.add(Text.literal(line));
	}
	
	@Override
	public void close()
	{
		client.setScreen(prevScreen);
	}
	
	private final class Entry
		extends AlwaysSelectedEntryListWidget.Entry<AltManagerScreen.Entry>
	{
		private final Alt alt;
		private long lastClickTime;
		
		public Entry(Alt alt)
		{
			this.alt = Objects.requireNonNull(alt);
		}
		
		@Override
		public Text getNarration()
		{
			return Text.translatable("narrator.select", "Alt " + alt + ", "
				+ StringHelper.stripTextFormat(getBottomText()));
		}
		
		@Override
		public boolean mouseClicked(double mouseX, double mouseY,
			int mouseButton)
		{
			if(mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT)
				return false;
			
			long timeSinceLastClick = Util.getMeasuringTimeMs() - lastClickTime;
			lastClickTime = Util.getMeasuringTimeMs();
			
			if(timeSinceLastClick < 250)
				pressLogin();
			
			return true;
		}
		
		@Override
		public void render(DrawContext context, int index, int y, int x,
			int entryWidth, int entryHeight, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			// green glow when logged in
			if(client.getSession().getUsername().equals(alt.getName()))
			{
				float opacity =
					0.3F - Math.abs(MathHelper.sin(System.currentTimeMillis()
						% 10000L / 10000F * (float)Math.PI * 2.0F) * 0.15F);
				
				int color = 0x00FF00 | (int)(opacity * 255) << 24;
				context.fill(x - 2, y - 2, x + 218, y + 28, color);
			}
			
			// face
			AltRenderer.drawAltFace(context, alt.getName(), x + 1, y + 1, 24,
				24, listGui.getSelectedOrNull() == this);
			
			TextRenderer tr = client.textRenderer;
			
			// name / email
			context.drawText(tr, "Name: " + alt.getDisplayName(), x + 31, y + 3,
				Colors.LIGHT_GRAY, false);
			
			// status
			context.drawText(tr, getBottomText(), x + 31, y + 15,
				Colors.LIGHT_GRAY, false);
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
		extends AlwaysSelectedEntryListWidget<AltManagerScreen.Entry>
	{
		public ListGui(MinecraftClient minecraft, AltManagerScreen screen,
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
			AltManagerScreen.Entry selected = getSelectedOrNull();
			return selected != null ? selected.alt : null;
		}
		
		/**
		 * @return The hovered Entry, or null if no Entry is hovered.
		 */
		public AltManagerScreen.Entry getHoveredEntry(double mouseX,
			double mouseY)
		{
			Optional<Element> hovered = hoveredElement(mouseX, mouseY);
			return hovered.map(e -> ((AltManagerScreen.Entry)e)).orElse(null);
		}
	}
}
