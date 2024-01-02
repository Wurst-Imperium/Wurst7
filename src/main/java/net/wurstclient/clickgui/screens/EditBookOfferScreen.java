/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import org.lwjgl.glfw.GLFW;

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
import net.wurstclient.hacks.autolibrarian.BookOffer;
import net.wurstclient.settings.BookOffersSetting;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.RenderUtils;

public final class EditBookOfferScreen extends Screen
{
	private final Screen prevScreen;
	private final BookOffersSetting bookOffers;
	
	private TextFieldWidget levelField;
	private ButtonWidget levelPlusButton;
	private ButtonWidget levelMinusButton;
	
	private TextFieldWidget priceField;
	private ButtonWidget pricePlusButton;
	private ButtonWidget priceMinusButton;
	
	private ButtonWidget saveButton;
	private ButtonWidget cancelButton;
	
	private BookOffer offerToSave;
	private int index;
	private boolean alreadyAdded;
	
	public EditBookOfferScreen(Screen prevScreen, BookOffersSetting bookOffers,
		int index)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		this.bookOffers = bookOffers;
		this.index = index;
		offerToSave = bookOffers.getOffers().get(index);
	}
	
	@Override
	public void init()
	{
		levelField = new TextFieldWidget(client.textRenderer, width / 2 - 32,
			110, 28, 12, Text.literal(""));
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
			
			if(offerToSave == null)
				return true;
			
			Enchantment enchantment = offerToSave.getEnchantment();
			return level <= enchantment.getMaxLevel();
		});
		levelField.setChangedListener(t -> {
			if(!MathUtils.isInteger(t))
				return;
			
			int level = Integer.parseInt(t);
			updateLevel(level, false);
		});
		
		priceField = new TextFieldWidget(client.textRenderer, width / 2 - 32,
			126, 28, 12, Text.literal(""));
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
				.dimensions(width / 2 + 2, 110, 20, 12).build());
		levelPlusButton.active = false;
		
		addDrawableChild(levelMinusButton =
			ButtonWidget.builder(Text.literal("-"), b -> updateLevel(-1, true))
				.dimensions(width / 2 + 26, 110, 20, 12).build());
		levelMinusButton.active = false;
		
		addDrawableChild(pricePlusButton =
			ButtonWidget.builder(Text.literal("+"), b -> updatePrice(1, true))
				.dimensions(width / 2 + 2, 126, 20, 12).build());
		pricePlusButton.active = false;
		
		addDrawableChild(priceMinusButton =
			ButtonWidget.builder(Text.literal("-"), b -> updatePrice(-1, true))
				.dimensions(width / 2 + 26, 126, 20, 12).build());
		priceMinusButton.active = false;
		
		addDrawableChild(
			saveButton = ButtonWidget.builder(Text.literal("Save"), b -> {
				if(offerToSave == null || !offerToSave.isValid())
					return;
				
				bookOffers.replace(index, offerToSave);
				client.setScreen(prevScreen);
			}).dimensions(width / 2 - 102, height / 3 * 2, 100, 20).build());
		saveButton.active = false;
		
		addDrawableChild(cancelButton = ButtonWidget
			.builder(Text.literal("Cancel"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 + 2, height / 3 * 2, 100, 20).build());
		
		updateSelectedOffer(offerToSave);
	}
	
	private void updateLevel(int i, boolean offset)
	{
		if(offerToSave == null)
			return;
		
		String id = offerToSave.id();
		int level = offset ? offerToSave.level() + i : i;
		int price = offerToSave.price();
		
		Enchantment enchantment = offerToSave.getEnchantment();
		if(level < 1 || level > enchantment.getMaxLevel())
			return;
		
		updateSelectedOffer(new BookOffer(id, level, price));
	}
	
	private void updatePrice(int i, boolean offset)
	{
		if(offerToSave == null)
			return;
		
		String id = offerToSave.id();
		int level = offerToSave.level();
		int price = offset ? offerToSave.price() + i : i;
		
		if(price < 1 || price > 64)
			return;
		
		updateSelectedOffer(new BookOffer(id, level, price));
	}
	
	private void updateSelectedOffer(BookOffer offer)
	{
		offerToSave = offer;
		alreadyAdded =
			offer != null && !offer.equals(bookOffers.getOffers().get(index))
				&& bookOffers.contains(offer);
		saveButton.active = offer != null && !alreadyAdded;
		
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
		
		if(mouseButton == GLFW.GLFW_MOUSE_BUTTON_4)
			cancelButton.onPress();
		
		return childClicked;
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_ENTER:
			if(saveButton.active)
				saveButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			cancelButton.onPress();
			break;
			
			default:
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		levelPlusButton.active = offerToSave != null
			&& offerToSave.level() < offerToSave.getEnchantment().getMaxLevel();
		levelMinusButton.active =
			offerToSave != null && offerToSave.level() > 1;
		
		pricePlusButton.active =
			offerToSave != null && offerToSave.price() < 64;
		priceMinusButton.active =
			offerToSave != null && offerToSave.price() > 1;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		renderBackgroundTexture(context);
		
		matrixStack.push();
		matrixStack.translate(0, 0, 300);
		
		TextRenderer tr = client.textRenderer;
		String titleText = "Edit Book Offer";
		context.drawCenteredTextWithShadow(tr, titleText, width / 2, 12,
			0xffffff);
		
		int x = width / 2 - 100;
		int y = 64;
		
		Item item = Registries.ITEM.get(new Identifier("enchanted_book"));
		ItemStack stack = new ItemStack(item);
		RenderUtils.drawItem(context, stack, x + 1, y + 1, true);
		
		BookOffer bookOffer = offerToSave;
		String name = bookOffer.getEnchantmentNameWithLevel();
		
		Enchantment enchantment = bookOffer.getEnchantment();
		int nameColor = enchantment.isCursed() ? 0xff5555 : 0xffffff;
		context.drawTextWithShadow(tr, name, x + 28, y, nameColor);
		
		context.drawText(tr, bookOffer.id(), x + 28, y + 9, 0xa0a0a0, false);
		
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
		
		levelField.render(context, mouseX, mouseY, partialTicks);
		priceField.render(context, mouseX, mouseY, partialTicks);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		matrixStack.translate(width / 2 - 100, 112, 0);
		
		context.drawTextWithShadow(tr, "Level:", 0, 0, 0xf0f0f0);
		context.drawTextWithShadow(tr, "Max price:", 0, 16, 0xf0f0f0);
		
		if(alreadyAdded && offerToSave != null)
		{
			String errorText = offerToSave.getEnchantmentNameWithLevel()
				+ " is already on your list!";
			context.drawTextWithShadow(tr, errorText, 0, 32, 0xff5555);
		}
		
		matrixStack.pop();
		
		RenderUtils.drawItem(context, new ItemStack(Items.EMERALD),
			width / 2 - 16, 126, false);
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
}
