/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.wurstclient.hacks.autolibrarian.BookOffer;
import net.wurstclient.settings.BookOffersSetting;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.WurstColors;

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
		addSelectableChild(listGui);
		
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
				.dimensions(width / 2 + 2, height - 74, 20, 12)
				.narrationSupplier(sup -> Text
					.translatable("gui.narrate.button", "increase level")
					.append(", current value: " + levelField.getText()))
				.build());
		levelPlusButton.active = false;
		
		addDrawableChild(levelMinusButton =
			ButtonWidget.builder(Text.literal("-"), b -> updateLevel(-1, true))
				.dimensions(width / 2 + 26, height - 74, 20, 12)
				.narrationSupplier(sup -> Text
					.translatable("gui.narrate.button", "decrease level")
					.append(", current value: " + levelField.getText()))
				.build());
		levelMinusButton.active = false;
		
		addDrawableChild(pricePlusButton = ButtonWidget
			.builder(Text.literal("+"), b -> updatePrice(1, true))
			.dimensions(width / 2 + 2, height - 58, 20, 12)
			.narrationSupplier(sup -> Text
				.translatable("gui.narrate.button", "increase max price")
				.append(", current value: " + priceField.getText()))
			.build());
		pricePlusButton.active = false;
		
		addDrawableChild(priceMinusButton = ButtonWidget
			.builder(Text.literal("-"), b -> updatePrice(-1, true))
			.dimensions(width / 2 + 26, height - 58, 20, 12)
			.narrationSupplier(sup -> Text
				.translatable("gui.narrate.button", "decrease max price")
				.append(", current value: " + priceField.getText()))
			.build());
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
	public boolean mouseClicked(Click context, boolean doubleClick)
	{
		boolean childClicked = super.mouseClicked(context, doubleClick);
		
		levelField.mouseClicked(context, doubleClick);
		priceField.mouseClicked(context, doubleClick);
		
		if(context.button() == GLFW.GLFW_MOUSE_BUTTON_4)
			cancelButton.onPress(context);
		
		return childClicked;
	}
	
	@Override
	public boolean keyPressed(KeyInput context)
	{
		switch(context.key())
		{
			case GLFW.GLFW_KEY_ENTER:
			if(addButton.active)
				addButton.onPress(context);
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			cancelButton.onPress(context);
			break;
			
			default:
			break;
		}
		
		return super.keyPressed(context);
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
		Matrix3x2fStack matrixStack = context.getMatrices();
		
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		matrixStack.pushMatrix();
		
		TextRenderer tr = client.textRenderer;
		String titleText =
			"Available Books (" + listGui.children().size() + ")";
		context.drawCenteredTextWithShadow(tr, titleText, width / 2, 12,
			Colors.WHITE);
		
		levelField.render(context, mouseX, mouseY, partialTicks);
		priceField.render(context, mouseX, mouseY, partialTicks);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		matrixStack.translate(width / 2 - 100, 0);
		
		context.drawTextWithShadow(tr, "Level:", 0, height - 72,
			WurstColors.VERY_LIGHT_GRAY);
		context.drawTextWithShadow(tr, "Max price:", 0, height - 56,
			WurstColors.VERY_LIGHT_GRAY);
		
		if(alreadyAdded && offerToAdd != null)
		{
			String errorText = offerToAdd.getEnchantmentNameWithLevel()
				+ " is already on your list!";
			context.drawTextWithShadow(tr, errorText, 0, height - 40,
				WurstColors.LIGHT_RED);
		}
		
		matrixStack.popMatrix();
		
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
	
	private final class Entry
		extends AlwaysSelectedEntryListWidget.Entry<AddBookOfferScreen.Entry>
	{
		private final BookOffer bookOffer;
		private long lastClickTime;
		
		public Entry(BookOffer bookOffer)
		{
			this.bookOffer = Objects.requireNonNull(bookOffer);
		}
		
		@Override
		public Text getNarration()
		{
			RegistryEntry<Enchantment> enchantment =
				bookOffer.getEnchantmentEntry().get();
			
			int maxLevel = enchantment.value().getMaxLevel();
			String levels = maxLevel + (maxLevel == 1 ? " level" : " levels");
			
			return Text.translatable("narrator.select",
				"Enchantment " + bookOffer.getEnchantmentName() + ", ID "
					+ bookOffer.id() + ", " + levels);
		}
		
		@Override
		public boolean mouseClicked(Click context, boolean doubleClick)
		{
			if(context.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT)
				return false;
			
			long timeSinceLastClick = Util.getMeasuringTimeMs() - lastClickTime;
			lastClickTime = Util.getMeasuringTimeMs();
			
			if(timeSinceLastClick < 250 && addButton.active)
				addButton.onPress(context);
			
			return true;
		}
		
		@Override
		public void render(DrawContext context, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			int x = getContentX();
			int y = getContentY();
			
			Item item = Registries.ITEM.get(Identifier.of("enchanted_book"));
			ItemStack stack = new ItemStack(item);
			RenderUtils.drawItem(context, stack, x + 1, y + 1, true);
			
			TextRenderer tr = client.textRenderer;
			RegistryEntry<Enchantment> enchantment =
				bookOffer.getEnchantmentEntry().get();
			
			String name = bookOffer.getEnchantmentName();
			int nameColor = enchantment.isIn(EnchantmentTags.CURSE)
				? WurstColors.LIGHT_RED : WurstColors.VERY_LIGHT_GRAY;
			context.drawText(tr, name, x + 28, y, nameColor, false);
			
			context.drawText(tr, bookOffer.id(), x + 28, y + 9,
				Colors.LIGHT_GRAY, false);
			
			int maxLevel = enchantment.value().getMaxLevel();
			String levels = maxLevel + (maxLevel == 1 ? " level" : " levels");
			context.drawText(tr, levels, x + 28, y + 18, Colors.LIGHT_GRAY,
				false);
		}
	}
	
	private final class ListGui
		extends AlwaysSelectedEntryListWidget<AddBookOfferScreen.Entry>
	{
		public ListGui(MinecraftClient minecraft, AddBookOfferScreen screen)
		{
			super(minecraft, screen.width, screen.height - 120, 36, 30);
			
			DynamicRegistryManager drm = client.world.getRegistryManager();
			Registry<Enchantment> registry =
				drm.getOrThrow(RegistryKeys.ENCHANTMENT);
			
			registry.stream().map(BookOffer::create)
				.filter(BookOffer::isFullyValid).sorted()
				.map(AddBookOfferScreen.Entry::new).forEach(this::addEntry);
		}
		
		@Override
		public void setSelected(@Nullable AddBookOfferScreen.Entry entry)
		{
			super.setSelected(entry);
			updateSelectedOffer(entry == null ? null : entry.bookOffer);
		}
	}
}
