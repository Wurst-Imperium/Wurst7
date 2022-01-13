/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.util.ItemUtils;
import net.wurstclient.util.ListWidget;

public final class EditItemListScreen extends Screen
{
	private final Screen prevScreen;
	private final ItemListSetting itemList;
	
	private ListGui listGui;
	private TextFieldWidget itemNameField;
	private ButtonWidget addButton;
	private ButtonWidget removeButton;
	private ButtonWidget doneButton;
	
	private Item itemToAdd;
	
	public EditItemListScreen(Screen prevScreen, ItemListSetting itemList)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
		this.itemList = itemList;
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this, itemList.getItemNames());
		
		itemNameField = new TextFieldWidget(client.textRenderer,
			width / 2 - 152, height - 55, 150, 18, new LiteralText(""));
		addSelectableChild(itemNameField);
		itemNameField.setMaxLength(256);
		
		addDrawableChild(addButton = new ButtonWidget(width / 2 - 2,
			height - 56, 30, 20, new LiteralText("Add"), b -> {
				itemList.add(itemToAdd);
				itemNameField.setText("");
			}));
		
		addDrawableChild(removeButton = new ButtonWidget(width / 2 + 52,
			height - 56, 100, 20, new LiteralText("Remove Selected"),
			b -> itemList.remove(listGui.selected)));
		
		addDrawableChild(new ButtonWidget(width - 108, 8, 100, 20,
			new LiteralText("Reset to Defaults"),
			b -> client.setScreen(new ConfirmScreen(b2 -> {
				if(b2)
					itemList.resetToDefaults();
				client.setScreen(EditItemListScreen.this);
			}, new LiteralText("Reset to Defaults"),
				new LiteralText("Are you sure?")))));
		
		addDrawableChild(
			doneButton = new ButtonWidget(width / 2 - 100, height - 28, 200, 20,
				new LiteralText("Done"), b -> client.setScreen(prevScreen)));
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		boolean childClicked = super.mouseClicked(mouseX, mouseY, mouseButton);
		
		itemNameField.mouseClicked(mouseX, mouseY, mouseButton);
		listGui.mouseClicked(mouseX, mouseY, mouseButton);
		
		if(!childClicked && (mouseX < (width - 220) / 2
			|| mouseX > width / 2 + 129 || mouseY < 32 || mouseY > height - 64))
			listGui.selected = -1;
		
		return childClicked;
	}
	
	@Override
	public boolean mouseDragged(double double_1, double double_2, int int_1,
		double double_3, double double_4)
	{
		listGui.mouseDragged(double_1, double_2, int_1, double_3, double_4);
		return super.mouseDragged(double_1, double_2, int_1, double_3,
			double_4);
	}
	
	@Override
	public boolean mouseReleased(double double_1, double double_2, int int_1)
	{
		listGui.mouseReleased(double_1, double_2, int_1);
		return super.mouseReleased(double_1, double_2, int_1);
	}
	
	@Override
	public boolean mouseScrolled(double double_1, double double_2,
		double double_3)
	{
		listGui.mouseScrolled(double_1, double_2, double_3);
		return super.mouseScrolled(double_1, double_2, double_3);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_ENTER:
			if(addButton.active)
				addButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_DELETE:
			if(!itemNameField.isFocused())
				removeButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			doneButton.onPress();
			break;
			
			default:
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		itemNameField.tick();
		
		itemToAdd = ItemUtils
			.getItemFromNameOrID(itemNameField.getText().toLowerCase());
		addButton.active = itemToAdd != null;
		
		removeButton.active =
			listGui.selected >= 0 && listGui.selected < listGui.list.size();
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(matrixStack, mouseX, mouseY, partialTicks);
		
		drawCenteredText(matrixStack, client.textRenderer,
			itemList.getName() + " (" + listGui.getItemCount() + ")", width / 2,
			12, 0xffffff);
		
		matrixStack.push();
		matrixStack.translate(0, 0, 300);
		
		itemNameField.render(matrixStack, mouseX, mouseY, partialTicks);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		
		matrixStack.translate(-64 + width / 2 - 152, 0, 0);
		
		if(itemNameField.getText().isEmpty() && !itemNameField.isFocused())
		{
			matrixStack.push();
			matrixStack.translate(0, 0, 300);
			drawStringWithShadow(matrixStack, client.textRenderer,
				"item name or ID", 68, height - 50, 0x808080);
			matrixStack.pop();
		}
		
		int border = itemNameField.isFocused() ? 0xffffffff : 0xffa0a0a0;
		int black = 0xff000000;
		
		fill(matrixStack, 48, height - 56, 64, height - 36, border);
		fill(matrixStack, 49, height - 55, 64, height - 37, black);
		fill(matrixStack, 214, height - 56, 244, height - 55, border);
		fill(matrixStack, 214, height - 37, 244, height - 36, border);
		fill(matrixStack, 244, height - 56, 246, height - 36, border);
		fill(matrixStack, 214, height - 55, 243, height - 52, black);
		fill(matrixStack, 214, height - 40, 243, height - 37, black);
		fill(matrixStack, 215, height - 55, 216, height - 37, black);
		fill(matrixStack, 242, height - 55, 245, height - 37, black);
		
		matrixStack.pop();
		
		listGui.renderIconAndGetName(matrixStack, new ItemStack(itemToAdd),
			width / 2 - 164, height - 52, false);
	}
	
	private static class ListGui extends ListWidget
	{
		private final MinecraftClient mc;
		private final List<String> list;
		private int selected = -1;
		
		public ListGui(MinecraftClient mc, EditItemListScreen screen,
			List<String> list)
		{
			super(mc, screen.width, screen.height, 32, screen.height - 64, 30);
			this.mc = mc;
			this.list = list;
		}
		
		@Override
		protected int getItemCount()
		{
			return list.size();
		}
		
		@Override
		protected boolean selectItem(int index, int int_2, double var3,
			double var4)
		{
			if(index >= 0 && index < list.size())
				selected = index;
			
			return true;
		}
		
		@Override
		protected boolean isSelectedItem(int index)
		{
			return index == selected;
		}
		
		@Override
		protected void renderBackground()
		{
			
		}
		
		@Override
		protected void renderItem(MatrixStack matrixStack, int index, int x,
			int y, int var4, int var5, int var6, float partialTicks)
		{
			String name = list.get(index);
			Item item = Registry.ITEM.get(new Identifier(name));
			ItemStack stack = new ItemStack(item);
			TextRenderer fr = mc.textRenderer;
			
			String displayName =
				renderIconAndGetName(matrixStack, stack, x + 1, y + 1, true);
			fr.draw(matrixStack, displayName, x + 28, y, 0xf0f0f0);
			fr.draw(matrixStack, name, x + 28, y + 9, 0xa0a0a0);
			fr.draw(matrixStack, "ID: " + Registry.ITEM.getRawId(item), x + 28,
				y + 18, 0xa0a0a0);
		}
		
		private String renderIconAndGetName(MatrixStack matrixStack,
			ItemStack stack, int x, int y, boolean large)
		{
			if(stack.isEmpty())
			{
				MatrixStack modelViewStack = RenderSystem.getModelViewStack();
				modelViewStack.push();
				modelViewStack.translate(x, y, 0);
				if(large)
					modelViewStack.scale(1.5F, 1.5F, 1.5F);
				else
					modelViewStack.scale(0.75F, 0.75F, 0.75F);
				
				DiffuseLighting.enableGuiDepthLighting();
				mc.getItemRenderer().renderInGuiWithOverrides(
					new ItemStack(Blocks.GRASS_BLOCK), 0, 0);
				DiffuseLighting.disableGuiDepthLighting();
				
				modelViewStack.pop();
				RenderSystem.applyModelViewMatrix();
				
				matrixStack.push();
				matrixStack.translate(x, y, 0);
				if(large)
					matrixStack.scale(2, 2, 2);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				TextRenderer fr = mc.textRenderer;
				fr.drawWithShadow(matrixStack, "?", 3, 2, 0xf0f0f0);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
				matrixStack.pop();
				
				return "\u00a7ounknown item\u00a7r";
			}
			
			MatrixStack modelViewStack = RenderSystem.getModelViewStack();
			modelViewStack.push();
			modelViewStack.translate(x, y, 0);
			
			if(large)
				modelViewStack.scale(1.5F, 1.5F, 1.5F);
			else
				modelViewStack.scale(0.75F, 0.75F, 0.75F);
			
			DiffuseLighting.enableGuiDepthLighting();
			mc.getItemRenderer().renderInGuiWithOverrides(stack, 0, 0);
			DiffuseLighting.disableGuiDepthLighting();
			
			modelViewStack.pop();
			RenderSystem.applyModelViewMatrix();
			
			return stack.getName().getString();
		}
	}
}
