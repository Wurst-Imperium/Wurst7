/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import org.apache.commons.lang3.StringEscapeUtils;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.LiteralText;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.events.ChatOutputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.services.GoogleService;
import net.wurstclient.services.GoogleService.Language;
import net.wurstclient.util.ChatUtils;

@SearchTags({ "AutoTranslate", "Translate" })
public final class AutoTranslateHack extends Hack implements ChatInputListener, ChatOutputListener {

    public static GoogleService google = new GoogleService();;

    private final CheckboxSetting mychat = new CheckboxSetting("Translate your own chat.", false);

    private final CheckboxSetting otherchat = new CheckboxSetting("Translate other chat.", true);

    private final EnumSetting<Language> translate_from = new EnumSetting<>("Translate other chat from?",
            Language.values(), Language.AUTO_DETECT);

    private final EnumSetting<Language> translate_to = new EnumSetting<>("Translate other chat to?", Language.values(),
            Language.ENGLISH);

    private final EnumSetting<Language> translate_my = new EnumSetting<>("Translate my own chat to?", Language.values(),
            Language.JAPANESE);

    public AutoTranslateHack() {
        super("AutoTranslate", "Translate every chat into other language.\n" 
        + "Translate your message into other language.\n"
        +"\n"
        +"Created by Dj-jom2x");
        setCategory(Category.CHAT);
        addSetting(mychat);
        addSetting(otherchat);
        addSetting(translate_my);
        addSetting(translate_from);
        addSetting(translate_to);
    }

    @Override
    public void onEnable() {
        EVENTS.add(ChatInputListener.class, this);
        EVENTS.add(ChatOutputListener.class, this);
    }

    @Override
    public void onDisable() {
        EVENTS.remove(ChatInputListener.class, this);
        EVENTS.remove(ChatOutputListener.class, this);
    }

    public static void translate(String message, String lang) {
        ChatHud chatHud = MC.inGameHud.getChatHud();
        LiteralText prefix = new LiteralText(String.format("\u00A7a[\u00A7b%s\u00A7a]:\u00a7r ", lang));
        chatHud.addMessage(prefix.append(new LiteralText(message)));
    }

    @Override
    public void onReceivedMessage(ChatInputEvent event) {
        if (otherchat.isChecked()) {
            new Thread(() -> {
                try {
                    String message = event.getComponent().getString();
                    if (!message.startsWith(ChatUtils.WURST_PREFIX) && !message.startsWith(
                            String.format("\u00A7a[\u00A7b%s\u00A7a]:\u00a7r ", translate_to.getSelected().name))) {
                        String text = StringEscapeUtils.unescapeHtml4(google.translate(message,
                                translate_from.getSelected().value, translate_to.getSelected().value));
                        if (text != null) {
                            translate(text, translate_to.getSelected().name);
                        }
                    }
                } catch (Exception e) {
                }
            }).start();
        }
    }

    @Override
    public void onSentMessage(ChatOutputEvent event) {
        String message = event.getOriginalMessage();
        if (mychat.isChecked()) {
            if (message.startsWith("/") || message.startsWith(".") || message.startsWith("@"))
                return;
            try {
                String text = StringEscapeUtils.unescapeHtml4(google.translate(message,
                        translate_from.getSelected().value, translate_my.getSelected().value));
                if (text != null) {
                    event.setMessage(text);
                }
            } catch (Exception e) {
                return;
            }
        }

    }
}
