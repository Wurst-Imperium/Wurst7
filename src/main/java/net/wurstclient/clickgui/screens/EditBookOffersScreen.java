/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.util.List;
import java.util.Objects;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.wurstclient.hacks.autolibrarian.BookOffer;
import net.wurstclient.settings.BookOffersSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.WurstColors;

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
		addSelectableChild(listGui);
		
		addDrawableChild(
			ButtonWidget
				.builder(Text.literal("Add"),
					b -> client
						.setScreen(new AddBookOfferScreen(this, bookOffers)))
				.dimensions(width / 2 - 154, height - 56, 100, 20).build());
		
		addDrawableChild(
			editButton = ButtonWidget.builder(Text.literal("Edit"), b -> {
				BookOffer selected = listGui.getSelectedOffer();
				if(selected == null)
					return;
				
				client.setScreen(new EditBookOfferScreen(this, bookOffers,
					bookOffers.indexOf(selected)));
			}).dimensions(width / 2 - 50, height - 56, 100, 20).build());
		editButton.active = false;
		
		addDrawableChild(
			removeButton = ButtonWidget.builder(Text.literal("Remove"), b -> {
				bookOffers
					.remove(bookOffers.indexOf(listGui.getSelectedOffer()));
				client.setScreen(EditBookOffersScreen.this);
			}).dimensions(width / 2 + 54, height - 56, 100, 20).build());
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
	public boolean mouseClicked(Click context, boolean doubleClick)
	{
		boolean childClicked = super.mouseClicked(context, doubleClick);
		
		if(context.button() == GLFW.GLFW_MOUSE_BUTTON_4)
			doneButton.onPress(context);
		
		return childClicked;
	}
	
	@Override
	public boolean keyPressed(KeyInput context)
	{
		switch(context.key())
		{
			case GLFW.GLFW_KEY_ENTER:
			if(editButton.active)
				editButton.onPress(context);
			break;
			
			case GLFW.GLFW_KEY_DELETE:
			removeButton.onPress(context);
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			case GLFW.GLFW_KEY_BACKSPACE:
			doneButton.onPress(context);
			break;
			
			default:
			break;
		}
		
		return super.keyPressed(context);
	}
	
	@Override
	public void tick()
	{
		boolean selected = listGui.getSelectedOrNull() != null;
		editButton.active = selected;
		removeButton.active = selected;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredTextWithShadow(client.textRenderer,
			bookOffers.getName() + " (" + bookOffers.getOffers().size() + ")",
			width / 2, 12, Colors.WHITE);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
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
		extends AlwaysSelectedEntryListWidget.Entry<EditBookOffersScreen.Entry>
	{
		private final BookOffer bookOffer;
		
		public Entry(BookOffer bookOffer)
		{
			this.bookOffer = Objects.requireNonNull(bookOffer);
		}
		
		@Override
		public Text getNarration()
		{
			return Text.translatable("narrator.select",
				"Book offer " + bookOffer.getEnchantmentNameWithLevel()
					+ ", ID " + bookOffer.id() + ", " + getPriceText());
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
			String name = bookOffer.getEnchantmentNameWithLevel();
			
			RegistryEntry<Enchantment> enchantment =
				bookOffer.getEnchantmentEntry().get();
			int nameColor = enchantment.isIn(EnchantmentTags.CURSE)
				? WurstColors.LIGHT_RED : WurstColors.VERY_LIGHT_GRAY;
			context.drawText(tr, name, x + 28, y, nameColor, false);
			
			context.drawText(tr, bookOffer.id(), x + 28, y + 9,
				Colors.LIGHT_GRAY, false);
			
			String price = getPriceText();
			context.drawText(tr, price, x + 28, y + 18, Colors.LIGHT_GRAY,
				false);
			
			if(bookOffer.price() < 64)
				RenderUtils.drawItem(context, new ItemStack(Items.EMERALD),
					x + 28 + tr.getWidth(price), y + 16, false);
		}
		
		private String getPriceText()
		{
			if(bookOffer.price() >= 64)
				return "any price";
			
			return "max " + bookOffer.price();
		}
	}
	
	private final class ListGui
		extends AlwaysSelectedEntryListWidget<EditBookOffersScreen.Entry>
	{
		public ListGui(MinecraftClient minecraft, EditBookOffersScreen screen,
			List<BookOffer> list)
		{
			super(minecraft, screen.width, screen.height - 108, 36, 30);
			
			list.stream().map(EditBookOffersScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public BookOffer getSelectedOffer()
		{
			EditBookOffersScreen.Entry entry = getSelectedOrNull();
			return entry != null ? entry.bookOffer : null;
		}
	}
}
