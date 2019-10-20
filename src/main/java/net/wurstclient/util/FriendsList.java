package net.wurstclient.util;

import java.nio.file.Path;
import java.util.ArrayList;
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
	 * Adds a friend, and saves them to a list.
	 * @param playerName The name of the player to add as friend.
	 */
	public void addFriend(String playerName)
	{
		friends.add(playerName);
		save();
	}

	/**
	 * Save all friends in the list to the file at friendsFile.
	 */
	private void save()
	{
		System.out.println("TEST: Saved!");
	}

	/**
	 * Loads all friends from file, and puts them in the list. If the file does not exist, a blank one will be created.
	 */
	private void load()
	{

	}
}
