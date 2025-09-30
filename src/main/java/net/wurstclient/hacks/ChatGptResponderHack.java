/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.text.Text;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.Hack;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.events.ChatInputListener.ChatInputEvent;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.TextFieldSetting;
import net.wurstclient.ai.OpenAIClient;

@SearchTags({"auto reply", "chatgpt", "gpt", "responder"})
public final class ChatGptResponderHack extends Hack
	implements ChatInputListener
{
	
	// =========================
	// Settings (existing)
	// =========================
	private final TextFieldSetting apiKey =
		new TextFieldSetting("OpenAI API Key", "");
	
	// Editable prompt template
	// Placeholders: {message}, {sender}, {me}, {is_dm}
	private final TextFieldSetting promptTemplate = new TextFieldSetting(
		"Prompt Template",
		"Reply concisely (max 2 short sentences) to this Minecraft chat from {sender}: {message}");
	
	private final CheckboxSetting onlyWhenMentioned = new CheckboxSetting(
		"Only reply when mentioned",
		"Reply only if your name is in the incoming chat line (public chat). DMs are always allowed when 'Respond to DMs' is ON.",
		true);
	
	private final CheckboxSetting respondToDMs = new CheckboxSetting(
		"Respond to DMs/whispers",
		"Reply even if your name isn't mentioned when it's a private message (DM).",
		true);
	
	private final CheckboxSetting ignoreOwnMessages = new CheckboxSetting(
		"Ignore my own messages",
		"Prevents loops if the server echoes your chat. (DMs with pattern like [Sender -> me] will NOT be ignored.)",
		true);
	
	private final CheckboxSetting testModeEcho =
		new CheckboxSetting("Test mode (echo instead of API)",
			"Send a simple echo reply to verify event & sending path.", false);
	
	private final CheckboxSetting debugLog = new CheckboxSetting(
		"Debug log to console", "Log decisions and outcomes.", false);
	
	// Rate control
	private final SliderSetting cooldownMs = new SliderSetting("Cooldown (ms)",
		"Minimum time between replies to the same sender.", 3000, 0, 60000, 250,
		SliderSetting.ValueDisplay.DECIMAL);
	
	private final SliderSetting sendDelayMs =
		new SliderSetting("Send delay (ms)",
			"Delay before sending a reply (adds human-like latency).", 750, 0,
			4000, 50, SliderSetting.ValueDisplay.DECIMAL);
	
	// =========================
	// NEW: Mod disabling settings
	// =========================
	private final CheckboxSetting disableMods = new CheckboxSetting(
		"Disable mods when messaging",
		"Temporarily disable selected hacks while composing/sending a reply.",
		true);
	
	private final CheckboxSetting disableSneak = new CheckboxSetting(
		"  – Sneak", "Disable Sneak while messaging.", true);
	
	private final CheckboxSetting disableTriggerBot = new CheckboxSetting(
		"  – TriggerBot", "Disable TriggerBot while messaging.", true);
	
	private final CheckboxSetting disableKillauraLegit = new CheckboxSetting(
		"  – Killaura Legit", "Disable KillauraLegit while messaging.", true);
	
	private final SliderSetting reenableDelayMs =
		new SliderSetting("Re-enable delay (ms)",
			"How long after sending to re-enable disabled hacks.", 500, 0, 5000,
			50, SliderSetting.ValueDisplay.DECIMAL);
	
	// last reply timestamps per sender (lowercased)
	private final Map<String, Long> lastReplyAt = new HashMap<>();
	
	// NEW: a ref-counted disabler so overlaps are safe
	private final ModDisabler modDisabler = new ModDisabler();
	
	public ChatGptResponderHack()
	{
		super("ChatGptResponder");
		setCategory(Category.CHAT);
		addSetting(apiKey);
		addSetting(promptTemplate);
		addSetting(onlyWhenMentioned);
		addSetting(respondToDMs);
		addSetting(ignoreOwnMessages);
		addSetting(testModeEcho);
		addSetting(debugLog);
		addSetting(cooldownMs);
		addSetting(sendDelayMs);
		
		// New section in GUI
		addSetting(disableMods);
		addSetting(disableSneak);
		addSetting(disableTriggerBot);
		addSetting(disableKillauraLegit);
		addSetting(reenableDelayMs);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(ChatInputListener.class, this);
		if(debugLog.isChecked())
			System.out.println("[ChatGptResponder] enabled");
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(ChatInputListener.class, this);
		if(debugLog.isChecked())
			System.out.println("[ChatGptResponder] disabled");
	}
	
	// Parsed result
	private static class Parsed
	{
		final String sender; // null if unknown
		final String msg; // the actual message content (no names)
		final boolean isDm; // true if a DM/whisper
		
		Parsed(String s, String m, boolean d)
		{
			sender = s;
			msg = m;
			isDm = d;
		}
	}
	
	// Try to detect DMs & extract (sender, message).
	private Parsed parse(String raw, String myName)
	{
		// [Sender -> me] message
		int b1 = raw.indexOf('[');
		int b2 = raw.indexOf(']');
		if(b1 >= 0 && b2 > b1)
		{
			String bracket = raw.substring(b1 + 1, b2).trim();
			String lower = bracket.toLowerCase();
			if(lower.contains("->"))
			{
				String[] parts = bracket.split("->", 2);
				if(parts.length == 2)
				{
					String left = parts[0].trim();
					String right = parts[1].trim();
					String remainder = raw.substring(b2 + 1).trim();
					if(right.equalsIgnoreCase("me")
						|| right.equalsIgnoreCase(myName))
					{
						String sender =
							left.replace("<", "").replace(">", "").trim();
						return new Parsed(sender, remainder, true);
					}
					if(left.equalsIgnoreCase("me")
						|| left.equalsIgnoreCase(myName))
					{
						return null; // our own outgoing DM echo
					}
				}
			}
		}
		// From <Name>: message
		if(raw.startsWith("From ") && raw.contains(":"))
		{
			int open = raw.indexOf('<');
			int close = raw.indexOf('>');
			if(open >= 0 && close > open)
			{
				String sender = raw.substring(open + 1, close);
				String msg =
					raw.substring(close + 1).replaceFirst("^:\\s*", "").trim();
				return new Parsed(sender, msg, true);
			}
			// From Name: message
			String rest = raw.substring("From ".length());
			int colon = rest.indexOf(':');
			if(colon > 0)
			{
				String sender = rest.substring(0, colon).trim();
				String msg = rest.substring(colon + 1).trim();
				return new Parsed(sender, msg, true);
			}
		}
		// <Name> message
		if(raw.startsWith("<") && raw.indexOf('>') > 1)
		{
			int end = raw.indexOf('>');
			String sender = raw.substring(1, end);
			String msg = raw.substring(end + 1).trim();
			return new Parsed(sender, msg, false);
		}
		// fallback: public/unknown
		return new Parsed(null, raw, false);
	}
	
	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		final Text comp = event.getComponent();
		final String raw = comp.getString();
		final String me = MC.getSession().getUsername();
		
		if(debugLog.isChecked())
			System.out.println("[ChatGptResponder] Saw: " + raw);
		
		Parsed parsed = parse(raw, me);
		if(parsed == null)
		{
			if(debugLog.isChecked())
				System.out.println(
					"[ChatGptResponder] Ignored (own/outgoing DM echo or unknown).");
			return;
		}
		
		if(ignoreOwnMessages.isChecked() && !parsed.isDm)
		{
			if(raw.contains("<" + me + ">") || raw.startsWith(me + ":"))
				return;
		}
		
		boolean mentioned = raw.toLowerCase().contains(me.toLowerCase());
		if(!parsed.isDm && onlyWhenMentioned.isChecked() && !mentioned)
			return;
		
		if(parsed.isDm && !respondToDMs.isChecked())
			return;
		
		// per-sender cooldown
		String keyForCooldown =
			(parsed.sender == null ? "unknown" : parsed.sender.toLowerCase());
		long now = System.currentTimeMillis();
		long last = lastReplyAt.getOrDefault(keyForCooldown, 0L);
		if(now - last < (long)cooldownMs.getValue())
			return;
		lastReplyAt.put(keyForCooldown, now);
		
		final boolean replyPrivately = parsed.isDm && parsed.sender != null;
		final String targetName = parsed.sender;
		final String textForModel = parsed.msg;
		final String publicPrefix =
			(parsed.sender != null ? "@" + parsed.sender + " " : "");
		final long delay = (long)sendDelayMs.getValue();
		
		// ---- TEST MODE ----
		if(testModeEcho.isChecked())
		{
			String reply = sanitizeForChat(textForModel);
			if(reply.isEmpty())
				return;
			
			Thread.ofVirtual().start(() -> {
				try
				{
					if(disableMods.isChecked())
						modDisabler.acquireAndApply();
					if(delay > 0)
						Thread.sleep(delay + jitter());
					if(MC.getNetworkHandler() != null)
					{
						if(replyPrivately)
						{
							MC.getNetworkHandler().sendChatCommand(
								"msg " + targetName + " " + reply);
							if(debugLog.isChecked())
								System.out.println(
									"[ChatGptResponder] Echo via /msg to "
										+ targetName);
						}else
						{
							MC.getNetworkHandler()
								.sendChatMessage(publicPrefix + reply);
							if(debugLog.isChecked())
								System.out.println(
									"[ChatGptResponder] Echo (public).");
						}
					}
				}catch(InterruptedException ignored)
				{}finally
				{
					if(disableMods.isChecked())
						modDisabler
							.releaseLater((long)reenableDelayMs.getValue());
				}
			});
			return;
		}
		
		// ---- REAL MODE (OpenAI) ----
		final String key =
			apiKey.getValue() == null ? "" : apiKey.getValue().trim();
		if(key.isEmpty())
		{
			if(debugLog.isChecked())
				System.out.println("[ChatGptResponder] No API key set.");
			return;
		}
		
		final String tpl = (promptTemplate.getValue() == null
			|| promptTemplate.getValue().isBlank())
				? "Reply concisely (max 2 short sentences) to this Minecraft chat from {sender}: {message}"
				: promptTemplate.getValue();
		
		final String prompt = tpl.replace("{message}", textForModel)
			.replace("{sender}", targetName == null ? "Unknown" : targetName)
			.replace("{me}", me)
			.replace("{is_dm}", Boolean.toString(parsed.isDm));
		
		Thread.ofVirtual().name("ChatGptResponder")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace())
			.start(() -> {
				try
				{
					if(disableMods.isChecked())
						modDisabler.acquireAndApply();
					
					String reply = OpenAIClient.completeChat(key, prompt);
					reply = sanitizeForChat(reply);
					if(reply == null || reply.isBlank())
					{
						if(debugLog.isChecked())
							System.out.println(
								"[ChatGptResponder] API returned empty/illegal.");
						return;
					}
					
					try
					{
						if(delay > 0)
							Thread.sleep(delay + jitter());
					}catch(InterruptedException ignored)
					{}
					
					if(MC.getNetworkHandler() != null)
					{
						if(replyPrivately)
						{
							MC.getNetworkHandler().sendChatCommand(
								"msg " + targetName + " " + reply);
							if(debugLog.isChecked())
								System.out.println(
									"[ChatGptResponder] Sent via /msg to "
										+ targetName);
						}else
						{
							MC.getNetworkHandler()
								.sendChatMessage(publicPrefix + reply);
							if(debugLog.isChecked())
								System.out.println(
									"[ChatGptResponder] Sent (public).");
						}
					}
				}catch(Exception e)
				{
					e.printStackTrace();
				}finally
				{
					if(disableMods.isChecked())
						modDisabler
							.releaseLater((long)reenableDelayMs.getValue());
				}
			});
	}
	
	// ============ Helpers ============
	
	private static long jitter()
	{
		return (long)((Math.random() - 0.5) * 300.0); // ±150ms
	}
	
	private static String sanitizeForChat(String s)
	{
		if(s == null)
			return "";
		s = s.replaceAll("§.", ""); // strip color codes
		s = s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
		s = s.replaceAll("\\p{C}", ""); // control chars
		s = s.trim();
		if(s.length() > 256)
			s = s.substring(0, 256);
		return s;
	}
	
	/**
	 * Temporarily disables selected hacks while messaging, with reference
	 * counting
	 * so overlapping replies don't re-enable too early.
	 */
	private final class ModDisabler
	{
		private final AtomicInteger holds = new AtomicInteger(0);
		private Boolean priorSneak, priorTriggerBot, priorKillauraLegit;
		
		void acquireAndApply()
		{
			if(holds.getAndIncrement() == 0)
			{
				apply();
			}
		}
		
		void releaseLater(long ms)
		{
			Thread.ofVirtual().start(() -> {
				try
				{
					if(ms > 0)
						Thread.sleep(ms);
				}catch(InterruptedException ignored)
				{}
				release();
			});
		}
		
		private synchronized void apply()
		{
			// snapshot & disable only once (first acquire)
			var hax = WurstClient.INSTANCE.getHax();
			
			if(disableSneak.isChecked() && priorSneak == null)
			{
				priorSneak = hax.sneakHack.isEnabled();
				if(priorSneak)
					hax.sneakHack.setEnabled(false);
			}
			if(disableTriggerBot.isChecked() && priorTriggerBot == null)
			{
				priorTriggerBot = hax.triggerBotHack.isEnabled();
				if(priorTriggerBot)
					hax.triggerBotHack.setEnabled(false);
			}
			if(disableKillauraLegit.isChecked() && priorKillauraLegit == null)
			{
				priorKillauraLegit = hax.killauraLegitHack.isEnabled();
				if(priorKillauraLegit)
					hax.killauraLegitHack.setEnabled(false);
			}
		}
		
		private synchronized void release()
		{
			if(holds.decrementAndGet() > 0)
				return; // still in use by another reply
				
			// restore exactly to previous states
			var hax = WurstClient.INSTANCE.getHax();
			
			if(priorSneak != null)
			{
				if(priorSneak)
					hax.sneakHack.setEnabled(true);
				priorSneak = null;
			}
			if(priorTriggerBot != null)
			{
				if(priorTriggerBot)
					hax.triggerBotHack.setEnabled(true);
				priorTriggerBot = null;
			}
			if(priorKillauraLegit != null)
			{
				if(priorKillauraLegit)
					hax.killauraLegitHack.setEnabled(true);
				priorKillauraLegit = null;
			}
		}
	}
}
