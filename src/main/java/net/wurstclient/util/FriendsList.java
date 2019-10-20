package net.wurstclient.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a list of friends, along with methods to save or load the list from disk.
 */
public class FriendsList {
	private final List<String> friends = new ArrayList<>();
	private Path friendsFile;

	public FriendsList(Path friendsFile)
	{
		this.friendsFile = friendsFile;
		load();
	}

	/**
	 * Adds a friend, and saves them to a list. If the player already exists, they will not be re-added, and a message will be printed to chat.
	 * @param playerName The name of the player to add as friend.
	 */
	public void addFriend(String playerName)
	{
		if (friends.contains(playerName))
		{
			ChatUtils.message(playerName + " is already a friend!");
		}
		else
		{
			friends.add(playerName);
			save();
		}
	}

	/**
	 * Removes a friend and then save the list. If the player does not exist, nothing will happen and a message will be printed.
	 * @param playerName The name of the player to remove from the friends list.
	 */
	public void removeFriend(String playerName) {
		if (friends.contains(playerName))
		{
			friends.remove(playerName);
		}
		else
		{
			ChatUtils.message(playerName + " is not a friend.");
		}
	}

	/**
	 * Gets a list of all friends. Attempts to add or remove from this will cause a crash.
	 * @return A unmodifiable list of friends.
	 */
	public List<String> getAllFriends()
	{
		return Collections.unmodifiableList(friends);
	}

	/**
	 * Save all friends in the list to the file at friendsFile.
	 */
	private void save()
	{

	}

	/**
	 * Loads all friends from file, and puts them in the list. If the file does not exist, a blank one will be created.
	 */
	private void load()
	{

	}
}
