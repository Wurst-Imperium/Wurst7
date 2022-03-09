package net.wurstclient.altmanager;

import java.util.UUID;

public class MinecraftProfile
{
	public UUID uuid;
	public String name;
	public String jwt;
	
	public MinecraftProfile(UUID uuid, String name, String jwt)
	{
		this.uuid = uuid;
		this.name = name;
		this.jwt = jwt;
	}
}
