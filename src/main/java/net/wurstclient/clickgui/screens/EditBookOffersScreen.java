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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.wurstclient.hacks.autolibrarian.BookOffer;
import net.wurstclient.settings.BookOffersSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.WurstColors;

public final class EditBookOffersScreen extends Screen
{
	private final Screen prevScreen;
	private final BookOffersSetting bookOffers;
	
	private ListGui listGui;
	private Button editButton;
	private Button removeButton;
	private Button doneButton;
	
	public EditBookOffersScreen(Screen prevScreen, BookOffersSetting bookOffers)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
		this.bookOffers = bookOffers;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(minecraft, this, bookOffers.getOffers());
		addWidget(listGui);
		
		addRenderableWidget(Button
			.builder(Component.literal("Add"),
				b -> minecraft
					.setScreen(new AddBookOfferScreen(this, bookOffers)))
			.bounds(width / 2 - 154, height - 56, 100, 20).build());
		
		addRenderableWidget(
			editButton = Button.builder(Component.literal("Edit"), b -> {
				BookOffer selected = listGui.getSelectedOffer();
				if(selected == null)
					return;
				
				minecraft.setScreen(new EditBookOfferScreen(this, bookOffers,
					bookOffers.indexOf(selected)));
			}).bounds(width / 2 - 50, height - 56, 100, 20).build());
		editButton.active = false;
		
		addRenderableWidget(
			removeButton = Button.builder(Component.literal("Remove"), b -> {
				bookOffers
					.remove(bookOffers.indexOf(listGui.getSelectedOffer()));
				minecraft.setScreen(EditBookOffersScreen.this);
			}).bounds(width / 2 + 54, height - 56, 100, 20).build());
		removeButton.active = false;
		
		addRenderableWidget(
			Button.builder(Component.literal("Reset to Defaults"),
				b -> minecraft.setScreen(new ConfirmScreen(b2 -> {
					if(b2)
						bookOffers.resetToDefaults();
					minecraft.setScreen(EditBookOffersScreen.this);
				}, Component.literal("Reset to Defaults"),
					Component.literal("Are you sure?"))))
				.bounds(width - 106, 6, 100, 20).build());
		
		addRenderableWidget(doneButton = Button
			.builder(Component.literal("Done"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 - 100, height - 32, 200, 20).build());
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent context, boolean doubleClick)
	{
		boolean childClicked = super.mouseClicked(context, doubleClick);
		
		if(context.button() == GLFW.GLFW_MOUSE_BUTTON_4)
			doneButton.onPress(context);
		
		return childClicked;
	}
	
	@Override
	public boolean keyPressed(KeyEvent context)
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
		boolean selected = listGui.getSelected() != null;
		editButton.active = selected;
		removeButton.active = selected;
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredString(minecraft.font,
			bookOffers.getName() + " (" + bookOffers.getOffers().size() + ")",
			width / 2, 12, CommonColors.WHITE);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	private final class Entry
		extends ObjectSelectionList.Entry<EditBookOffersScreen.Entry>
	{
		private final BookOffer bookOffer;
		
		public Entry(BookOffer bookOffer)
		{
			this.bookOffer = Objects.requireNonNull(bookOffer);
		}
		
		@Override
		public Component getNarration()
		{
			return Component.translatable("narrator.select",
				"Book offer " + bookOffer.getEnchantmentNameWithLevel()
					+ ", ID " + bookOffer.id() + ", " + getPriceText());
		}
		
		@Override
		public void renderContent(GuiGraphics context, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			int x = getContentX();
			int y = getContentY();
			
			Item item = BuiltInRegistries.ITEM
				.getValue(Identifier.parse("enchanted_book"));
			ItemStack stack = new ItemStack(item);
			RenderUtils.drawItem(context, stack, x + 1, y + 1, true);
			
			Font tr = minecraft.font;
			String name = bookOffer.getEnchantmentNameWithLevel();
			
			Holder<Enchantment> enchantment =
				bookOffer.getEnchantmentEntry().get();
			int nameColor = enchantment.is(EnchantmentTags.CURSE)
				? WurstColors.LIGHT_RED : WurstColors.VERY_LIGHT_GRAY;
			context.drawString(tr, name, x + 28, y, nameColor, false);
			
			context.drawString(tr, bookOffer.id(), x + 28, y + 9,
				CommonColors.LIGHT_GRAY, false);
			
			String price = getPriceText();
			context.drawString(tr, price, x + 28, y + 18,
				CommonColors.LIGHT_GRAY, false);
			
			if(bookOffer.price() < 64)
				RenderUtils.drawItem(context, new ItemStack(Items.EMERALD),
					x + 28 + tr.width(price), y + 16, false);
		}
		
		private String getPriceText()
		{
			if(bookOffer.price() >= 64)
				return "any price";
			
			return "max " + bookOffer.price();
		}
	}
	
	private final class ListGui
		extends ObjectSelectionList<EditBookOffersScreen.Entry>
	{
		public ListGui(Minecraft minecraft, EditBookOffersScreen screen,
			List<BookOffer> list)
		{
			super(minecraft, screen.width, screen.height - 108, 36, 30);
			
			list.stream().map(EditBookOffersScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public BookOffer getSelectedOffer()
		{
			EditBookOffersScreen.Entry entry = getSelected();
			return entry != null ? entry.bookOffer : null;
		}
	}
}
