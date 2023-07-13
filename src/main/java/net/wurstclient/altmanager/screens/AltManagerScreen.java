/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
import java.util.StringJoiner;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.WurstClient;
import net.wurstclient.altmanager.*;
import net.wurstclient.mixinterface.IScreen;
import net.wurstclient.util.ListWidget;
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
		
		Exception folderException = altManager.getFolderException();
		if(folderException != null && shouldAsk)
		{
			Text title =
				Text.translatable("gui.wurst.altmanager.folder_error.title");
			Text message = Text.translatable(
				"gui.wurst.altmanager.folder_error.message", folderException);
			Text buttonText = Text.translatable("gui.done");
			
			// This just sets shouldAsk to false and closes the message.
			Runnable action = () -> confirmGenerate(false);
			
			NoticeScreen screen =
				new NoticeScreen(action, title, message, buttonText, false);
			client.setScreen(screen);
			
		}else if(altManager.getList().isEmpty() && shouldAsk)
		{
			Text title = Text.translatable("gui.wurst.altmanager.empty.title");
			Text message =
				Text.translatable("gui.wurst.altmanager.empty.message");
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
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		listGui.mouseClicked(mouseX, mouseY, mouseButton);
		
		if(mouseY >= 36 && mouseY <= height - 57)
			if(mouseX >= width / 2 + 140 || mouseX <= width / 2 - 126)
				listGui.selected = -1;
			
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button,
		double deltaX, double deltaY)
	{
		listGui.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		listGui.mouseReleased(mouseX, mouseY, button);
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override
	public boolean mouseScrolled(double d, double e, double amount)
	{
		listGui.mouseScrolled(d, e, amount);
		return super.mouseScrolled(d, e, amount);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			useButton.onPress();
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public void tick()
	{
		boolean altSelected = listGui.selected >= 0
			&& listGui.selected < altManager.getList().size();
		
		useButton.active = altSelected;
		starButton.active = altSelected;
		editButton.active = altSelected;
		deleteButton.active = altSelected;
		
		boolean windowMode = !client.options.getFullscreen().getValue();
		importButton.active = windowMode;
		exportButton.active = windowMode;
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
	
	private void pressFavorite()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		altManager.toggleFavorite(alt);
		listGui.selected = -1;
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
				throw new IOException("No reponse from FileChooser");
			
			try
			{
				return Paths.get(response);
				
			}catch(InvalidPathException e)
			{
				throw new IOException(
					"Reponse from FileChooser is not a valid path");
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
		if(listGui.getSelectedAlt() == null)
			return;
		
		if(confirmed)
			altManager.remove(listGui.selected);
		
		client.setScreen(this);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(context);
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		MatrixStack matrixStack = context.getMatrices();
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		// skin preview
		if(listGui.getSelectedSlot() != -1
			&& listGui.getSelectedSlot() < altManager.getList().size())
		{
			Alt alt = listGui.getSelectedAlt();
			if(alt == null)
				return;
			
			AltRenderer.drawAltBack(context, alt.getName(),
				(width / 2 - 125) / 2 - 32, height / 2 - 64 - 9, 64, 128);
			AltRenderer.drawAltBody(context, alt.getName(),
				width - (width / 2 - 140) / 2 - 32, height / 2 - 64 - 9, 64,
				128);
		}
		
		// title text
		context.drawCenteredTextWithShadow(textRenderer, "Alt Manager",
			width / 2, 4, 16777215);
		context.drawCenteredTextWithShadow(textRenderer,
			"Alts: " + altManager.getList().size(), width / 2, 14, 10526880);
		context.drawCenteredTextWithShadow(
			textRenderer, "premium: " + altManager.getNumPremium()
				+ ", cracked: " + altManager.getNumCracked(),
			width / 2, 24, 10526880);
		
		// red flash for errors
		if(errorTimer > 0)
		{
			RenderSystem.setShader(GameRenderer::getPositionProgram);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_BLEND);
			
			RenderSystem.setShaderColor(1, 0, 0, errorTimer / 16F);
			
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
				VertexFormats.POSITION);
			bufferBuilder.vertex(matrix, 0, 0, 0).next();
			bufferBuilder.vertex(matrix, width, 0, 0).next();
			bufferBuilder.vertex(matrix, width, height, 0).next();
			bufferBuilder.vertex(matrix, 0, height, 0).next();
			tessellator.draw();
			
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_BLEND);
			errorTimer--;
		}
		
		super.render(context, mouseX, mouseY, partialTicks);
		renderButtonTooltip(context, mouseX, mouseY);
		renderAltTooltip(context, mouseX, mouseY);
	}
	
	private void renderAltTooltip(DrawContext context, int mouseX, int mouseY)
	{
		if(!listGui.isMouseInList(mouseX, mouseY))
			return;
		
		List<Alt> altList = altManager.getList();
		int hoveredIndex = listGui.getItemAtPosition(mouseX, mouseY);
		
		if(hoveredIndex < 0 || hoveredIndex >= altList.size())
			return;
		
		int itemX = mouseX - (width - listGui.getRowWidth()) / 2;
		int itemY = mouseY - 36 + (int)listGui.getScrollAmount() - 4
			- hoveredIndex * 30;
		
		if(itemX < 31 || itemY < 15 || itemY >= 25)
			return;
		
		Alt alt = altList.get(hoveredIndex);
		ArrayList<Text> tooltip = new ArrayList<>();
		
		if(itemX >= 31 + textRenderer.getWidth(listGui.getBottomText(alt)))
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
		for(Drawable d : ((IScreen)(Object)this).getButtons())
		{
			if(!(d instanceof ClickableWidget))
				continue;
			
			ClickableWidget button = (ClickableWidget)d;
			
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
	
	public static final class ListGui extends ListWidget
	{
		private final List<Alt> list;
		private int selected = -1;
		private AltManagerScreen prevScreen;
		private long lastTime;
		
		public ListGui(MinecraftClient minecraft, AltManagerScreen prevScreen,
			List<Alt> list)
		{
			super(minecraft, prevScreen.width, prevScreen.height, 36,
				prevScreen.height - 56, 30);
			
			this.prevScreen = prevScreen;
			this.list = list;
		}
		
		@Override
		protected boolean isSelectedItem(int id)
		{
			return selected == id;
		}
		
		protected int getSelectedSlot()
		{
			return selected;
		}
		
		/**
		 * @return The selected Alt, or null if no Alt is selected.
		 */
		protected Alt getSelectedAlt()
		{
			if(selected < 0 || selected >= list.size())
				return null;
			
			return list.get(selected);
		}
		
		@Override
		protected int getItemCount()
		{
			return list.size();
		}
		
		@Override
		protected boolean selectItem(int index, int button, double mouseX,
			double mouseY)
		{
			if(index == selected && Util.getMeasuringTimeMs() - lastTime < 250)
				prevScreen.pressLogin();
			
			if(index >= 0 && index < list.size())
				selected = index;
			
			lastTime = Util.getMeasuringTimeMs();
			return true;
		}
		
		@Override
		protected void renderBackground()
		{
			
		}
		
		@Override
		protected void renderItem(DrawContext context, int id, int x, int y,
			int var4, int var5, int var6, float partialTicks)
		{
			Alt alt = list.get(id);
			
			MatrixStack matrixStack = context.getMatrices();
			Matrix4f matrix = matrixStack.peek().getPositionMatrix();
			Tessellator tessellator = RenderSystem.renderThreadTesselator();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			RenderSystem.setShader(GameRenderer::getPositionProgram);
			
			// green glow when logged in
			if(client.getSession().getUsername().equals(alt.getName()))
			{
				GL11.glDisable(GL11.GL_CULL_FACE);
				GL11.glEnable(GL11.GL_BLEND);
				
				float opacity =
					0.3F - Math.abs(MathHelper.sin(System.currentTimeMillis()
						% 10000L / 10000F * (float)Math.PI * 2.0F) * 0.15F);
				
				RenderSystem.setShaderColor(0, 1, 0, opacity);
				
				bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
					VertexFormats.POSITION);
				bufferBuilder.vertex(matrix, x - 2, y - 2, 0).next();
				bufferBuilder.vertex(matrix, x - 2 + 220, y - 2, 0).next();
				bufferBuilder.vertex(matrix, x - 2 + 220, y - 2 + 30, 0).next();
				bufferBuilder.vertex(matrix, x - 2, y - 2 + 30, 0).next();
				tessellator.draw();
				
				GL11.glEnable(GL11.GL_CULL_FACE);
				GL11.glDisable(GL11.GL_BLEND);
			}
			
			// face
			AltRenderer.drawAltFace(context, alt.getName(), x + 1, y + 1, 24,
				24, isSelectedItem(id));
			
			// name / email
			context.drawText(client.textRenderer,
				"Name: " + alt.getDisplayName(), x + 31, y + 3, 10526880,
				false);
			context.drawText(client.textRenderer,
				"Name: " + alt.getDisplayName(), x + 31, y + 3, 10526880,
				false);
			
			String bottomText = getBottomText(alt);
			context.drawText(client.textRenderer, bottomText, x + 31, y + 15,
				10526880, false);
		}
		
		public String getBottomText(Alt alt)
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
}
