package net.wurstclient.altmanager;

public final class XBoxLiveToken
{
	private final String token;
	private final String uhs;
	
	public XBoxLiveToken(String token, String uhs)
	{
		this.token = token;
		this.uhs = uhs;
	}
	
	public String getToken()
	{
		return token;
	}
	
	public String getUHS()
	{
		return uhs;
	}
}
