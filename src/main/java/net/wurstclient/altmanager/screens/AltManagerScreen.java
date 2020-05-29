/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
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
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.WurstClient;
import net.wurstclient.altmanager.Alt;
import net.wurstclient.altmanager.AltManager;
import net.wurstclient.altmanager.AltRenderer;
import net.wurstclient.altmanager.ImportAltsFileChooser;
import net.wurstclient.altmanager.LoginManager;
import net.wurstclient.altmanager.NameGenerator;
import net.wurstclient.util.ListWidget;
import net.wurstclient.util.MultiProcessingUtils;

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
		
		addButton(useButton = new ButtonWidget(width / 2 - 154, height - 52,
			100, 20, new LiteralText("Use"), b -> pressUse()));
		
		addButton(new ButtonWidget(width / 2 - 50, height - 52, 100, 20,
			new LiteralText("Direct Login"),
			b -> client.openScreen(new DirectLoginScreen(this))));
		
		addButton(new ButtonWidget(width / 2 + 54, height - 52, 100, 20,
			new LiteralText("Add"),
			b -> client.openScreen(new AddAltScreen(this, altManager))));
		
		addButton(starButton = new ButtonWidget(width / 2 - 154, height - 28,
			75, 20, new LiteralText("Star"), b -> pressStar()));
		
		addButton(editButton = new ButtonWidget(width / 2 - 76, height - 28, 74,
			20, new LiteralText("Edit"), b -> pressEdit()));
		
		addButton(deleteButton = new ButtonWidget(width / 2 + 2, height - 28,
			74, 20, new LiteralText("Delete"), b -> pressDelete()));
		
		addButton(new ButtonWidget(width / 2 + 80, height - 28, 75, 20,
			new LiteralText("Cancel"), b -> client.openScreen(prevScreen)));
		
		addButton(new ButtonWidget(8, 8, 100, 20,
			new LiteralText("Import Alts"), b -> pressImportAlts()));
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
	}
	
	private void pressUse()
	{
		Alt alt = listGui.getSelectedAlt();
		
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
	
	private void pressStar()
	{
		Alt alt = listGui.getSelectedAlt();
		altManager.setStarred(listGui.selected, !alt.isStarred());
		listGui.selected = -1;
	}
	
	private void pressEdit()
	{
		Alt alt = listGui.getSelectedAlt();
		client.openScreen(new EditAltScreen(this, altManager, alt));
	}
	
	private void pressDelete()
	{
		LiteralText text =
			new LiteralText("Are you sure you want to remove this alt?");
		
		String altName = listGui.getSelectedAlt().getNameOrEmail();
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
			
			try(BufferedReader bf = new BufferedReader(new InputStreamReader(
				process.getInputStream(), StandardCharsets.UTF_8)))
			{
				ArrayList<Alt> alts = new ArrayList<>();
				
				for(String line = ""; (line = bf.readLine()) != null;)
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
			
			process.waitFor();
			
		}catch(IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
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
		
		// skin preview
		if(listGui.getSelectedSlot() != -1
			&& listGui.getSelectedSlot() < altManager.getList().size())
		{
			Alt alt = listGui.getSelectedAlt();
			AltRenderer.drawAltBack(matrixStack, alt.getNameOrEmail(),
				(width / 2 - 125) / 2 - 32, height / 2 - 64 - 9, 64, 128);
			AltRenderer.drawAltBody(matrixStack, alt.getNameOrEmail(),
				width - (width / 2 - 140) / 2 - 32, height / 2 - 64 - 9, 64,
				128);
		}
		
		// title text
		drawCenteredString(matrixStack, textRenderer, "Alt Manager", width / 2,
			4, 16777215);
		drawCenteredString(matrixStack, textRenderer,
			"Alts: " + altManager.getList().size(), width / 2, 14, 10526880);
		drawCenteredString(
			matrixStack, textRenderer, "premium: " + altManager.getNumPremium()
				+ ", cracked: " + altManager.getNumCracked(),
			width / 2, 24, 10526880);
		
		// red flash for errors
		if(errorTimer > 0)
		{
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_BLEND);
			
			GL11.glColor4f(1, 0, 0, errorTimer / 16F);
			
			GL11.glBegin(GL11.GL_QUADS);
			{
				GL11.glVertex2d(0, 0);
				GL11.glVertex2d(width, 0);
				GL11.glVertex2d(width, height);
				GL11.glVertex2d(0, height);
			}
			GL11.glEnd();
			
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_BLEND);
			errorTimer--;
		}
		
		super.render(matrixStack, mouseX, mouseY, partialTicks);
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
			
			// green glow when logged in
			if(client.getSession().getUsername().equals(alt.getName()))
			{
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				GL11.glDisable(GL11.GL_CULL_FACE);
				GL11.glEnable(GL11.GL_BLEND);
				
				float opacity =
					0.3F - Math.abs(MathHelper.sin(System.currentTimeMillis()
						% 10000L / 10000F * (float)Math.PI * 2.0F) * 0.15F);
				
				GL11.glColor4f(0, 1, 0, opacity);
				
				GL11.glBegin(GL11.GL_QUADS);
				{
					GL11.glVertex2d(x - 2, y - 2);
					GL11.glVertex2d(x - 2 + 220, y - 2);
					GL11.glVertex2d(x - 2 + 220, y - 2 + 30);
					GL11.glVertex2d(x - 2, y - 2 + 30);
				}
				GL11.glEnd();
				
				GL11.glEnable(GL11.GL_TEXTURE_2D);
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
				tags += "\u00a7r, \u00a7estarred";
			if(alt.isUnchecked())
				tags += "\u00a7r, \u00a7cunchecked";
			client.textRenderer.draw(matrixStack, tags, x + 31, y + 15,
				10526880);
		}
	}
}
