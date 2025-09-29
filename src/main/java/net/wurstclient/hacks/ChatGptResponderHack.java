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

import net.minecraft.text.Text;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
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
	
	// New: rate control
	private final SliderSetting cooldownMs = new SliderSetting("Cooldown (ms)",
		"Minimum time between replies to the same sender.", 3000, 0, 60000, 250,
		SliderSetting.ValueDisplay.DECIMAL);
	
	private final SliderSetting sendDelayMs =
		new SliderSetting("Send delay (ms)",
			"Delay before sending a reply (adds human-like latency).", 750, 0,
			4000, 50, SliderSetting.ValueDisplay.DECIMAL);
	
	// last reply timestamps per sender (lowercased)
	private final Map<String, Long> lastReplyAt = new HashMap<>();
	
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
	// Handles:
	// 1) "<Name> message" -> public
	// 2) "From <Name>: message" -> DM
	// 3) "From Name: message" -> DM
	// 4) "[Sender -> me] message" -> DM (your server format)
	// 5) "[me -> Target] message" -> outgoing DM (we ignore)
	private Parsed parse(String raw, String myName)
	{
		// (4) Arrow pattern inside brackets
		int b1 = raw.indexOf('[');
		int b2 = raw.indexOf(']');
		if(b1 >= 0 && b2 > b1)
		{
			String bracket = raw.substring(b1 + 1, b2).trim(); // e.g.
																// "Vijay765846
																// -> me" or "me
																// -> Vijay"
			String lower = bracket.toLowerCase();
			if(lower.contains("->"))
			{
				String[] parts = bracket.split("->", 2);
				if(parts.length == 2)
				{
					String left = parts[0].trim();
					String right = parts[1].trim();
					String remainder = raw.substring(b2 + 1).trim();
					
					// inbound DM: [Sender -> me]
					if(right.equalsIgnoreCase("me")
						|| right.equalsIgnoreCase(myName))
					{
						String sender =
							left.replace("<", "").replace(">", "").trim();
						return new Parsed(sender, remainder, true);
					}
					// outgoing DM: [me -> Target] ; ignore as "own message"
					// even if server wraps it oddly
					if(left.equalsIgnoreCase("me")
						|| left.equalsIgnoreCase(myName))
					{
						return null; // treat as our own outgoing DM echo -> do
										// nothing
					}
				}
			}
		}
		
		// (2) From <Name>: message
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
			// (3) From Name: message
			String rest = raw.substring("From ".length());
			int colon = rest.indexOf(':');
			if(colon > 0)
			{
				String sender = rest.substring(0, colon).trim();
				String msg = rest.substring(colon + 1).trim();
				return new Parsed(sender, msg, true);
			}
		}
		
		// (1) Vanilla public: "<Name> message"
		if(raw.startsWith("<") && raw.indexOf('>') > 1)
		{
			int end = raw.indexOf('>');
			String sender = raw.substring(1, end);
			String msg = raw.substring(end + 1).trim();
			return new Parsed(sender, msg, false);
		}
		
		// Unknown format; treat entire line as the message (public), with
		// unknown sender
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
			
		// Parse first (important: some servers echo DMs as "<me> [Sender -> me]
		// msg")
		Parsed parsed = parse(raw, me);
		if(parsed == null)
		{
			// our own outgoing DM echo or unhandled we choose to ignore
			if(debugLog.isChecked())
				System.out.println(
					"[ChatGptResponder] Ignored (own/outgoing DM echo or unknown).");
			return;
		}
		
		// Ignore own public lines (unless it's an inbound DM disguised in our
		// line)
		if(ignoreOwnMessages.isChecked() && !parsed.isDm)
		{
			if(raw.contains("<" + me + ">") || raw.startsWith(me + ":"))
				return;
		}
		
		// mention gating for PUBLIC chat only
		boolean mentioned = raw.toLowerCase().contains(me.toLowerCase());
		if(!parsed.isDm && onlyWhenMentioned.isChecked() && !mentioned)
			return;
		
		// DMs allowed?
		if(parsed.isDm && !respondToDMs.isChecked())
			return;
		
		// Rate-limit per sender (unknown sender uses key "unknown")
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
		
		// ---- TEST MODE ----
		if(testModeEcho.isChecked())
		{
			String reply = sanitizeForChat(textForModel);
			if(reply.isEmpty())
				return;
			
			long delay = (long)sendDelayMs.getValue();
			Thread.ofVirtual().start(() -> {
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
						MC.getNetworkHandler()
							.sendChatCommand("msg " + targetName + " " + reply);
						if(debugLog.isChecked())
							System.out
								.println("[ChatGptResponder] Echo via /msg to "
									+ targetName);
					}else
					{
						MC.getNetworkHandler()
							.sendChatMessage(publicPrefix + reply);
						if(debugLog.isChecked())
							System.out
								.println("[ChatGptResponder] Echo (public).");
					}
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
		
		long delay = (long)sendDelayMs.getValue();
		
		Thread.ofVirtual().name("ChatGptResponder")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace())
			.start(() -> {
				try
				{
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
				}
			});
	}
	
	private static long jitter()
	{
		// ±150ms jitter
		return (long)((Math.random() - 0.5) * 300.0);
	}
	
	private static String sanitizeForChat(String s)
	{
		if(s == null)
			return "";
		// strip color codes
		s = s.replaceAll("§.", "");
		// normalize whitespace
		s = s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
		// strip other control chars
		s = s.replaceAll("\\p{C}", "");
		s = s.trim();
		if(s.length() > 256)
			s = s.substring(0, 256);
		return s;
	}
}
