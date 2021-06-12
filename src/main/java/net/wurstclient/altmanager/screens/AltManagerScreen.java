/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
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
		super(new LiteralText("Alt Manager"));
		this.prevScreen = prevScreen;
		this.altManager = altManager;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this, altManager.getList());
		
		if(altManager.getList().isEmpty() && shouldAsk)
			client.openScreen(new ConfirmScreen(this::confirmGenerate,
				new LiteralText("Your alt list is empty."), new LiteralText(
					"Would you like some random alts to get started?")));
		
		addDrawableChild(useButton = new ButtonWidget(width / 2 - 154,
			height - 52, 100, 20, new LiteralText("Login"), b -> pressLogin()));
		
		addDrawableChild(new ButtonWidget(width / 2 - 50, height - 52, 100, 20,
			new LiteralText("Direct Login"),
			b -> client.openScreen(new DirectLoginScreen(this))));
		
		addDrawableChild(new ButtonWidget(width / 2 + 54, height - 52, 100, 20,
			new LiteralText("Add"),
			b -> client.openScreen(new AddAltScreen(this, altManager))));
		
		addDrawableChild(
			starButton = new ButtonWidget(width / 2 - 154, height - 28, 75, 20,
				new LiteralText("Favorite"), b -> pressFavorite()));
		
		addDrawableChild(editButton = new ButtonWidget(width / 2 - 76,
			height - 28, 74, 20, new LiteralText("Edit"), b -> pressEdit()));
		
		addDrawableChild(
			deleteButton = new ButtonWidget(width / 2 + 2, height - 28, 74, 20,
				new LiteralText("Delete"), b -> pressDelete()));
		
		addDrawableChild(new ButtonWidget(width / 2 + 80, height - 28, 75, 20,
			new LiteralText("Cancel"), b -> client.openScreen(prevScreen)));
		
		addDrawableChild(importButton = new ButtonWidget(8, 8, 50, 20,
			new LiteralText("Import"), b -> pressImportAlts()));
		
		addDrawableChild(exportButton = new ButtonWidget(58, 8, 50, 20,
			new LiteralText("Export"), b -> pressExportAlts()));
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
		
		boolean windowMode = !client.options.fullscreen;
		importButton.active = windowMode;
		exportButton.active = windowMode;
	}
	
	private void pressLogin()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		if(alt.isCracked())
		{
			LoginManager.changeCrackedName(alt.getEmail());
			client.openScreen(prevScreen);
			return;
		}
		
		String error = LoginManager.login(alt.getEmail(), alt.getPassword());
		
		if(!error.isEmpty())
		{
			errorTimer = 8;
			return;
		}
		
		altManager.setChecked(listGui.selected,
			client.getSession().getUsername());
		client.openScreen(prevScreen);
	}
	
	private void pressFavorite()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		altManager.setStarred(listGui.selected, !alt.isStarred());
		listGui.selected = -1;
	}
	
	private void pressEdit()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		client.openScreen(new EditAltScreen(this, altManager, alt));
	}
	
	private void pressDelete()
	{
		Alt alt = listGui.getSelectedAlt();
		if(alt == null)
			return;
		
		LiteralText text =
			new LiteralText("Are you sure you want to remove this alt?");
		
		String altName = alt.getNameOrEmail();
		LiteralText message = new LiteralText(
			"\"" + altName + "\" will be lost forever! (A long time!)");
		
		ConfirmScreen screen = new ConfirmScreen(this::confirmRemove, text,
			message, new LiteralText("Delete"), new LiteralText("Cancel"));
		client.openScreen(screen);
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
				alts.add(new Alt(data[0], null, null));
				break;
				
				case 2:
				alts.add(new Alt(data[0], data[1], null));
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
			if(alt.isCracked())
				lines.add(alt.getEmail());
			else
				lines.add(alt.getEmail() + ":" + alt.getPassword());
			
		Files.write(path, lines);
	}
	
	private void confirmGenerate(boolean confirmed)
	{
		if(confirmed)
		{
			ArrayList<Alt> alts = new ArrayList<>();
			for(int i = 0; i < 8; i++)
				alts.add(new Alt(NameGenerator.generateName(), null, null));
			
			altManager.addAll(alts);
		}
		
		shouldAsk = false;
		client.openScreen(this);
	}
	
	private void confirmRemove(boolean confirmed)
	{
		if(confirmed)
			altManager.remove(listGui.selected);
		
		client.openScreen(this);
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(matrixStack);
		listGui.render(matrixStack, mouseX, mouseY, partialTicks);
		
		Matrix4f matrix = matrixStack.peek().getModel();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		// skin preview
		if(listGui.getSelectedSlot() != -1
			&& listGui.getSelectedSlot() < altManager.getList().size())
		{
			Alt alt = listGui.getSelectedAlt();
			if(alt == null)
				return;
			
			AltRenderer.drawAltBack(matrixStack, alt.getNameOrEmail(),
				(width / 2 - 125) / 2 - 32, height / 2 - 64 - 9, 64, 128);
			AltRenderer.drawAltBody(matrixStack, alt.getNameOrEmail(),
				width - (width / 2 - 140) / 2 - 32, height / 2 - 64 - 9, 64,
				128);
		}
		
		// title text
		drawCenteredText(matrixStack, textRenderer, "Alt Manager", width / 2, 4,
			16777215);
		drawCenteredText(matrixStack, textRenderer,
			"Alts: " + altManager.getList().size(), width / 2, 14, 10526880);
		drawCenteredText(
			matrixStack, textRenderer, "premium: " + altManager.getNumPremium()
				+ ", cracked: " + altManager.getNumCracked(),
			width / 2, 24, 10526880);
		
		// red flash for errors
		if(errorTimer > 0)
		{
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_BLEND);
			
			RenderSystem.setShaderColor(1, 0, 0, errorTimer / 16F);
			
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
				VertexFormats.POSITION);
			bufferBuilder.vertex(matrix, 0, 0, 0).next();
			bufferBuilder.vertex(matrix, width, 0, 0).next();
			bufferBuilder.vertex(matrix, width, height, 0).next();
			bufferBuilder.vertex(matrix, 0, height, 0).next();
			bufferBuilder.end();
			BufferRenderer.draw(bufferBuilder);
			
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_BLEND);
			errorTimer--;
		}
		
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		renderButtonTooltip(matrixStack, mouseX, mouseY);
	}
	
	private void renderButtonTooltip(MatrixStack matrixStack, int mouseX,
		int mouseY)
	{
		for(Drawable d : ((IScreen)(Object)this).getButtons())
		{
			if(!(d instanceof ClickableWidget))
				continue;
			
			ClickableWidget button = (ClickableWidget)d;
			
			if(!button.isHovered())
				continue;
			
			if(button != importButton && button != exportButton)
				continue;
			
			ArrayList<Text> tooltip = new ArrayList<>();
			tooltip.add(new LiteralText("This button opens another window."));
			if(client.options.fullscreen)
				tooltip
					.add(new LiteralText("\u00a7cTurn off fullscreen mode!"));
			else
			{
				tooltip
					.add(new LiteralText("It might look like the game is not"));
				tooltip.add(
					new LiteralText("responding while that window is open."));
			}
			renderTooltip(matrixStack, tooltip, mouseX, mouseY);
			break;
		}
	}
	
	public static final class ListGui extends ListWidget
	{
		private final List<Alt> list;
		private int selected = -1;
		
		public ListGui(MinecraftClient minecraft, AltManagerScreen prevScreen,
			List<Alt> list)
		{
			super(minecraft, prevScreen.width, prevScreen.height, 36,
				prevScreen.height - 56, 30);
			
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
			if(index >= 0 && index < list.size())
				selected = index;
			
			return true;
		}
		
		@Override
		protected void renderBackground()
		{
			
		}
		
		@Override
		protected void renderItem(MatrixStack matrixStack, int id, int x, int y,
			int var4, int var5, int var6, float partialTicks)
		{
			Alt alt = list.get(id);
			
			Matrix4f matrix = matrixStack.peek().getModel();
			BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
			RenderSystem.setShader(GameRenderer::getPositionShader);
			
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
				bufferBuilder.end();
				BufferRenderer.draw(bufferBuilder);
				
				GL11.glEnable(GL11.GL_CULL_FACE);
				GL11.glDisable(GL11.GL_BLEND);
			}
			
			// face
			AltRenderer.drawAltFace(matrixStack, alt.getNameOrEmail(), x + 1,
				y + 1, 24, 24, isSelectedItem(id));
			
			// name / email
			client.textRenderer.draw(matrixStack,
				"Name: " + alt.getNameOrEmail(), x + 31, y + 3, 10526880);
			
			// tags
			String tags = alt.isCracked() ? "\u00a78cracked" : "\u00a72premium";
			if(alt.isStarred())
				tags += "\u00a7r, \u00a7efavorite";
			if(alt.isUnchecked())
				tags += "\u00a7r, \u00a7cunchecked";
			client.textRenderer.draw(matrixStack, tags, x + 31, y + 15,
				10526880);
		}
	}
}
