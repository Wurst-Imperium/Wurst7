/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.wurstclient.hacks.AutoCraftHack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoStealHack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(HandledScreen.class)
public abstract class AutoCraftHandledScreenMixin<T extends ScreenHandler>
        extends Screen
        implements ScreenHandlerProvider<T>
{
    private final AutoCraftHack autoCraft = WurstClient.INSTANCE.getHax().autoCraftHack;

    public AutoCraftHandledScreenMixin(Text name)
    {
        super(name);
    }

    private boolean isCraftingTable() {
        Screen screenObj = (Screen)this;
        return screenObj instanceof CraftingScreen;
    }

    @Inject(at = @At("TAIL"), method = "init")
    protected void init(CallbackInfo info)
    {
        if(!WurstClient.INSTANCE.isEnabled() || !isCraftingTable())
            return;

        /*if(autoCraft.isEnabled()) {
            autoCraft.queueCraft(new Identifier("minecraft:wooden_pickaxe"));
            autoCraft.queueCraft(new Identifier("minecraft:wooden_sword"));
            autoCraft.queueCraft(new Identifier("minecraft:wooden_axe"));
            autoCraft.queueCraft(new Identifier("minecraft:wooden_hoe"));
            autoCraft.queueCraft(new Identifier("minecraft:wooden_shovel"));
        }*/
    }
}
