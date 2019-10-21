package net.wurstclient.util;

import com.google.gson.JsonArray;
import net.wurstclient.hack.Hack;
import net.wurstclient.hack.HackList;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents a list of friends, along with methods to save or load the list from disk.
 */
public class FriendsList {
	private List<String> friends = new ArrayList<>();
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
			save();
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
	 * Save all friends in the list to the file at friendsFile. This file will be created if necessary.
	 */
	private void save()
	{
		try
		{
			JsonUtils.toJson(createJson(), friendsFile);
		}
		catch (JsonException | IOException e)
		{
			System.out.println("Error while trying to save friends list.");
			e.printStackTrace();
		}
	}

	/**
	 * Loads all friends from file, and puts them in the list. If the file does not exist, a blank one will be created.
	 */
	private void load()
	{
		try
		{
			friends = JsonUtils.parseFileToArray(friendsFile).getAllStrings();
		} catch (IOException | JsonException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Creates a JsonArray from the friends list.
	 * @return A JsonArray
	 */
	private JsonArray createJson()
	{
		JsonArray json = new JsonArray();
		friends.forEach(json::add);

		return json;
	}
}
