/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.util.List;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.wurstclient.hacks.autolibrarian.BookOffer;
import net.wurstclient.settings.BookOffersSetting;
import net.wurstclient.util.ListWidget;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.RenderUtils;

public final class AddBookOfferScreen extends Screen
{
	private final Screen prevScreen;
	private final BookOffersSetting bookOffers;
	
	private ListGui listGui;
	
	private TextFieldWidget levelField;
	private ButtonWidget levelPlusButton;
	private ButtonWidget levelMinusButton;
	
	private TextFieldWidget priceField;
	private ButtonWidget pricePlusButton;
	private ButtonWidget priceMinusButton;
	
	private ButtonWidget addButton;
	private ButtonWidget cancelButton;
	
	private BookOffer offerToAdd;
	private boolean alreadyAdded;
	
	public AddBookOfferScreen(Screen prevScreen, BookOffersSetting bookOffers)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		this.bookOffers = bookOffers;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this);
		
		levelField = new TextFieldWidget(client.textRenderer, width / 2 - 32,
			height - 74, 28, 12, Text.literal(""));
		addSelectableChild(levelField);
		levelField.setMaxLength(2);
		levelField.setTextPredicate(t -> {
			if(t.isEmpty())
				return true;
			
			if(!MathUtils.isInteger(t))
				return false;
			
			int level = Integer.parseInt(t);
			if(level < 1 || level > 10)
				return false;
			
			if(offerToAdd == null)
				return true;
			
			Enchantment enchantment = offerToAdd.getEnchantment();
			return level <= enchantment.getMaxLevel();
		});
		levelField.setChangedListener(t -> {
			if(!MathUtils.isInteger(t))
				return;
			
			int level = Integer.parseInt(t);
			updateLevel(level, false);
		});
		
		priceField = new TextFieldWidget(client.textRenderer, width / 2 - 32,
			height - 58, 28, 12, Text.literal(""));
		addSelectableChild(priceField);
		priceField.setMaxLength(2);
		priceField.setTextPredicate(t -> t.isEmpty() || MathUtils.isInteger(t)
			&& Integer.parseInt(t) >= 1 && Integer.parseInt(t) <= 64);
		priceField.setChangedListener(t -> {
			if(!MathUtils.isInteger(t))
				return;
			
			int price = Integer.parseInt(t);
			updatePrice(price, false);
		});
		
		addDrawableChild(levelPlusButton =
			ButtonWidget.builder(Text.literal("+"), b -> updateLevel(1, true))
				.dimensions(width / 2 + 2, height - 74, 20, 12).build());
		levelPlusButton.active = false;
		
		addDrawableChild(levelMinusButton =
			ButtonWidget.builder(Text.literal("-"), b -> updateLevel(-1, true))
				.dimensions(width / 2 + 26, height - 74, 20, 12).build());
		levelMinusButton.active = false;
		
		addDrawableChild(pricePlusButton =
			ButtonWidget.builder(Text.literal("+"), b -> updatePrice(1, true))
				.dimensions(width / 2 + 2, height - 58, 20, 12).build());
		pricePlusButton.active = false;
		
		addDrawableChild(priceMinusButton =
			ButtonWidget.builder(Text.literal("-"), b -> updatePrice(-1, true))
				.dimensions(width / 2 + 26, height - 58, 20, 12).build());
		priceMinusButton.active = false;
		
		addDrawableChild(
			addButton = ButtonWidget.builder(Text.literal("Add"), b -> {
				bookOffers.add(offerToAdd);
				client.setScreen(prevScreen);
			}).dimensions(width / 2 - 102, height - 28, 100, 20).build());
		addButton.active = false;
		
		addDrawableChild(cancelButton = ButtonWidget
			.builder(Text.literal("Cancel"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 + 2, height - 28, 100, 20).build());
	}
	
	private void updateLevel(int i, boolean offset)
	{
		if(offerToAdd == null)
			return;
		
		String id = offerToAdd.id();
		int level = offset ? offerToAdd.level() + i : i;
		int price = offerToAdd.price();
		
		Enchantment enchantment = offerToAdd.getEnchantment();
		if(level < 1 || level > enchantment.getMaxLevel())
			return;
		
		updateSelectedOffer(new BookOffer(id, level, price));
	}
	
	private void updatePrice(int i, boolean offset)
	{
		if(offerToAdd == null)
			return;
		
		String id = offerToAdd.id();
		int level = offerToAdd.level();
		int price = offset ? offerToAdd.price() + i : i;
		
		if(price < 1 || price > 64)
			return;
		
		updateSelectedOffer(new BookOffer(id, level, price));
	}
	
	private void updateSelectedOffer(BookOffer offer)
	{
		offerToAdd = offer;
		alreadyAdded = offer != null && bookOffers.contains(offer);
		addButton.active = offer != null && !alreadyAdded;
		
		if(offer == null)
		{
			if(!levelField.getText().isEmpty())
				levelField.setText("");
			
			if(!priceField.getText().isEmpty())
				priceField.setText("");
			
		}else
		{
			String level = "" + offer.level();
			if(!levelField.getText().equals(level))
				levelField.setText(level);
			
			String price = "" + offer.price();
			if(!priceField.getText().equals(price))
				priceField.setText(price);
		}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		boolean childClicked = super.mouseClicked(mouseX, mouseY, mouseButton);
		
		levelField.mouseClicked(mouseX, mouseY, mouseButton);
		priceField.mouseClicked(mouseX, mouseY, mouseButton);
		listGui.mouseClicked(mouseX, mouseY, mouseButton);
		
		if(!childClicked && mouseButton == 0
			&& (mouseX < (width - 220) / 2 || mouseX > width / 2 + 129)
			&& mouseY >= 32 && mouseY <= height - 80)
		{
			listGui.selected = -1;
			updateSelectedOffer(null);
		}
		
		if(mouseButton == GLFW.GLFW_MOUSE_BUTTON_4)
			cancelButton.onPress();
		
		return childClicked;
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
			if(addButton.active)
				addButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			cancelButton.onPress();
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
		levelPlusButton.active = offerToAdd != null
			&& offerToAdd.level() < offerToAdd.getEnchantment().getMaxLevel();
		levelMinusButton.active = offerToAdd != null && offerToAdd.level() > 1;
		
		pricePlusButton.active = offerToAdd != null && offerToAdd.price() < 64;
		priceMinusButton.active = offerToAdd != null && offerToAdd.price() > 1;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		matrixStack.push();
		matrixStack.translate(0, 0, 300);
		
		TextRenderer tr = client.textRenderer;
		String titleText = "Available Books (" + listGui.getItemCount() + ")";
		context.drawCenteredTextWithShadow(tr, titleText, width / 2, 12,
			0xffffff);
		
		levelField.render(context, mouseX, mouseY, partialTicks);
		priceField.render(context, mouseX, mouseY, partialTicks);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		matrixStack.translate(width / 2 - 100, 0, 0);
		
		context.drawTextWithShadow(tr, "Level:", 0, height - 72, 0xf0f0f0);
		context.drawTextWithShadow(tr, "Max price:", 0, height - 56, 0xf0f0f0);
		
		if(alreadyAdded && offerToAdd != null)
		{
			String errorText = offerToAdd.getEnchantmentNameWithLevel()
				+ " is already on your list!";
			context.drawTextWithShadow(tr, errorText, 0, height - 40, 0xff5555);
		}
		
		matrixStack.pop();
		
		RenderUtils.drawItem(context, new ItemStack(Items.EMERALD),
			width / 2 - 16, height - 58, false);
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
		private final AddBookOfferScreen screen;
		private final List<BookOffer> list;
		private int selected = -1;
		private long lastTime;
		
		public ListGui(MinecraftClient mc, AddBookOfferScreen screen)
		{
			super(mc, screen.width, screen.height, 32, screen.height - 80, 30);
			this.mc = mc;
			this.screen = screen;
			list = Registries.ENCHANTMENT.stream().map(BookOffer::create)
				.filter(BookOffer::isValid).sorted()
				.collect(Collectors.toList());
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
			if(button != 0)
				return true;
			
			if(index == selected && Util.getMeasuringTimeMs() - lastTime < 250
				&& screen.addButton.active)
				screen.addButton.onPress();
			
			if(index >= 0 && index < list.size())
			{
				selected = index;
				screen.updateSelectedOffer(list.get(index));
				lastTime = Util.getMeasuringTimeMs();
			}
			
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
			int var4, int mouseX, int mouseY, float partialTicks)
		{
			MatrixStack matrixStack = context.getMatrices();
			if(isSelectedItem(index))
				drawSelectionOutline(matrixStack, x, y);
			
			Item item = Registries.ITEM.get(new Identifier("enchanted_book"));
			ItemStack stack = new ItemStack(item);
			RenderUtils.drawItem(context, stack, x + 1, y + 1, true);
			
			TextRenderer tr = mc.textRenderer;
			BookOffer bookOffer = list.get(index);
			Enchantment enchantment = bookOffer.getEnchantment();
			
			String name = bookOffer.getEnchantmentName();
			int nameColor = enchantment.isCursed() ? 0xff5555 : 0xf0f0f0;
			context.drawText(tr, name, x + 28, y, nameColor, false);
			
			context.drawText(tr, bookOffer.id(), x + 28, y + 9, 0xa0a0a0,
				false);
			
			int maxLevel = enchantment.getMaxLevel();
			String levels = maxLevel + (maxLevel == 1 ? " level" : " levels");
			context.drawText(tr, levels, x + 28, y + 18, 0xa0a0a0, false);
		}
	}
}
