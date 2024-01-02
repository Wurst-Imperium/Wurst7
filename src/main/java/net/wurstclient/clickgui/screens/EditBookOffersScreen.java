/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.wurstclient.hacks.autolibrarian.BookOffer;
import net.wurstclient.settings.BookOffersSetting;
import net.wurstclient.util.ListWidget;
import net.wurstclient.util.RenderUtils;

public final class EditBookOffersScreen extends Screen
{
	private final Screen prevScreen;
	private final BookOffersSetting bookOffers;
	
	private ListGui listGui;
	private ButtonWidget editButton;
	private ButtonWidget removeButton;
	private ButtonWidget doneButton;
	
	public EditBookOffersScreen(Screen prevScreen, BookOffersSetting bookOffers)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		this.bookOffers = bookOffers;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this, bookOffers.getOffers());
		
		addDrawableChild(
			ButtonWidget
				.builder(Text.literal("Add"),
					b -> client
						.setScreen(new AddBookOfferScreen(this, bookOffers)))
				.dimensions(width / 2 - 154, height - 56, 100, 20).build());
		
		addDrawableChild(
			editButton = ButtonWidget.builder(Text.literal("Edit"), b -> {
				boolean selected = listGui.selected >= 0
					&& listGui.selected < listGui.list.size();
				if(!selected)
					return;
				
				client.setScreen(new EditBookOfferScreen(this, bookOffers,
					listGui.selected));
			}).dimensions(width / 2 - 50, height - 56, 100, 20).build());
		editButton.active = false;
		
		addDrawableChild(removeButton = ButtonWidget
			.builder(Text.literal("Remove"),
				b -> bookOffers.remove(listGui.selected))
			.dimensions(width / 2 + 54, height - 56, 100, 20).build());
		removeButton.active = false;
		
		addDrawableChild(ButtonWidget.builder(Text.literal("Reset to Defaults"),
			b -> client.setScreen(new ConfirmScreen(b2 -> {
				if(b2)
					bookOffers.resetToDefaults();
				client.setScreen(EditBookOffersScreen.this);
			}, Text.literal("Reset to Defaults"),
				Text.literal("Are you sure?"))))
			.dimensions(width - 106, 6, 100, 20).build());
		
		addDrawableChild(doneButton = ButtonWidget
			.builder(Text.literal("Done"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 - 100, height - 32, 200, 20).build());
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		boolean childClicked = super.mouseClicked(mouseX, mouseY, mouseButton);
		
		listGui.mouseClicked(mouseX, mouseY, mouseButton);
		
		if(!childClicked && mouseButton == 0 && (mouseX < (width - 220) / 2
			|| mouseX > width / 2 + 129 || mouseY < 32 || mouseY > height - 64))
			listGui.selected = -1;
		
		if(mouseButton == GLFW.GLFW_MOUSE_BUTTON_4)
			doneButton.onPress();
		
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
	public boolean mouseScrolled(double mouseX, double mouseY,
		double horizontalAmount, double verticalAmount)
	{
		listGui.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount,
			verticalAmount);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_ENTER:
			if(editButton.active)
				editButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_DELETE:
			removeButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			case GLFW.GLFW_KEY_BACKSPACE:
			doneButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_UP:
			listGui.selectItem(listGui.selected - 1, 0, 0, 0);
			break;
			
			case GLFW.GLFW_KEY_DOWN:
			listGui.selectItem(listGui.selected + 1, 0, 0, 0);
			break;
			
			default:
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		boolean selected =
			listGui.selected >= 0 && listGui.selected < listGui.list.size();
		
		editButton.active = selected;
		removeButton.active = selected;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		matrixStack.push();
		matrixStack.translate(0, 0, 300);
		
		context.drawCenteredTextWithShadow(client.textRenderer,
			bookOffers.getName() + " (" + listGui.getItemCount() + ")",
			width / 2, 12, 0xffffff);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		matrixStack.pop();
	}
	
	@Override
	public boolean shouldPause()
	{
		return false;
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	private static class ListGui extends ListWidget
	{
		private final MinecraftClient mc;
		private final List<BookOffer> list;
		private int selected = -1;
		
		public ListGui(MinecraftClient mc, EditBookOffersScreen screen,
			List<BookOffer> list)
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
		protected void renderItem(DrawContext context, int index, int x, int y,
			int var4, int var5, int var6, float partialTicks)
		{
			MatrixStack matrixStack = context.getMatrices();
			if(isSelectedItem(index))
				drawSelectionOutline(matrixStack, x, y);
			
			Item item = Registries.ITEM.get(new Identifier("enchanted_book"));
			ItemStack stack = new ItemStack(item);
			RenderUtils.drawItem(context, stack, x + 1, y + 1, true);
			
			TextRenderer tr = mc.textRenderer;
			BookOffer bookOffer = list.get(index);
			String name = bookOffer.getEnchantmentNameWithLevel();
			
			Enchantment enchantment = bookOffer.getEnchantment();
			int nameColor = enchantment.isCursed() ? 0xff5555 : 0xf0f0f0;
			context.drawText(tr, name, x + 28, y, nameColor, false);
			
			context.drawText(tr, bookOffer.id(), x + 28, y + 9, 0xa0a0a0,
				false);
			
			String price;
			if(bookOffer.price() >= 64)
				price = "any price";
			else
			{
				price = "max " + bookOffer.price();
				RenderUtils.drawItem(context, new ItemStack(Items.EMERALD),
					x + 28 + tr.getWidth(price), y + 16, false);
			}
			
			context.drawText(tr, price, x + 28, y + 18, 0xa0a0a0, false);
		}
	}
}
