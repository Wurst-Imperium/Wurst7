package net.wurstclient.hacks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import net.minecraft.text.Text;
import net.wurstclient.Category;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.hack.Hack;

public class BetterChatHack extends Hack implements ChatInputListener {

	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
	
	public BetterChatHack() {
		super("Better Chat", "Improves the formating of chat.");
		setCategory(Category.CHAT);
	}
	
	@Override
	protected void onEnable() {
		EVENTS.add(ChatInputListener.class, this);
	}

	@Override
	public void onReceivedMessage(ChatInputEvent event) {		
		String message = event.getComponent().getString();
		String timeStamp = "";
		
		LocalDateTime now = LocalDateTime.now();  
		
		timeStamp = "[" + dtf.format(now) + "] ";
		
		message = timeStamp + event.getComponent().getString();
		
		event.setComponent(Text.of(message));
	}
	
	@Override
	protected void onDisable() {
		EVENTS.remove(ChatInputListener.class, this);
	}
}
