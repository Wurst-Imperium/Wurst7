package net.wurstclient.hacks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import net.minecraft.text.Text;
import net.wurstclient.Category;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

public class BetterChatHack extends Hack implements ChatInputListener {

	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
	
	private CheckboxSetting timestampsSetting = new CheckboxSetting("Timestamps", true);
	private CheckboxSetting militaryTimeSetting = new CheckboxSetting("24-hour clock", false);
	
	public BetterChatHack() {
		super("Better Chat", "Improves the formating of chat.");
		setCategory(Category.CHAT);
		addSetting(timestampsSetting);
		addSetting(militaryTimeSetting);
	}
	
	@Override
	protected void onEnable() {
		EVENTS.add(ChatInputListener.class, this);
	}

	@Override
	public void onReceivedMessage(ChatInputEvent event) {
		AddTimestamp(event.getComponent().getString(), event);
	}
	
	public void AddTimestamp(String message, ChatInputEvent event) {
		if(!timestampsSetting.isChecked())
			return;
		
		String timeStamp = "";
		LocalDateTime now = LocalDateTime.now();
		
		if(!militaryTimeSetting.isChecked()) {
			if(now.getHour() > 12) {
				timeStamp = "[" + dtf.format(now.minusHours(12)) + "] ";
				
				message = timeStamp + event.getComponent().getString();
				
				event.setComponent(Text.of(message));
			}
			else {
				Add24HourTimestamp(now, timeStamp, message, event);
			}
		}
		else {
			Add24HourTimestamp(now, timeStamp, message, event);
		}	
	}
	
	public void Add24HourTimestamp(LocalDateTime now, String timeStamp, String message, ChatInputEvent event) {
		timeStamp = "[" + dtf.format(now) + "] ";
		
		message = timeStamp + event.getComponent().getString();
		
		event.setComponent(Text.of(message));
	}
	
	@Override
	protected void onDisable() {
		EVENTS.remove(ChatInputListener.class, this);
	}
}
